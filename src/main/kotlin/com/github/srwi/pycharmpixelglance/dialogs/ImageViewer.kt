package com.github.srwi.pycharmpixelglance.dialogs

import com.github.srwi.pycharmpixelglance.actions.CopyToClipboardAction
import com.github.srwi.pycharmpixelglance.actions.FitZoomToWindowAction
import com.github.srwi.pycharmpixelglance.actions.SaveAsPngAction
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.ui.PopupHandler
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBLayeredPane
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
import java.awt.event.MouseWheelEvent
import java.awt.event.MouseWheelListener
import java.awt.image.BufferedImage
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.*
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

internal class ImageViewer(image: BufferedImage) : ImageComponentDecorator, Disposable, PersistentDialogWrapper(), DataProvider {

    private val optionsChangeListener: PropertyChangeListener = OptionsChangeListener()
    val internalZoomModel: ImageZoomModel = ImageZoomModelImpl()
    val imageComponent: ImageComponent = ImageComponent()
    private var scrollPane: JScrollPane = JScrollPane()
    private val wheelAdapter = ImageWheelAdapter()
    private val infoLabel: JLabel = JLabel()

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

        setImage(image)

        SwingUtilities.invokeLater {
            internalZoomModel.fitZoomToWindow()
        }
    }

    private fun setImage(image: BufferedImage) {
        val document: ImageDocument = imageComponent.document
        try {
            val previousImage = document.value
            document.value = image
            if (previousImage == null || !internalZoomModel.isZoomLevelChanged) {
                val options = OptionsManager.getInstance().options
                val zoomOptions = options.editorOptions.zoomOptions
                internalZoomModel.zoomFactor = 1.0
                if (zoomOptions.isSmartZooming) {
                    val prefferedSize = zoomOptions.prefferedSize
                    if (prefferedSize.width > image.width && prefferedSize.height > image.height) {
                        val factor = (prefferedSize.getWidth() / image.width.toDouble() + prefferedSize.getHeight() / image.height.toDouble()) / 2.0
                        internalZoomModel.zoomFactor = ceil(factor)
                    }
                }
            }
        } catch (e: Exception) {
            document.value = null
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

    override fun dispose() {
        imageComponent.removeMouseWheelListener(wheelAdapter)
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

    private inner class ImageZoomModelImpl : ImageZoomModel {
        private var zoomLevelChanged = false
        private var zoomFactor = 1.0

        override fun getZoomFactor(): Double = zoomFactor

        override fun setZoomFactor(zoomFactor: Double) {
            val oldZoomFactor = this.zoomFactor
            if (oldZoomFactor != zoomFactor) {
                this.zoomFactor = zoomFactor
                updateImageComponentSize()
                imageComponent.revalidate()
                imageComponent.repaint()
                zoomLevelChanged = false
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
            zoomLevelChanged = false
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
            zoomLevelChanged = true
        }

        override fun zoomIn() {
            setZoomFactor(getNextZoomIn())
            zoomLevelChanged = true
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
            zoomLevelChanged = value
        }

        override fun canZoomOut(): Boolean {
            return zoomFactor - 1e-14 > minimumZoomFactor
        }

        override fun canZoomIn(): Boolean {
            return zoomFactor < maximumZoomFactor
        }

        override fun isZoomLevelChanged(): Boolean {
            return zoomLevelChanged
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
            return this
        }
        return null
    }

    override fun setTransparencyChessboardVisible(visible: Boolean) {
        imageComponent.isTransparencyChessboardVisible = visible
        contentPane.repaint()
    }

    override fun isTransparencyChessboardVisible(): Boolean {
        return imageComponent.isTransparencyChessboardVisible
    }

    override fun isEnabledForActionPlace(place: String): Boolean {
        return ThumbnailViewActions.ACTION_PLACE != place
    }

    override fun setGridVisible(visible: Boolean) {
        imageComponent.isGridVisible = visible
        contentPane.repaint()
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