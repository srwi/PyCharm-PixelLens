package com.github.srwi.pycharmpixelglance.dialogs

import com.intellij.openapi.project.Project
import java.awt.BorderLayout
import java.awt.image.BufferedImage
import javax.swing.JDialog
import javax.swing.WindowConstants

class ImageViewerDialog(project: Project, image: BufferedImage) : JDialog() {
    private val imageEditor: ImageEditorImpl = ImageEditorImpl(project, image)

    init {
        title = "Image Viewer"
        layout = BorderLayout()
        add(imageEditor.component, BorderLayout.CENTER)
        setSize(800, 600)
        setLocationRelativeTo(null)
        defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
    }

    fun showDialog() {
        isVisible = true
    }

    override fun dispose() {
        isVisible = false
        imageEditor.dispose()
        super.dispose()
    }
}

//package com.github.srwi.pycharmpixelglance.dialogs
//
//import com.github.srwi.pycharmpixelglance.data.DisplayableData
//import com.intellij.icons.AllIcons
//import com.intellij.openapi.actionSystem.*
//import com.intellij.openapi.project.DumbAwareAction
//import com.intellij.openapi.project.Project
//import com.intellij.openapi.ui.DialogWrapper
//import com.intellij.ui.JBColor
//import com.intellij.ui.components.JBScrollPane
//import com.intellij.ui.scale.JBUIScale
//import com.intellij.util.ui.ImageUtil
//import java.awt.BorderLayout
//import java.awt.Dimension
//import java.awt.Graphics2D
//import java.awt.Toolkit
//import java.awt.datatransfer.DataFlavor
//import java.awt.datatransfer.Transferable
//import java.awt.event.ComponentAdapter
//import java.awt.event.ComponentEvent
//import java.awt.event.MouseEvent
//import java.awt.event.MouseMotionListener
//import java.awt.image.BufferedImage
//import javax.imageio.ImageIO
//import javax.swing.*
//import javax.swing.filechooser.FileNameExtensionFilter
//import kotlin.math.max
//import kotlin.math.min
//
//class ImageViewDialog(
//    project: Project?,
//    image: DisplayableData,
//    title: String
//) : DialogWrapper(project) {
//
//    private val imageLabel: JLabel
//    private val image: DisplayableData
//    private val displayedImage: BufferedImage
//    private val scrollPane: JBScrollPane
//    private val footerLabel: JLabel
//    private val checkerboardSize = 4
//    private var checkerboardEnabled = true
//    private var pixelGridEnabled = false
//    private var autoResizeEnabled = false
//
//    private var currentZoom = 1.0
//        set(value) {
//            field = min(MAX_ZOOM, max(MIN_ZOOM, value))
//        }
//
//    private lateinit var zoomInAction: ZoomInAction
//    private lateinit var zoomOutAction: ZoomOutAction
//    private lateinit var fitToWindowAction: FitToWindowAction
//    private lateinit var originalSizeAction: OriginalSizeAction
//
//    companion object {
//        private const val MAX_ZOOM = 30.0
//        private const val MIN_ZOOM = 0.1
//    }
//
//    init {
//        this.title = title
//        this.image = image
//        displayedImage = image.getBuffer()
//
//
//        imageLabel = JLabel()
//        imageLabel.horizontalAlignment = SwingConstants.CENTER
//        imageLabel.verticalAlignment = SwingConstants.CENTER
//        scrollPane = JBScrollPane(imageLabel)
//        footerLabel = JLabel(" ")
//
//
//
//        init()
////        updateImage()
//    }
//
//    override fun createCenterPanel(): JComponent {
//        val panel = JPanel(BorderLayout())
//
//        val toolbar = createToolbar()
//        panel.add(toolbar, BorderLayout.NORTH)
//
//        panel.add(scrollPane, BorderLayout.CENTER)
//        panel.add(footerLabel, BorderLayout.SOUTH)
//
//        addMouseMotionListener()
//        addResizeListener()
//
//        return panel
//    }
//
//    private fun createToolbar(): JComponent {
//        zoomInAction = ZoomInAction()
//        zoomOutAction = ZoomOutAction()
//        fitToWindowAction = FitToWindowAction()
//        originalSizeAction = OriginalSizeAction()
//
//        val actionGroup = DefaultActionGroup().apply {
//            add(SaveAction())
//            add(CopyAction())
//            add(Separator())
//            add(zoomInAction)
//            add(zoomOutAction)
//            add(fitToWindowAction)
//            add(originalSizeAction)
//            add(Separator())
//            add(ToggleCheckerboardAction())
//            add(TogglePixelGridAction())
//        }
//
//        return ActionManager.getInstance()
//            .createActionToolbar("ImageViewerToolbar", actionGroup, true)
//            .component
//    }
//
//    private fun addMouseMotionListener() {
//        imageLabel.addMouseMotionListener(object : MouseMotionListener {
//            override fun mouseDragged(e: MouseEvent) {
//                updateFooter(e)
//            }
//
//            override fun mouseMoved(e: MouseEvent) {
//                updateFooter(e)
//            }
//
//            private fun updateFooter(e: MouseEvent) {
//                val icon = imageLabel.icon as ImageIcon
//                val iconWidth = icon.iconWidth
//                val iconHeight = icon.iconHeight
//
//                val viewPosition = scrollPane.viewport.viewPosition
//
//                val xOffset = max((scrollPane.viewport.width - iconWidth) / 2, 0)
//                val yOffset = max((scrollPane.viewport.height - iconHeight) / 2, 0)
//
//                val imgX = ((e.x + viewPosition.x - xOffset) / currentZoom).toInt()
//                val imgY = ((e.y + viewPosition.y - yOffset) / currentZoom).toInt()
//
//                if (imgX in 0 until displayedImage.width && imgY in 0 until displayedImage.height) {
//                    val pixel = displayedImage.getRGB(imgX, imgY)
//                    val r = (pixel shr 16) and 0xFF
//                    val g = (pixel shr 8) and 0xFF
//                    val b = pixel and 0xFF
//                    val a = (pixel shr 24) and 0xFF
//                    footerLabel.text = "x=$imgX, y=$imgY, value=($r, $g, $b, $a)"
//                } else {
//                    footerLabel.text = " "
//                }
//            }
//        })
//    }
//
//    private fun addResizeListener() {
//        window.addComponentListener(object : ComponentAdapter() {
//            override fun componentResized(e: ComponentEvent) {
//                if (autoResizeEnabled) {
////                    fitToWindow()
//                }
//            }
//        })
//    }
//
//    private inner class CopyAction : DumbAwareAction("Copy", "Copy image to clipboard", AllIcons.Actions.Copy) {
//        override fun actionPerformed(e: AnActionEvent) {
//            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
//            clipboard.setContents(TransferableImage(displayedImage), null)
//        }
//    }
//
//    private inner class SaveAction : DumbAwareAction("Save...", "Save image to file", AllIcons.Actions.MenuSaveall) {
//        override fun actionPerformed(e: AnActionEvent) {
//            val fileChooser = JFileChooser()
//            fileChooser.dialogTitle = "Save Image"
//            fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
//            fileChooser.fileFilter = FileNameExtensionFilter("PNG (*.png)", "*.png")
//
//            val userSelection = fileChooser.showSaveDialog(null)
//            if (userSelection == JFileChooser.APPROVE_OPTION) {
//                val fileToSave = fileChooser.selectedFile
//                ImageIO.write(displayedImage, "png", fileToSave)
//            }
//        }
//    }
//
//    private inner class ZoomInAction : DumbAwareAction("Zoom In", "Zoom in", AllIcons.General.ZoomIn) {
//        override fun actionPerformed(e: AnActionEvent) {
////            disableAutoResize()
////            zoom(1.5)
//        }
//
//        override fun update(e: AnActionEvent) {
//            e.presentation.isEnabled = currentZoom < MAX_ZOOM
//        }
//    }
//
//    private inner class ZoomOutAction : DumbAwareAction("Zoom Out", "Zoom out", AllIcons.General.ZoomOut) {
//        override fun actionPerformed(e: AnActionEvent) {
////            disableAutoResize()
////            zoom(1 / 1.5)
//        }
//
//        override fun update(e: AnActionEvent) {
//            e.presentation.isEnabled = currentZoom > MIN_ZOOM
//        }
//    }
//
//    private inner class FitToWindowAction : DumbAwareAction("Fit to Window", "Fit image to window size", AllIcons.General.FitContent) {
//        override fun actionPerformed(e: AnActionEvent) {
//            autoResizeEnabled = true
////            fitToWindow()
//        }
//
//        override fun update(e: AnActionEvent) {
//            e.presentation.isEnabled = !autoResizeEnabled
//        }
//    }
//
//    private inner class OriginalSizeAction : DumbAwareAction("Original Size", "Reset to original size", AllIcons.General.ActualZoom) {
//        override fun actionPerformed(e: AnActionEvent) {
////            disableAutoResize()
////            resetZoom()
//        }
//
//        override fun update(e: AnActionEvent) {
//            e.presentation.isEnabled = currentZoom != 1.0
//        }
//    }
//
//    private inner class ToggleCheckerboardAction : ToggleAction("Toggle Checkerboard", "Toggle checkerboard background", AllIcons.Gutter.Colors) {
//        override fun isSelected(e: AnActionEvent): Boolean = checkerboardEnabled
//
//        override fun setSelected(e: AnActionEvent, state: Boolean) {
//            checkerboardEnabled = state
////            updateImage()
//        }
//    }
//
//    private inner class TogglePixelGridAction : ToggleAction("Toggle Pixel Grid", "Toggle pixel grid overlay", AllIcons.Graph.Grid) {
//        override fun isSelected(e: AnActionEvent): Boolean = pixelGridEnabled
//
//        override fun setSelected(e: AnActionEvent, state: Boolean) {
//            pixelGridEnabled = state
////            updateImage()
//        }
//    }
//
//    private fun updateActionStates() {
//        zoomInAction.templatePresentation.isEnabled = currentZoom < MAX_ZOOM
//        zoomOutAction.templatePresentation.isEnabled = currentZoom > MIN_ZOOM
//        originalSizeAction.templatePresentation.isEnabled = currentZoom != 1.0
//        fitToWindowAction.templatePresentation.isEnabled = !autoResizeEnabled
//    }
//
//    private class TransferableImage(private val image: BufferedImage) : Transferable {
//        override fun getTransferData(flavor: DataFlavor?): Any {
//            return image
//        }
//
//        override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean {
//            return flavor == DataFlavor.imageFlavor
//        }
//
//        override fun getTransferDataFlavors(): Array<DataFlavor> {
//            return arrayOf(DataFlavor.imageFlavor)
//        }
//    }
//}