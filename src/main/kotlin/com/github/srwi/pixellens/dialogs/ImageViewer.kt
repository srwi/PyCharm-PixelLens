package com.github.srwi.pixellens.dialogs

import com.github.srwi.pixellens.UserSettings
import com.github.srwi.pixellens.actions.*
import com.github.srwi.pixellens.data.Utils
import com.github.srwi.pixellens.icons.ImageViewerIcons
import com.github.srwi.pixellens.imageProviders.Batch
import com.intellij.icons.AllIcons
import com.intellij.ide.ActivityTracker
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.ui.PopupHandler
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBLayeredPane
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.Magnificator
import com.intellij.ui.components.TwoSideComponent
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
import org.intellij.images.ui.ImageComponent
import org.intellij.images.ui.ImageComponentDecorator
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.Point
import java.awt.event.*
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.*
import javax.swing.border.Border
import kotlin.math.max

class ImageViewer(project: Project, val batch: Batch) : DialogWrapper(project), ImageComponentDecorator, DataProvider, Disposable {

    var normalizeEnabled: Boolean = batch.data.normalized
        set(value) {
            if (field == value) return
            field = value
            UserSettings.normalizeEnabled = value
            batch.data.normalized = value
            updateImage()
        }

    var transposeEnabled: Boolean = batch.data.channelsFirst
        set(value) {
            if (field == value) return
            field = value
            batch.data.channelsFirst = value
            sidebar.updateChannelList(batch.data.channels)
            internalZoomModel.isZoomLevelChanged = true  // TODO: restore fit zoom to window
            updateImage()
        }

    var reverseChannelsEnabled: Boolean = batch.data.reversedChannels
        set(value) {
            if (field == value) return
            field = value
            batch.data.reversedChannels = value
            updateImage()
        }

    var applyColormapEnabled: Boolean = batch.data.grayscaleColormap
        set(value) {
            if (field == value) return
            field = value
            batch.data.grayscaleColormap = value
            updateImage()
        }

    var activeSidebar: SidebarType? = null
        set(value) {
            field = value
            if (value != null) {
                sidebar.showPanel(value)
                setSidebarVisibility(true)
            } else {
                setSidebarVisibility(false)
            }
        }

    private var selectedBatchIndex: Int = 0

    var selectedChannelIndex: Int? = if (batch.data.supportsMultiChannelDisplay()) null else 0
        private set

    private val optionsChangeListener: PropertyChangeListener = OptionsChangeListener()
    private val imageComponent: ImageComponent = ImageComponent()
    private val internalZoomModel: ImageZoomModelImpl = ImageZoomModelImpl(imageComponent)
    private val wheelAdapter = ImageWheelAdapter()
    private val resizeAdapter = ImageResizeAdapter()
    private var scrollPane: JScrollPane = JBScrollPane()

    private lateinit var sidebar: Sidebar
    private lateinit var sidebarPanel: JPanel

    init {
        title = batch.name
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
        val image = batch.data.getImage(selectedBatchIndex, selectedChannelIndex)
        val document: ImageDocument = imageComponent.document
        document.value = image
        if (repaint) repaintImage()
        ActivityTracker.getInstance().inc()  // Update toolbar actions
    }

    private fun setSidebarVisibility(visible: Boolean) {
        sidebarPanel.isVisible = visible
        sidebarPanel.revalidate()
        sidebarPanel.repaint()
    }

    private fun smartZoom() {
        val zoomModel = internalZoomModel
        zoomModel.smartZoom(scrollPane.viewport.width, scrollPane.viewport.height)
    }

    private fun repaintImage() {
        internalZoomModel.repaintImageComponent()
    }

    override fun createCenterPanel(): JComponent {
        val contentPanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createEmptyBorder()
            add(createImagePanel(), BorderLayout.CENTER)
            add(createRightPanel(), BorderLayout.EAST)
            minimumSize = Dimension(0, 600)
        }
        return contentPanel
    }

    override fun createContentPaneBorder(): Border {
        return JBUI.Borders.empty()
    }

    private fun createRightPanel(): JComponent {
        sidebar = Sidebar()
        sidebar.updateChannelList(batch.data.channels)
        sidebar.updateBatchList(batch.data.batchSize)
        sidebar.setSelectedBatchIndex(selectedBatchIndex)
        sidebar.setSelectedChannelIndex(selectedChannelIndex)

        sidebar.onBatchIndexChanged { index ->
            selectedBatchIndex = index
            updateImage()
        }

        sidebar.onChannelIndexChanged { index ->
            selectedChannelIndex = index
            updateImage()
        }

        sidebarPanel = JPanel(BorderLayout()).apply {
            add(sidebar.getComponent(), BorderLayout.CENTER)
            isVisible = false
        }

        return sidebarPanel
    }

    override fun createNorthPanel(): JComponent {
        val actionManager = ActionManager.getInstance()
        val actionGroup = createCustomActionGroup()
        val actionToolbar = actionManager.createActionToolbar("MainToolbar", actionGroup, true)
        actionToolbar.apply { setReservePlaceAutoPopupIcon(false) }

        val sidebarToggleGroup = DefaultActionGroup().apply {
            add(ToggleBatchSidebarAction())
            add(ToggleChannelSidebarAction())
        }
        val sidebarToggleToolbar = actionManager.createActionToolbar("SidebarToolbar", sidebarToggleGroup, true)
        sidebarToggleToolbar.apply { setReservePlaceAutoPopupIcon(false) }

        val twoSideComponent = TwoSideComponent(actionToolbar.component, sidebarToggleToolbar.component)
        return JPanel(BorderLayout()).apply {
            border = BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.LIGHT_GRAY)
            add(twoSideComponent, BorderLayout.CENTER)
            minimumSize = Dimension(650, 0)
        }
    }

    override fun createSouthPanel(): JComponent {
        val statusBar = JPanel(BorderLayout()).apply {
            border = BorderFactory.createMatteBorder(1, 0, 0, 0, JBColor.LIGHT_GRAY)
        }
        val coordinateValueLabel = JLabel().apply {
            border = JBUI.Borders.empty(5)
        }
        val shapeLabel = JLabel(batch.metadata.shape.joinToString("x")).apply {
            border = JBUI.Borders.empty(5)
        }
        val dtypeLabel = JLabel(batch.metadata.dtype).apply {
            border = JBUI.Borders.empty(5)
        }

        val rightPanel = JPanel(BorderLayout()).apply {
            add(shapeLabel, BorderLayout.WEST)
            add(dtypeLabel, BorderLayout.EAST)
        }

        statusBar.add(coordinateValueLabel, BorderLayout.WEST)
        statusBar.add(rightPanel, BorderLayout.EAST)

        imageComponent.addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseMoved(e: MouseEvent) {
                val mouseX = e.x
                val mouseY = e.y
                val zoomFactor = internalZoomModel.zoomFactor

                val originalX = (mouseX / zoomFactor).toInt()
                val originalY = (mouseY / zoomFactor).toInt()

                val value = getDataValueAt(originalX, originalY)
                val formattedValue = Utils.formatArrayOrScalar(value)

                coordinateValueLabel.text = "($originalX, $originalY): $formattedValue"
            }
        })

        imageComponent.addMouseListener(object : MouseAdapter() {
            override fun mouseExited(e: MouseEvent) {
                coordinateValueLabel.text = null
            }
        })

        return statusBar
    }

    private fun getDataValueAt(x: Int, y: Int): Any {
        return batch.data.getValue(selectedBatchIndex, x, y, selectedChannelIndex)
    }

    private fun createImagePanel(): JComponent {
        val view = ImageContainerPane(imageComponent)
        scrollPane = ScrollPaneFactory.createScrollPane(view)
        scrollPane.border = null
        scrollPane.viewport.background = EditorColorsManager.getInstance().globalScheme.defaultBackground
        scrollPane.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
        scrollPane.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        scrollPane.addMouseWheelListener(wheelAdapter)
        scrollPane.addComponentListener(resizeAdapter)
        return scrollPane
    }

    override fun getDimensionServiceKey() = "com.github.srwi.pixellens.dialogs.ImageViewer"

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