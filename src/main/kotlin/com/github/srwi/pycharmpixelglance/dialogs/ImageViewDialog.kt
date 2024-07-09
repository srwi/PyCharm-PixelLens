package com.github.srwi.pycharmpixelglance.dialogs

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBViewport
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Image
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionListener
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.util.*
import javax.imageio.ImageIO
import javax.swing.*
import kotlin.math.max
import kotlin.math.min

class ImageViewDialog(
    project: Project?,
    base64ImageString: String,
    title: String = "Image Viewer"
) : DialogWrapper(project) {

    private val imageLabel: JLabel
    private val originalImage: BufferedImage
    private var currentZoom: Double = 1.0
    private val scrollPane: JBScrollPane
    private val footerLabel: JLabel

    init {
        this.title = title
        originalImage = base64ToImage(base64ImageString)
        imageLabel = JLabel(ImageIcon(originalImage))
        scrollPane = JBScrollPane(imageLabel)
        footerLabel = JLabel(" ")
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())

        val toolbar = createToolbar()
        panel.add(toolbar, BorderLayout.NORTH)

        scrollPane.preferredSize = Dimension(600, 400)

        val viewport = JBViewport()
        viewport.add(scrollPane, BorderLayout.CENTER)

        panel.add(viewport, BorderLayout.CENTER)
        panel.add(footerLabel, BorderLayout.SOUTH)

        imageLabel.addMouseMotionListener(object : MouseMotionListener {
            override fun mouseDragged(e: MouseEvent?) {
                updateFooter(e)
            }

            override fun mouseMoved(e: MouseEvent) {
                updateFooter(e)
            }

            private fun updateFooter(e: MouseEvent?) {
                e?.let {
                    val mouseX = e.x
                    val mouseY = e.y

                    val icon = imageLabel.icon as ImageIcon
                    val iconWidth = icon.iconWidth
                    val iconHeight = icon.iconHeight

                    val viewport = scrollPane.viewport
                    val viewPosition = viewport.viewPosition

                    val xOffset = max((viewport.width - iconWidth) / 2 - viewPosition.x, 0)
                    val yOffset = max((viewport.height - iconHeight) / 2 - viewPosition.y, 0)

                    val imgX = ((mouseX - xOffset) / currentZoom).toInt()
                    val imgY = ((mouseY - yOffset) / currentZoom).toInt()

                    if (imgX in 0 until originalImage.width && imgY in 0 until originalImage.height) {
                        val pixel = originalImage.getRGB(imgX, imgY)
                        val r = (pixel shr 16) and 0xFF
                        val g = (pixel shr 8) and 0xFF
                        val b = pixel and 0xFF
                        footerLabel.text = "x=$imgX, y=$imgY, value=($r, $g, $b)"
                    } else {
                        footerLabel.text = " "
                    }
                }
            }
        })

        return panel
    }

    private fun createToolbar(): JComponent {
        val actionGroup = DefaultActionGroup().apply {
            add(CopyAction())
            add(SaveAction())
            add(ZoomInAction())
            add(ZoomOutAction())
            add(FitToWindowAction())
            add(OriginalSizeAction())
        }

        return ActionManager.getInstance()
            .createActionToolbar("ImageViewerToolbar", actionGroup, true)
            .component
    }

    private inner class CopyAction : DumbAwareAction("Copy", "Copy image to clipboard", AllIcons.Actions.Copy) {
        override fun actionPerformed(e: AnActionEvent) {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(TransferableImage(originalImage), null)
        }
    }

    private inner class SaveAction : DumbAwareAction("Save...", "Save image to file", AllIcons.Actions.MenuSaveall) {
        override fun actionPerformed(e: AnActionEvent) {
            val fileChooser = JFileChooser()
            fileChooser.dialogTitle = "Save Image"
            fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY

            val userSelection = fileChooser.showSaveDialog(null)
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                val fileToSave = fileChooser.selectedFile
                ImageIO.write(originalImage, "png", fileToSave)
            }
        }
    }

    private inner class ZoomInAction : DumbAwareAction("Zoom In", "Zoom in", AllIcons.General.ZoomIn) {
        override fun actionPerformed(e: AnActionEvent) {
            zoom(1.25)
        }
    }

    private inner class ZoomOutAction : DumbAwareAction("Zoom Out", "Zoom out", AllIcons.General.ZoomOut) {
        override fun actionPerformed(e: AnActionEvent) {
            zoom(0.8)
        }
    }

    private inner class FitToWindowAction : DumbAwareAction("Fit to Window", "Fit image to window size", AllIcons.General.FitContent) {
        override fun actionPerformed(e: AnActionEvent) {
            fitToWindow()
        }
    }

    private inner class OriginalSizeAction : DumbAwareAction("Original Size", "Reset to original size", AllIcons.General.ActualZoom) {
        override fun actionPerformed(e: AnActionEvent) {
            resetZoom()
        }
    }

    private fun base64ToImage(base64String: String): BufferedImage {
        val imageBytes = Base64.getDecoder().decode(base64String)
        return ByteArrayInputStream(imageBytes).use {
            ImageIO.read(it)
        }
    }

    private fun zoom(factor: Double) {
        currentZoom *= factor
        updateImage()
    }

    private fun fitToWindow() {
        val viewportSize = scrollPane.viewport.size
        val widthRatio = viewportSize.width.toDouble() / originalImage.width
        val heightRatio = viewportSize.height.toDouble() / originalImage.height
        currentZoom = min(widthRatio, heightRatio)
        updateImage()
    }

    private fun resetZoom() {
        currentZoom = 1.0
        updateImage()
    }

    private fun updateImage() {
        val newWidth = (originalImage.width * currentZoom).toInt()
        val newHeight = (originalImage.height * currentZoom).toInt()
        val resizedImage = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_DEFAULT)
        imageLabel.icon = ImageIcon(resizedImage)
        imageLabel.revalidate()
        imageLabel.repaint()
    }

    private class TransferableImage(private val image: BufferedImage) : Transferable {
        override fun getTransferData(flavor: DataFlavor?): Any {
            return image
        }

        override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean {
            return flavor == DataFlavor.imageFlavor
        }

        override fun getTransferDataFlavors(): Array<DataFlavor> {
            return arrayOf(DataFlavor.imageFlavor)
        }
    }
}
