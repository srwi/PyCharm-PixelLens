package com.github.srwi.pixellens.dialogs

import com.github.srwi.pixellens.UserSettings
import com.github.srwi.pixellens.actions.ToggleNormalizeAction
import com.github.srwi.pixellens.data.Batch
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.GotItTooltip
import com.intellij.ui.JBColor
import com.intellij.ui.PopupHandler
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBLayeredPane
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.Magnificator
import com.intellij.ui.components.TwoSideComponent
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.*
import org.intellij.images.editor.ImageDocument
import org.intellij.images.editor.ImageZoomModel
import org.intellij.images.editor.actionSystem.ImageEditorActions
import org.intellij.images.options.OptionsManager
import org.intellij.images.ui.ImageComponent
import org.intellij.images.ui.ImageComponentDecorator
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.Point
import java.awt.event.*
import java.awt.image.BufferedImage
import javax.swing.*
import javax.swing.border.Border
import kotlin.coroutines.cancellation.CancellationException
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
            updateImage().invokeOnCompletion {
                smartZoom()
            }
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

    var selectedChannelIndex: Int? = if (batch.data.supportsMultiChannelDisplay()) null else 0
        private set

    private var selectedBatchIndex: Int = 0

    private var didInitialUpdate = false
    private var updateJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val editorMouseWheelAdapter = EditorMouseWheelAdapter()
    private val editorResizeAdapter = EditorResizeAdapter()
    private val editorActionPopupAdapter = EditorActionPopupAdapter()
    private val mouseMotionAdapter = EditorMouseMotionAdapter()
    private val mouseExitAdapter = EditorMouseExitAdapter()

    private var scrollPane = JBScrollPane()
    private val coordinateValueLabel = JLabel()
    private val sidebar = Sidebar()
    private val sidebarPanel = JPanel(BorderLayout())
    private val imageComponent = ImageComponent()
    private val internalZoomModel = ImageZoomModelImpl(imageComponent)

    private lateinit var mainToolbar: ActionToolbar
    private lateinit var sidebarToolbar: ActionToolbar

    init {
        title = batch.expression
        isModal = false

        val editorOptions = OptionsManager.getInstance().options.editorOptions
        val chessboardOptions = editorOptions.transparencyChessboardOptions
        val gridOptions = editorOptions.gridOptions
        imageComponent.addMouseListener(editorActionPopupAdapter)
        imageComponent.setTransparencyChessboardBlankColor(chessboardOptions.blackColor)
        imageComponent.transparencyChessboardCellSize = chessboardOptions.cellSize
        imageComponent.transparencyChessboardWhiteColor = chessboardOptions.whiteColor
        imageComponent.gridLineZoomFactor = gridOptions.lineZoomFactor
        imageComponent.gridLineSpan = gridOptions.lineSpan
        imageComponent.gridLineColor = gridOptions.lineColor
        imageComponent.isBorderVisible = false

        init()

        updateImage()
    }

    private fun updateImage(): Job {
        updateJob?.cancel()
        updateJob = coroutineScope.launch {
            try {
                // On the first repaint we want to ensure the smart zoom is applied.
                // Therefore the coroutine should be non-cancellable
                val renderedImage = withContext(if (didInitialUpdate) Dispatchers.Default else (Dispatchers.Default + NonCancellable)) {
                    batch.data.getImage(selectedBatchIndex, selectedChannelIndex)
                }

                withContext(if (didInitialUpdate) Dispatchers.Main else (Dispatchers.Main + NonCancellable)) {
                    val document: ImageDocument = imageComponent.document
                    document.value = renderedImage.image
                    mainToolbar.updateActionsImmediately()
                    sidebarToolbar.updateActionsImmediately()

                    if (!didInitialUpdate) {
                        initialUpdateRoutine(renderedImage.valuesClipped)
                        didInitialUpdate = true
                    }

                    repaintImage()
                }
            } catch (e: CancellationException) {
                // Task was cancelled, do nothing
            }
        }
        return updateJob!!
    }

    private fun initialUpdateRoutine(valuesClipped: Boolean) {
        if (batch.data.batchSize > 1) {
            activeSidebar = SidebarType.BatchSidebar
        }
        if (valuesClipped) {
            notifyAboutClippedValues()
        }
        smartZoom()
    }

    private fun repaintImage() {
        internalZoomModel.repaintImageComponent()
    }

    private fun smartZoom() {
        internalZoomModel.smartZoom(scrollPane.viewport.width, scrollPane.viewport.height)
    }

    private fun setSidebarVisibility(visible: Boolean) {
        sidebarPanel.isVisible = visible
        sidebarPanel.revalidate()
        sidebarPanel.repaint()
    }

    private fun notifyAboutClippedValues() {
        val normalizeActionComponent = mainToolbar.component.components.find { it is ActionButton && it.action is ToggleNormalizeAction } as ActionButton?
        normalizeActionComponent?.let {
            GotItTooltip("normalizeTooltip", "Some values exceed the displayable range. Please consider enabling normalization.", this)
                .withTimeout(5000)
                .withShowCount(Int.MAX_VALUE)  // Show each time
                .show(it, GotItTooltip.BOTTOM_MIDDLE)
        }
    }

    override fun createCenterPanel(): JComponent {
        val contentPanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createEmptyBorder()
            add(createImagePanel(), BorderLayout.CENTER)
            add(createRightPanel(), BorderLayout.EAST)
            minimumSize = Dimension(0, 400)
        }
        return contentPanel
    }

    override fun createContentPaneBorder(): Border {
        return JBUI.Borders.empty()
    }

    override fun createNorthPanel(): JComponent {
        val actionManager = ActionManager.getInstance()

        val actionGroup = actionManager.getAction("MainToolbarActionGroup") as ActionGroup
        mainToolbar = actionManager.createActionToolbar("MainToolbar", actionGroup, true)
        mainToolbar.apply { setReservePlaceAutoPopupIcon(false) }

        val sidebarToggleGroup = actionManager.getAction("SidebarToolbarActionGroup") as ActionGroup
        sidebarToolbar = actionManager.createActionToolbar("SidebarToolbar", sidebarToggleGroup, true)
        sidebarToolbar.apply { setReservePlaceAutoPopupIcon(false) }

        val twoSideComponent = TwoSideComponent(mainToolbar.component, sidebarToolbar.component)

        return JPanel(BorderLayout()).apply {
            add(twoSideComponent, BorderLayout.CENTER)
            border = BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.LIGHT_GRAY)
            minimumSize = Dimension(650, 0)
        }
    }

    override fun createSouthPanel(): JComponent {
        val shapeLabel = JLabel(batch.shape.joinToString("x")).apply {
            border = JBUI.Borders.empty(5)
        }
        val dtypeLabel = JLabel(batch.dtype).apply {
            border = JBUI.Borders.empty(5)
        }
        val rightPanel = JPanel(BorderLayout()).apply {
            add(shapeLabel, BorderLayout.WEST)
            add(dtypeLabel, BorderLayout.EAST)
        }
        val statusBar = JPanel(BorderLayout()).apply {
            border = BorderFactory.createMatteBorder(1, 0, 0, 0, JBColor.LIGHT_GRAY)
        }
        coordinateValueLabel.apply {
            border = JBUI.Borders.empty(5)
        }
        statusBar.add(coordinateValueLabel, BorderLayout.WEST)
        statusBar.add(rightPanel, BorderLayout.EAST)

        imageComponent.addMouseMotionListener(mouseMotionAdapter)
        imageComponent.addMouseListener(mouseExitAdapter)

        return statusBar
    }

    private fun createRightPanel(): JComponent {
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

        sidebarPanel.apply {
            add(sidebar.getComponent(), BorderLayout.CENTER)
            isVisible = false
        }

        return sidebarPanel
    }

    private fun createImagePanel(): JComponent {
        val view = ImageContainerPane(imageComponent)
        scrollPane = ScrollPaneFactory.createScrollPane(view) as JBScrollPane
        scrollPane.border = null
        scrollPane.viewport.background = EditorColorsManager.getInstance().globalScheme.defaultBackground
        scrollPane.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
        scrollPane.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        scrollPane.addMouseWheelListener(editorMouseWheelAdapter)
        scrollPane.addComponentListener(editorResizeAdapter)
        return scrollPane
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

    private inner class EditorMouseWheelAdapter : MouseWheelListener {
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

    private inner class EditorResizeAdapter : ComponentAdapter() {
        override fun componentResized(e: ComponentEvent?) {
            if (!internalZoomModel.isZoomLevelChanged) {
                internalZoomModel.fitZoomToWindow()
            }
        }
    }

    private inner class EditorActionPopupAdapter : PopupHandler() {
        override fun invokePopup(comp: Component, x: Int, y: Int) {
            val actionManager = ActionManager.getInstance()
            val actionGroup = actionManager.getAction("MainToolbarActionGroup") as ActionGroup
            val menu = actionManager.createActionPopupMenu(ImageEditorActions.ACTION_PLACE, actionGroup)
            val popupMenu = menu.component
            popupMenu.show(comp, x, y)
        }
    }

    private inner class EditorMouseMotionAdapter : MouseMotionAdapter() {
        override fun mouseMoved(e: MouseEvent) {
            val zoomFactor = internalZoomModel.zoomFactor

            val originalX = (e.x / zoomFactor).toInt().coerceIn(0 until batch.data.width)
            val originalY = (e.y / zoomFactor).toInt().coerceIn(0 until batch.data.height)

            val value = batch.data.getValue(selectedBatchIndex, originalX, originalY, selectedChannelIndex)
            val formattedValue = Utils.formatArrayOrScalar(value)

            coordinateValueLabel.text = "($originalX, $originalY): $formattedValue"
        }
    }

    private inner class EditorMouseExitAdapter : MouseAdapter() {
        override fun mouseExited(e: MouseEvent) {
            coordinateValueLabel.text = null
        }
    }

    fun getDisplayedImage(): BufferedImage {
        return imageComponent.document.value
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

    override fun getDimensionServiceKey() = "com.github.srwi.pixellens.dialogs.ImageViewer"

    override fun dispose() {
        updateJob?.cancel()
        imageComponent.removeMouseMotionListener(mouseMotionAdapter)
        imageComponent.removeMouseListener(mouseExitAdapter)
        imageComponent.removeMouseListener(editorActionPopupAdapter)
        scrollPane.removeMouseWheelListener(editorMouseWheelAdapter)
        scrollPane.removeComponentListener(editorResizeAdapter)
        super.dispose()
    }
}