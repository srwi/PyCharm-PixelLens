package com.github.srwi.pycharmpixelglance.dialogs

import com.intellij.ide.CopyProvider
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.PopupHandler
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBLayeredPane
import com.intellij.ui.components.Magnificator
import com.intellij.util.ui.UIUtil
import org.intellij.images.editor.ImageEditor
import org.intellij.images.editor.ImageZoomModel
import org.intellij.images.editor.actionSystem.ImageEditorActions
import org.intellij.images.options.EditorOptions
import org.intellij.images.options.OptionsManager
import org.intellij.images.ui.ImageComponent
import org.intellij.images.ui.ImageComponentDecorator
import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.event.MouseWheelListener
import java.awt.image.BufferedImage
import java.io.IOException
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants
import javax.swing.SwingConstants
import kotlin.math.max
import kotlin.math.min

internal class ImageEditorUI(private val editor: ImageEditor, editorOptions: EditorOptions) : JPanel(), DataProvider, CopyProvider {
    val zoomModel: ImageZoomModel = ImageZoomModelImpl()
    val imageComponent: ImageComponent = ImageComponent()
    private val wheelAdapter = ImageWheelAdapter()
    private val infoLabel: JLabel

    init {
        // Set options
        val chessboardOptions = editorOptions.transparencyChessboardOptions
        val gridOptions = editorOptions.gridOptions
        imageComponent.transparencyChessboardCellSize = chessboardOptions.cellSize
        imageComponent.transparencyChessboardWhiteColor = chessboardOptions.whiteColor
        imageComponent.setTransparencyChessboardBlankColor(chessboardOptions.blackColor)
        imageComponent.gridLineZoomFactor = gridOptions.lineZoomFactor
        imageComponent.gridLineSpan = gridOptions.lineSpan
        imageComponent.gridLineColor = gridOptions.lineColor
        // Create layout
        val view = ImageContainerPane(imageComponent)
//        view.addMouseListener(EditorMouseAdapter())  // right click menu
        view.addMouseListener(FocusRequester())
        val scrollPane = ScrollPaneFactory.createScrollPane(view)
        scrollPane.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
        scrollPane.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        // Zoom by wheel listener
        scrollPane.addMouseWheelListener(wheelAdapter)
        // Construct UI
        layout = BorderLayout()

        val actionManager = ActionManager.getInstance()
        val actionGroup = actionManager.getAction(ImageEditorActions.GROUP_TOOLBAR) as ActionGroup
        val actionToolbar = actionManager.createActionToolbar(
            ImageEditorActions.ACTION_PLACE, actionGroup, true
        )
        actionToolbar.targetComponent = this
        val toolbarPanel = actionToolbar.component
        toolbarPanel.addMouseListener(FocusRequester())

        val topPanel = JPanel(BorderLayout())
        topPanel.add(toolbarPanel, BorderLayout.WEST)
        infoLabel = JLabel(null as String?, SwingConstants.RIGHT)
        infoLabel.border = IdeBorderFactory.createEmptyBorder(0, 0, 0, 2)
        topPanel.add(infoLabel, BorderLayout.EAST)

        add(topPanel, BorderLayout.NORTH)
        add(scrollPane, BorderLayout.CENTER)

        updateInfo()
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

    fun dispose() {
        imageComponent.removeMouseWheelListener(wheelAdapter)
        removeAll()
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

        private fun centerComponents() {
            val bounds = bounds
            val point = imageComponent.location
            point.x = (bounds.width - imageComponent.width) / 2
            point.y = (bounds.height - imageComponent.height) / 2
            imageComponent.location = point
        }

        override fun invalidate() {
            centerComponents()
            super.invalidate()
        }

        override fun getPreferredSize(): Dimension {
            return imageComponent.size
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            if (UIUtil.isUnderDarcula()) {
                g.color = UIUtil.getControlColor().brighter()
                g.fillRect(0, 0, width, height)
            }
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
                revalidate()
                repaint()
                myZoomLevelChanged = false
                imageComponent.firePropertyChange("ImageEditor.zoomFactor", oldZoomFactor, zoomFactor)
            }
        }

        override fun fitZoomToWindow() {
            val image = imageComponent.document.value
            val widthRatio = width.toDouble() / image.width
            val heightRatio = height.toDouble() / image.height
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

    // Don't know if needed
    private inner class FocusRequester : MouseAdapter() {
        override fun mousePressed(e: MouseEvent) {
            requestFocus()
        }
    }

    // Right click context menu
    private class EditorMouseAdapter : PopupHandler() {
        override fun invokePopup(comp: Component, x: Int, y: Int) {
            // Single right click
            val actionManager = ActionManager.getInstance()
            val actionGroup = actionManager.getAction(ImageEditorActions.GROUP_POPUP) as ActionGroup  // TODO: should create same action group as toolbar
            val menu = actionManager.createActionPopupMenu(ImageEditorActions.ACTION_PLACE, actionGroup)
            val popupMenu = menu.component
            popupMenu.pack()
            popupMenu.show(comp, x, y)
        }
    }

    override fun getData(dataId: String): Any? {
        if (ImageComponentDecorator.DATA_KEY.`is`(dataId)) {
            return editor
        }
//        else if (CommonDataKeys.PROJECT.`is`(dataId)) {
//            return editor.project
//        } else if (CommonDataKeys.VIRTUAL_FILE.`is`(dataId)) {
//            return editor.file
//        } else if (CommonDataKeys.VIRTUAL_FILE_ARRAY.`is`(dataId)) {
//            return arrayOf(editor.file)
//        } else if (CommonDataKeys.PSI_FILE.`is`(dataId)) {
//            return getData(CommonDataKeys.PSI_ELEMENT.name)
//        } else if (CommonDataKeys.PSI_ELEMENT.`is`(dataId)) {
//            val file = editor.file
//            return if (file != null && file.isValid) PsiManager.getInstance(editor.project).findFile(file) else null
//        } else if (LangDataKeys.PSI_ELEMENT_ARRAY.`is`(dataId)) {
//            return arrayOf(getData(CommonDataKeys.PSI_ELEMENT.name) as PsiElement?)
//        }
        return null
    }

    override fun performCopy(dataContext: DataContext) {
        val document = imageComponent.document
        val image = document.value
        CopyPasteManager.getInstance().setContents(ImageTransferable(image))
    }

    override fun isCopyEnabled(dataContext: DataContext): Boolean {
        return true
    }

    override fun isCopyVisible(dataContext: DataContext): Boolean {
        return true
    }

    private class ImageTransferable(private val myImage: BufferedImage) : Transferable {
        override fun getTransferDataFlavors(): Array<DataFlavor> {
            return arrayOf(DataFlavor.imageFlavor)
        }

        override fun isDataFlavorSupported(dataFlavor: DataFlavor): Boolean {
            return DataFlavor.imageFlavor.equals(dataFlavor)
        }

        @Throws(UnsupportedFlavorException::class, IOException::class)
        override fun getTransferData(dataFlavor: DataFlavor): Any {
            if (!DataFlavor.imageFlavor.equals(dataFlavor)) {
                throw UnsupportedFlavorException(dataFlavor)
            }
            return myImage
        }
    }

    companion object {
        private val LOG = Logger.getInstance(ImageEditorUI::class.java)
    }
}