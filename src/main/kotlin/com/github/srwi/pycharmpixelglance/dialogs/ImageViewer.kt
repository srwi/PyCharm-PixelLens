package com.github.srwi.pycharmpixelglance.dialogs

import com.github.srwi.pycharmpixelglance.actions.CopyToClipboardAction
import com.github.srwi.pycharmpixelglance.actions.FitZoomToWindowAction
import com.github.srwi.pycharmpixelglance.actions.SaveAsPngAction
import com.github.srwi.pycharmpixelglance.actions.ToggleInvertAction
import com.github.srwi.pycharmpixelglance.data.DisplayableData
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.ui.PopupHandler
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBLayeredPane
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.Magnificator
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

internal class ImageViewer(private val data: DisplayableData) : ImageComponentDecorator, Disposable, PersistentDialogWrapper(), DataProvider {

    private val optionsChangeListener: PropertyChangeListener = OptionsChangeListener()
    private val imageComponent: ImageComponent = ImageComponent()
    private val internalZoomModel: ImageZoomModel = ImageZoomModelImpl(imageComponent)
    private val wheelAdapter = ImageWheelAdapter()
    private val resizeAdapter = ImageResizeAdapter()
    private val infoLabel: JLabel = JLabel()
    private var scrollPane: JScrollPane = JBScrollPane()
    private var batchIndex: Int = 0
    private var channelIndex: Int = -1

    var invertEnabled: Boolean = false
        set(value) {
            field = value
            updateImage()
        }

    init {
        title = "Image Editor"

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

    private fun applyModifiers(data: DisplayableData): DisplayableData {
        var modified = data
        if (invertEnabled) {
            modified = modified.invert()
        }
        return modified
    }

    private fun selectBatchAndChannel(batchIndex: Int, channelIndex: Int): DisplayableData {
        // TODO: implement batch/channel logic
        val data = this.data
        return data
    }

    private fun updateImage(repaint: Boolean = true) {
        val image = applyModifiers(selectBatchAndChannel(batchIndex, channelIndex)).getBuffer()
        val document: ImageDocument = imageComponent.document
        document.value = image
        if (repaint) {
            repaintImage()
        }
    }

    private fun smartZoom(repaint: Boolean = true) {
        val zoomModel = internalZoomModel as ImageZoomModelImpl
        zoomModel.smartZoom(scrollPane.viewport.width, scrollPane.viewport.height)
        if (repaint) {
            repaintImage()
        }
    }

    private fun repaintImage() {
        contentPane.repaint()
    }

    override fun createCenterPanel(): JComponent {
        val view = ImageContainerPane(imageComponent)
        scrollPane = ScrollPaneFactory.createScrollPane(view)
        scrollPane.border = null
        scrollPane.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
        scrollPane.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        scrollPane.addMouseWheelListener(wheelAdapter)
        scrollPane.addComponentListener(resizeAdapter)
        return scrollPane
    }

    override fun createNorthPanel(): JComponent {
        val actionManager = ActionManager.getInstance()
        val actionGroup = createCustomActionGroup()
        val actionToolbar = actionManager.createActionToolbar(
            ImageEditorActions.ACTION_PLACE, actionGroup, true
        )
        val toolbarPanel = actionToolbar.component

        val topPanel = JPanel(BorderLayout())
        topPanel.add(toolbarPanel, BorderLayout.WEST)
        topPanel.add(infoLabel, BorderLayout.EAST)

        return topPanel
    }

    override fun createSouthPanel(): JComponent? {
        return null
    }

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
                templatePresentation.icon = AllIcons.Gutter.Colors
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
            add(ToggleInvertAction().apply {
                templatePresentation.icon = AllIcons.General.ChevronDown
                templatePresentation.text = "Toggle Invert"
                templatePresentation.description = "Toggle image inversion"
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