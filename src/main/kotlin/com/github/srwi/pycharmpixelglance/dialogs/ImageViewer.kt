package com.github.srwi.pycharmpixelglance.dialogs

import com.github.srwi.pycharmpixelglance.actions.*
import com.github.srwi.pycharmpixelglance.data.DisplayableData
import com.github.srwi.pycharmpixelglance.icons.ImageViewerIcons
import com.intellij.icons.AllIcons
import com.intellij.ide.ActivityTracker
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.PopupHandler
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.*
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
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.*
import kotlin.math.max

class ImageViewer(project: Project, val data: DisplayableData) : DialogWrapper(project), ImageComponentDecorator, DataProvider, Disposable {

    var modifiedData: DisplayableData = data

    // TODO: read default from options
    var normalizeEnabled: Boolean = false
        get() = field
        set(value) {
            field = value
            applyDataModifications()
        }

    var transposeEnabled: Boolean = false
        get() = field
        set(value) {
            field = value
            applyDataModifications()
        }

    var reverseChannelsEnabled: Boolean = false
        get() = field
        set(value) {
            field = value
            applyDataModifications()
        }

    var applyColormapEnabled: Boolean = false
        get() = field
        set(value) {
            field = value
            applyDataModifications()
        }

    var isSidebarVisible: Boolean
        get() = sidebar.isVisible
        set(value) {
            sidebar.isVisible = value
        }

    private val optionsChangeListener: PropertyChangeListener = OptionsChangeListener()
    private val imageComponent: ImageComponent = ImageComponent()
    private val internalZoomModel: ImageZoomModel = ImageZoomModelImpl(imageComponent)
    private val wheelAdapter = ImageWheelAdapter()
    private val resizeAdapter = ImageResizeAdapter()
    private val infoLabel: JLabel = JLabel()
    private var scrollPane: JScrollPane = JBScrollPane()
    private var selectedBatchIndex: Int = 0
    private var selectedChannelIndex: Int? = null
    private lateinit var sidebar: JComponent

    init {
        title = "Image Viewer"  // TODO: replace with variable name, shape and dtype

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

        updateImage()
        smartZoom()
    }

    private fun applyDataModifications() {
        modifiedData = data
        if (transposeEnabled) {
            modifiedData = modifiedData.transpose()
        }
        if (normalizeEnabled) {
            modifiedData = modifiedData.normalize()
        }
        if (reverseChannelsEnabled) {
            modifiedData = modifiedData.reverseChannels()
        }
        updateImage()
        repaintImage()
    }

    private fun updateImage() {
        val colormappedData = if (applyColormapEnabled) {
            modifiedData.applyColormap()
        } else {
            modifiedData
        }
        val image = colormappedData.getBuffer(selectedBatchIndex, selectedChannelIndex)
        val document: ImageDocument = imageComponent.document
        document.value = image
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
        val mainPanel = JPanel(BorderLayout())
        val view = ImageContainerPane(imageComponent)
        scrollPane = ScrollPaneFactory.createScrollPane(view)
        scrollPane.border = null
        scrollPane.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
        scrollPane.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        scrollPane.addMouseWheelListener(wheelAdapter)
        scrollPane.addComponentListener(resizeAdapter)

        sidebar = createSidebar()
        mainPanel.add(scrollPane, BorderLayout.CENTER)
        mainPanel.add(sidebar, BorderLayout.EAST)

        return mainPanel
    }

    override fun createNorthPanel(): JComponent {
        val actionManager = ActionManager.getInstance()
        val actionGroup = createCustomActionGroup()
        val actionToolbar = actionManager.createActionToolbar(
            ImageEditorActions.ACTION_PLACE, actionGroup, true
        )
        val toolbarPanel = actionToolbar.component

        val toggleSidebarAction = ToggleSidebarAction(this)
        val toggleSidebarButton = actionManager.createActionToolbar(
            "ToggleSidebar", DefaultActionGroup(toggleSidebarAction), true
        ).component

        val topPanel = JPanel(BorderLayout())
        topPanel.add(toolbarPanel, BorderLayout.WEST)
        topPanel.add(toggleSidebarButton, BorderLayout.EAST)
        topPanel.add(infoLabel, BorderLayout.CENTER)

        return topPanel
    }

    override fun createSouthPanel(): JComponent? {
        return null
    }

    private fun createSidebar(): JComponent {
        val tabbedPane = JBTabbedPane()

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
        val batchScrollPane = JBScrollPane(batchList)
        tabbedPane.addTab("B", ImageViewerIcons.Layers, batchScrollPane)

        val channelListModel = DefaultListModel<Any>().apply {
            addElement("All")
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
        val channelScrollPane = JBScrollPane(channelList)
        tabbedPane.addTab("C", ImageViewerIcons.Channels, channelScrollPane)

        return tabbedPane
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
        return ThumbnailViewActions.ACTION_PLACE != place
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