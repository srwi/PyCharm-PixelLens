package com.github.srwi.pycharmpixelglance.dialogs

import com.github.srwi.pycharmpixelglance.actions.*
import com.github.srwi.pycharmpixelglance.data.Batch
import com.github.srwi.pycharmpixelglance.icons.ImageViewerIcons
import com.intellij.icons.AllIcons
import com.intellij.ide.ActivityTracker
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.PopupHandler
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.*
import com.intellij.util.ui.JBUI
import org.intellij.images.actions.ToggleTransparencyChessboardAction
import org.intellij.images.editor.ImageDocument
import org.intellij.images.editor.ImageZoomModel
import org.intellij.images.editor.actionSystem.ImageEditorActions
import org.intellij.images.editor.actions.ActualSizeAction
import org.intellij.images.editor.actions.ToggleGridAction
import org.intellij.images.editor.actions.ZoomInAction
import org.intellij.images.editor.actions.ZoomOutAction
import org.intellij.images.options.Options
import org.intellij.images.options.OptionsManager
import org.intellij.images.thumbnail.actionSystem.ThumbnailViewActions
import org.intellij.images.ui.ImageComponent
import org.intellij.images.ui.ImageComponentDecorator
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.Point
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseWheelEvent
import java.awt.event.MouseWheelListener
import java.awt.image.BufferedImage
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.*
import javax.swing.border.Border
import kotlin.math.max

class ImageViewer(project: Project, val data: Batch) : DialogWrapper(project), ImageComponentDecorator, DataProvider, Disposable {
    // TODO: read default from options
    var normalizeSelected: Boolean = data.normalized
        set(value) {
            if (field == value) return
            field = value
            data.normalized = value
            updateImage()
        }

    var transposeSelected: Boolean = data.channelsFirst
        set(value) {
            if (field == value) return
            field = value
            data.channelsFirst = value
            updateImage()
        }

    var reverseChannelsEnabled: Boolean = data.reversedChannels
        set(value) {
            if (field == value) return
            field = value
            data.reversedChannels = value
            updateImage()
        }

    var applyColormapEnabled: Boolean = data.grayscaleColormap
        set(value) {
            if (field == value) return
            field = value
            data.grayscaleColormap = value
            updateImage()
        }

    var selectedBatchIndex: Int = 0

    var selectedChannelIndex: Int? = null

    private val optionsChangeListener: PropertyChangeListener = OptionsChangeListener()
    private val imageComponent: ImageComponent = ImageComponent()
    private val internalZoomModel: ImageZoomModel = ImageZoomModelImpl(imageComponent)
    private val wheelAdapter = ImageWheelAdapter()
    private val resizeAdapter = ImageResizeAdapter()
    private var scrollPane: JScrollPane = JBScrollPane()
    private var currentSidebar: JComponent? = null
    private lateinit var batchSidebar: JComponent
    private lateinit var channelSidebar: JComponent
    private lateinit var sidebarPanel: JPanel

    private val toggleBatchSidebarAction = object : ToggleAction("Toggle Batch Sidebar", "", ImageViewerIcons.Layers) {
        override fun isSelected(e: AnActionEvent) = currentSidebar == batchSidebar
        override fun setSelected(e: AnActionEvent, state: Boolean) = toggleSidebar(state, batchSidebar)

        override fun getActionUpdateThread(): ActionUpdateThread {
            return ActionUpdateThread.EDT
        }
    }

    private val toggleChannelSidebarAction = object : ToggleAction("Toggle Channel Sidebar", "", ImageViewerIcons.Channels) {
        override fun isSelected(e: AnActionEvent) = currentSidebar == channelSidebar
        override fun setSelected(e: AnActionEvent, state: Boolean) = toggleSidebar(state, channelSidebar)

        override fun getActionUpdateThread(): ActionUpdateThread {
            return ActionUpdateThread.EDT
        }
    }

    init {
        title = "Image Viewer"  // TODO: replace with variable name, shape and dtype
        isModal = false

        val options = OptionsManager.getInstance().options
        options.addPropertyChangeListener(optionsChangeListener, this)

        val editorOptions = options.editorOptions
        val chessboardOptions = editorOptions.transparencyChessboardOptions
        val gridOptions = editorOptions.gridOptions
        imageComponent.transparencyChessboardCellSize = chessboardOptions.cellSize
        imageComponent.transparencyChessboardWhiteColor = chessboardOptions.whiteColor
        imageComponent.setTransparencyChessboardBlankColor(chessboardOptions.blackColor)
        imageComponent.gridLineZoomFactor = gridOptions.lineZoomFactor
        imageComponent.gridLineSpan = gridOptions.lineSpan
        imageComponent.gridLineColor = gridOptions.lineColor
        imageComponent.isBorderVisible = false
        imageComponent.addMouseListener(EditorMouseAdapter())

        init()

        updateImage(repaint = false)
        smartZoom()
    }

    private fun updateImage(repaint: Boolean = true) {
        val image = data.getImage(selectedBatchIndex, selectedChannelIndex)
        val document: ImageDocument = imageComponent.document
        document.value = image
        if (repaint) {
            repaintImage()
        }
        ActivityTracker.getInstance().inc()  // TODO: only update this toolbar
    }

    private fun smartZoom() {
        val zoomModel = internalZoomModel as ImageZoomModelImpl
        zoomModel.smartZoom(scrollPane.viewport.width, scrollPane.viewport.height)
        repaintImage()
    }

    private fun repaintImage() {
        contentPane.repaint()
    }

    override fun createCenterPanel(): JComponent {
        val contentPanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createEmptyBorder()
            add(createImagePanel(), BorderLayout.CENTER)
            add(createRightPanel(), BorderLayout.EAST)
        }
        batchSidebar = createBatchSidebar()
        channelSidebar = createChannelSidebar()
        return contentPanel
    }

    override fun createContentPaneBorder(): Border {
        return JBUI.Borders.empty(5)
    }

    private fun createRightPanel(): JComponent {
        val rightPanel = JPanel(BorderLayout())
        sidebarPanel = JPanel(BorderLayout()).apply {
            isVisible = false
            preferredSize = Dimension(200, 0)
        }
        rightPanel.add(sidebarPanel, BorderLayout.CENTER)
        return rightPanel
    }

    override fun createNorthPanel(): JComponent {
        val actionManager = ActionManager.getInstance()
        val actionGroup = createCustomActionGroup()
        val actionToolbar = actionManager.createActionToolbar(
            "MainToolbar", actionGroup, true
        ).apply {
            setReservePlaceAutoPopupIcon(false)
        }
        val toolbarPanel = actionToolbar.component.apply {
            border = BorderFactory.createEmptyBorder()
        }

        val sidebarToggleGroup = DefaultActionGroup().apply {
            add(toggleBatchSidebarAction)
            add(toggleChannelSidebarAction)
        }
        val sidebarToggleToolbar = actionManager.createActionToolbar(
            "SidebarToolbar", sidebarToggleGroup, true
        ).apply {
            setReservePlaceAutoPopupIcon(false)
        }.component.apply {
            border = BorderFactory.createEmptyBorder()
        }

        val twoSideComponent = TwoSideComponent(toolbarPanel, sidebarToggleToolbar)
        val topPanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createEmptyBorder()
            add(twoSideComponent, BorderLayout.CENTER)
        }

        return topPanel
    }

    override fun createSouthPanel(): JComponent? {
        // TODO: add status bar
        return null
    }

    private fun createImagePanel(): JComponent {
        val view = ImageContainerPane(imageComponent)
        scrollPane = ScrollPaneFactory.createScrollPane(view)
        scrollPane.border = null
        scrollPane.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
        scrollPane.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        scrollPane.addMouseWheelListener(wheelAdapter)
        scrollPane.addComponentListener(resizeAdapter)
        return scrollPane
    }

    private fun createBatchSidebar(): JComponent {
        val batchListModel = DefaultListModel<Int>().apply {
            for (i in 0 until data.batchSize) addElement(i)
        }
        val batchList = JBList(batchListModel).apply {
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            selectedIndex = selectedBatchIndex
            addListSelectionListener {
                selectedBatchIndex = selectedValue
                updateImage()
            }
        }
        val batchSidebarPanel = JPanel(BorderLayout()).apply {
            add(JLabel("Batch Index"), BorderLayout.NORTH)
            add(JBScrollPane(batchList), BorderLayout.CENTER)
        }
        return batchSidebarPanel
    }

    private fun createChannelSidebar(): JComponent {
        val channelListModel = DefaultListModel<Any>().apply {
            if (data.channels == 3 || data.channels == 4) addElement("All")
            for (i in 0 until data.channels) addElement(i)
        }
        val channelList = JBList(channelListModel).apply {
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            selectedIndex = selectedChannelIndex ?: 0
            addListSelectionListener {
                selectedChannelIndex = if (selectedValue == "All") null else selectedValue as Int
                updateImage()
            }
        }
        val channelSidebarPanel = JPanel(BorderLayout()).apply {
            add(JLabel("Channel Index"), BorderLayout.NORTH)
            add(JBScrollPane(channelList), BorderLayout.CENTER)
        }
        return channelSidebarPanel
    }

    private fun toggleSidebar(state: Boolean, sidebar: JComponent) {
        if (state) {
            if (currentSidebar != sidebar) {
                currentSidebar?.let { sidebarPanel.remove(it) }
                sidebarPanel.add(sidebar, BorderLayout.CENTER)
                currentSidebar = sidebar
            }
            sidebarPanel.isVisible = true
        } else {
            if (currentSidebar == sidebar) {
                sidebarPanel.remove(sidebar)
                currentSidebar = null
                sidebarPanel.isVisible = false
            }
        }
        sidebarPanel.revalidate()
        sidebarPanel.repaint()
    }


    override fun getDimensionServiceKey() = "com.github.srwi.pycharmpixelglance.dialogs.ImageViewer"

    override fun getPreferredSize() = Dimension(800, 600)

    private fun createCustomActionGroup(): ActionGroup {
        return DefaultActionGroup().apply {
            add(SaveAsPngAction { imageComponent.document.value }.apply {
                templatePresentation.icon = AllIcons.Actions.MenuSaveall
                templatePresentation.text = "Save Image"
                templatePresentation.description = "Save image to file"
            })
            add(CopyToClipboardAction { imageComponent.document.value }.apply {
                templatePresentation.icon = AllIcons.Actions.Copy
                templatePresentation.text = "Copy Image"
                templatePresentation.description = "Copy image to clipboard"
            })
            addSeparator()
            add(ToggleTransparencyChessboardAction().apply {
                templatePresentation.icon = ImageViewerIcons.Chessboard
                templatePresentation.text = "Toggle Chessboard"
                templatePresentation.description = "Toggle transparency chessboard"
            })
            add(ToggleGridAction().apply {
                templatePresentation.icon = AllIcons.Graph.Grid
                templatePresentation.text = "Toggle Grid"
                templatePresentation.description = "Toggle pixel grid"
            })
            addSeparator()
            add(ZoomInAction().apply {
                templatePresentation.icon = AllIcons.General.ZoomIn
                templatePresentation.text = "Zoom In"
                templatePresentation.description = "Zoom in"
            })
            add(ZoomOutAction().apply {
                templatePresentation.icon = AllIcons.General.ZoomOut
                templatePresentation.text = "Zoom Out"
                templatePresentation.description = "Zoom out"
            })
            add(ActualSizeAction().apply {
                templatePresentation.icon = AllIcons.General.ActualZoom
                templatePresentation.text = "Actual Size"
                templatePresentation.description = "Reset zoom to actual image size"
            })
            add(FitZoomToWindowAction().apply {
                templatePresentation.icon = AllIcons.General.FitContent
                templatePresentation.text = "Fit Zoom to Window"
                templatePresentation.description = "Fit zoom to window"
            })
            addSeparator()
            add(ToggleTransposeAction().apply {
                templatePresentation.icon = ImageViewerIcons.Transpose
                templatePresentation.text = "Toggle Transpose (HWC → CHW)"
                templatePresentation.description = "Treat image as CHW instead of HWC"
            })
            add(ToggleReverseChannelsAction().apply {
                templatePresentation.icon = ImageViewerIcons.ReverseChannels
                templatePresentation.text = "Toggle Reverse Channels (RGB → BGR)"
                templatePresentation.description = "Treat image as BGR instead of RGB"
            })
            add(ToggleNormalizeAction().apply {
                templatePresentation.icon = ImageViewerIcons.Normalize
                templatePresentation.text = "Toggle Normalize"
                templatePresentation.description = "Normalize image values"
            })
            add(ToggleApplyColormapAction().apply {
                templatePresentation.icon = ImageViewerIcons.Colormap
                templatePresentation.text = "Toggle Colormap (Viridis)"
                templatePresentation.description = "Apply Viridis colormap to grayscale image"
            })
        }
    }

    override fun dispose() {
        imageComponent.removeMouseWheelListener(wheelAdapter)
        scrollPane.removeMouseWheelListener(wheelAdapter)
        scrollPane.removeComponentListener(resizeAdapter)
        super.dispose()
    }

    private inner class ImageContainerPane(private val imageComponent: ImageComponent) : JBLayeredPane() {
        init {
            add(imageComponent)
            putClientProperty(Magnificator.CLIENT_PROPERTY_KEY, Magnificator { scale, at ->
                val locationBefore = imageComponent.location
                val model = internalZoomModel
                val factor = model.zoomFactor
                model.zoomFactor = scale * factor
                Point(
                    (((at.x - max((if (scale > 1.0) locationBefore.x else 0).toDouble(), 0.0)) * scale).toInt()),
                    (((at.y - max((if (scale > 1.0) locationBefore.y else 0).toDouble(), 0.0)) * scale).toInt())
                )
            })
        }

        private fun centerImageComponent() {
            val point = imageComponent.location
            point.x = (bounds.width - imageComponent.width) / 2
            point.y = (bounds.height - imageComponent.height) / 2
            imageComponent.location = point
        }

        override fun invalidate() {
            centerImageComponent()
            super.invalidate()
        }

        override fun getPreferredSize(): Dimension {
            return imageComponent.size
        }
    }

    private inner class ImageWheelAdapter : MouseWheelListener {
        override fun mouseWheelMoved(e: MouseWheelEvent) {
            val options = OptionsManager.getInstance().options
            val editorOptions = options.editorOptions
            val zoomOptions = editorOptions.zoomOptions
            if (zoomOptions.isWheelZooming && e.isControlDown) {
                if (e.wheelRotation > 0) {
                    internalZoomModel.zoomOut()
                } else {
                    internalZoomModel.zoomIn()
                }
                e.consume()
            }
        }
    }

    private inner class ImageResizeAdapter : ComponentAdapter() {
        override fun componentResized(e: ComponentEvent?) {
            if (!internalZoomModel.isZoomLevelChanged) {
                internalZoomModel.fitZoomToWindow()
            }
        }
    }

    private inner class EditorMouseAdapter : PopupHandler() {
        override fun invokePopup(comp: Component, x: Int, y: Int) {
            val actionManager = ActionManager.getInstance()
            val actionGroup = createCustomActionGroup()
            val menu = actionManager.createActionPopupMenu(ImageEditorActions.ACTION_PLACE, actionGroup)
            val popupMenu = menu.component
            popupMenu.show(comp, x, y)
        }
    }

    override fun getData(dataId: String): Any? {
        if (ImageComponentDecorator.DATA_KEY.`is`(dataId)) {
            return this
        }
        return null
    }

    override fun setTransparencyChessboardVisible(visible: Boolean) {
        imageComponent.isTransparencyChessboardVisible = visible
        repaintImage()
    }

    override fun isTransparencyChessboardVisible(): Boolean {
        return imageComponent.isTransparencyChessboardVisible
    }

    override fun isEnabledForActionPlace(place: String): Boolean {
        return true
    }

    override fun setGridVisible(visible: Boolean) {
        imageComponent.isGridVisible = visible
        repaintImage()
    }

    override fun isGridVisible(): Boolean {
        return imageComponent.isGridVisible
    }

    override fun getZoomModel(): ImageZoomModel {
        return internalZoomModel
    }

    private inner class OptionsChangeListener : PropertyChangeListener {
        override fun propertyChange(evt: PropertyChangeEvent) {
            val options = evt.source as Options
            val editorOptions = options.editorOptions
            val chessboardOptions = editorOptions.transparencyChessboardOptions
            val gridOptions = editorOptions.gridOptions
            imageComponent.transparencyChessboardCellSize = chessboardOptions.cellSize
            imageComponent.transparencyChessboardWhiteColor = chessboardOptions.whiteColor
            imageComponent.setTransparencyChessboardBlankColor(chessboardOptions.blackColor)
            imageComponent.gridLineZoomFactor = gridOptions.lineZoomFactor
            imageComponent.gridLineSpan = gridOptions.lineSpan
            imageComponent.gridLineColor = gridOptions.lineColor
        }
    }
}