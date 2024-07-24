package com.github.srwi.pycharmpixelglance.dialogs

import com.github.srwi.pycharmpixelglance.actions.CopyToClipboardAction
import com.github.srwi.pycharmpixelglance.actions.FitZoomToWindowAction
import com.github.srwi.pycharmpixelglance.actions.SaveAsPngAction
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.PopupHandler
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBLayeredPane
import com.intellij.ui.components.Magnificator
import org.intellij.images.actions.ToggleTransparencyChessboardAction
import org.intellij.images.editor.ImageZoomModel
import org.intellij.images.editor.actionSystem.ImageEditorActions
import org.intellij.images.editor.actions.ActualSizeAction
import org.intellij.images.editor.actions.ToggleGridAction
import org.intellij.images.editor.actions.ZoomInAction
import org.intellij.images.editor.actions.ZoomOutAction
import org.intellij.images.options.EditorOptions
import org.intellij.images.options.OptionsManager
import org.intellij.images.ui.ImageComponent
import org.intellij.images.ui.ImageComponentDecorator
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.Point
import java.awt.event.MouseWheelEvent
import java.awt.event.MouseWheelListener
import javax.swing.*
import kotlin.math.max
import kotlin.math.min
import com.intellij.ide.util.PropertiesComponent

internal class ImageEditorUI(
    project: Project,
    private val editor: ImageComponentDecorator,
    editorOptions: EditorOptions
) : DialogWrapper(project), DataProvider {

    val zoomModel: ImageZoomModel = ImageZoomModelImpl()
    val imageComponent: ImageComponent = ImageComponent()
    private var scrollPane: JScrollPane = JScrollPane()
    private val wheelAdapter = ImageWheelAdapter()
    private val infoLabel: JLabel = JLabel()

    companion object {
        private const val WINDOW_WIDTH_KEY = "ImageEditorUI.WindowWidth"
        private const val WINDOW_HEIGHT_KEY = "ImageEditorUI.WindowHeight"
        private const val DEFAULT_WIDTH = 800
        private const val DEFAULT_HEIGHT = 600
    }

    init {
        title = "Image Editor"

        val size = loadWindowSize()
        setSize(size.width, size.height)

        init()

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

        updateInfo()

        SwingUtilities.invokeLater {
            zoomModel.fitZoomToWindow()
        }
    }

    override fun createCenterPanel(): JComponent {
        val view = ImageContainerPane(imageComponent)
        scrollPane = ScrollPaneFactory.createScrollPane(view)
        scrollPane.border = null
        scrollPane.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
        scrollPane.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        scrollPane.addMouseWheelListener(wheelAdapter)

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
        infoLabel.border = IdeBorderFactory.createEmptyBorder(0, 0, 0, 2)
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
                templatePresentation.text = "Reset Zoom"
                templatePresentation.description = "Reset zoom to original size"
            })
            add(FitZoomToWindowAction().apply {
                templatePresentation.icon = AllIcons.General.FitContent
                templatePresentation.text = "Fit to Window"
                templatePresentation.description = "Fit image to window"
            })
        }
    }

    private fun updateInfo() {
        val document = imageComponent.document
        val image = document.value
//        if (image != null) {
//            val colorModel = image.colorModel
//            var format = document.format
//            format = format?.uppercase() ?: ImagesBundle.message("unknown.format")
//            val file = editor.file
//            infoLabel.text = ImagesBundle.message(
//                "image.info",
//                image.width, image.height, format,
//                colorModel.pixelSize, if (file != null) StringUtil.formatFileSize(file.length) else ""
//            )
//        } else {
//            infoLabel.text = null
//        }
    }

    public override fun dispose() {
        saveWindowSize(size)
        imageComponent.removeMouseWheelListener(wheelAdapter)
        super.dispose()
    }

    private fun loadWindowSize(): Dimension {
        val properties = PropertiesComponent.getInstance()
        val width = properties.getInt(WINDOW_WIDTH_KEY, DEFAULT_WIDTH)
        val height = properties.getInt(WINDOW_HEIGHT_KEY, DEFAULT_HEIGHT)
        return Dimension(width, height)
    }

    private fun saveWindowSize(size: Dimension) {
        val properties = PropertiesComponent.getInstance()
        properties.setValue(WINDOW_WIDTH_KEY, size.width, DEFAULT_WIDTH)
        properties.setValue(WINDOW_HEIGHT_KEY, size.height, DEFAULT_HEIGHT)
    }

    private inner class ImageContainerPane(private val imageComponent: ImageComponent) : JBLayeredPane() {
        init {
            add(imageComponent)
            putClientProperty(Magnificator.CLIENT_PROPERTY_KEY, Magnificator { scale, at ->
                val locationBefore = imageComponent.location
                val model = editor.zoomModel
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
                    zoomModel.zoomOut()
                } else {
                    zoomModel.zoomIn()
                }
                e.consume()
            }
        }
    }

    private inner class ImageZoomModelImpl : ImageZoomModel {
        private var myZoomLevelChanged = false
        private var zoomFactor = 1.0

        override fun getZoomFactor(): Double = zoomFactor

        override fun setZoomFactor(zoomFactor: Double) {
            val oldZoomFactor = this.zoomFactor
            if (oldZoomFactor != zoomFactor) {
                this.zoomFactor = zoomFactor
                updateImageComponentSize()
                imageComponent.revalidate()
                imageComponent.repaint()
                myZoomLevelChanged = false
                imageComponent.firePropertyChange("ImageEditor.zoomFactor", oldZoomFactor, zoomFactor)
            }
        }

        override fun fitZoomToWindow() {
            val image = imageComponent.document.value ?: return
            val verticalScrollBarWidth = scrollPane.verticalScrollBar.preferredSize.width
            val horizontalScrollBarHeight = scrollPane.horizontalScrollBar.preferredSize.height
            val adjustedWidth = scrollPane.viewport.width - verticalScrollBarWidth
            val adjustedHeight = scrollPane.viewport.height - horizontalScrollBarHeight
            val widthRatio = adjustedWidth.toDouble() / image.width
            val heightRatio = adjustedHeight.toDouble() / image.height
            val newZoomFactor = min(widthRatio, heightRatio)
            setZoomFactor(newZoomFactor)
            myZoomLevelChanged = false
        }

        private val minimumZoomFactor: Double
            get() {
                val bounds = imageComponent.document.bounds
                val factor = bounds?.let { 1.0 / it.width } ?: 0.0
                return max(factor, ImageZoomModel.MICRO_ZOOM_LIMIT)
            }

        private val maximumZoomFactor: Double
            get() = min(ImageZoomModel.MACRO_ZOOM_LIMIT, Double.MAX_VALUE)

        override fun zoomOut() {
            setZoomFactor(getNextZoomOut())
            myZoomLevelChanged = true
        }

        override fun zoomIn() {
            setZoomFactor(getNextZoomIn())
            myZoomLevelChanged = true
        }

        private fun getNextZoomOut(): Double {
            var factor = zoomFactor
            if (factor > 1.0) {
                factor /= 2.0
                factor = max(factor, 1.0)
            } else {
                factor /= 1.5
            }
            return max(factor, minimumZoomFactor)
        }

        private fun getNextZoomIn(): Double {
            var factor = zoomFactor
            if (factor >= 1.0) {
                factor *= 2.0
            } else {
                factor *= 1.5
                factor = min(factor, 1.0)
            }
            return min(factor, maximumZoomFactor)
        }

        override fun setZoomLevelChanged(value: Boolean) {
            myZoomLevelChanged = value
        }

        override fun canZoomOut(): Boolean {
            return zoomFactor - 1e-14 > minimumZoomFactor
        }

        override fun canZoomIn(): Boolean {
            return zoomFactor < maximumZoomFactor
        }

        override fun isZoomLevelChanged(): Boolean {
            return myZoomLevelChanged
        }

        private fun updateImageComponentSize() {
            val image = imageComponent.document.value
            image?.let {
                val newWidth = (it.width * zoomFactor).toInt()
                val newHeight = (it.height * zoomFactor).toInt()
                imageComponent.canvasSize = Dimension(newWidth, newHeight)
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
            return editor
        }
        return null
    }
}
