package com.github.srwi.pycharmpixelglance.dialogs

import com.github.srwi.pycharmpixelglance.data.DisplayableData
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBViewport
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.ImageUtil
import java.awt.BorderLayout
import java.awt.Graphics2D
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionListener
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.math.max
import kotlin.math.min

class ImageViewDialog(
    project: Project?,
    image: DisplayableData,
    title: String
) : DialogWrapper(project) {

    private val imageLabel: JLabel
    private val image: DisplayableData
    private val displayedImage: BufferedImage
    private val scrollPane: JBScrollPane
    private val footerLabel: JLabel
    private val checkerboardSize = 4
    private var checkerboardEnabled = true
    private var pixelGridEnabled = false

    private var currentZoom = 1.0
        set(value) {
            field = min(30.0, value)
        }

    init {
        this.title = title
        this.image = image
        displayedImage = image.getBuffer()
        imageLabel = JLabel()
        imageLabel.horizontalAlignment = SwingConstants.CENTER
        imageLabel.verticalAlignment = SwingConstants.CENTER
        scrollPane = JBScrollPane(imageLabel)
        footerLabel = JLabel(" ")
        init()
        updateImage()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())

        val toolbar = createToolbar()
        panel.add(toolbar, BorderLayout.NORTH)

//        scrollPane.preferredSize = Dimension(600, 400)

        val viewport = JBViewport()
        viewport.add(scrollPane, BorderLayout.CENTER)

        panel.add(viewport, BorderLayout.CENTER)
        panel.add(footerLabel, BorderLayout.SOUTH)

        imageLabel.addMouseMotionListener(object : MouseMotionListener {
            override fun mouseDragged(e: MouseEvent) {
                updateFooter(e)
            }

            override fun mouseMoved(e: MouseEvent) {
                updateFooter(e)
            }

            private fun updateFooter(e: MouseEvent) {
                e.let {
                    val icon = imageLabel.icon as ImageIcon
                    val iconWidth = icon.iconWidth
                    val iconHeight = icon.iconHeight

                    val aViewport = scrollPane.viewport
                    val viewPosition = aViewport.viewPosition

                    val xOffset = max((aViewport.width - iconWidth) / 2 - viewPosition.x, 0)
                    val yOffset = max((aViewport.height - iconHeight) / 2 - viewPosition.y, 0)

                    val imgX = ((e.x - xOffset) / currentZoom).toInt()
                    val imgY = ((e.y - yOffset) / currentZoom).toInt()

                    if (imgX in 0 until displayedImage.width && imgY in 0 until displayedImage.height) {
                        val pixel = displayedImage.getRGB(imgX, imgY)
                        val r = (pixel shr 16) and 0xFF
                        val g = (pixel shr 8) and 0xFF
                        val b = pixel and 0xFF
                        val a = (pixel shr 24) and 0xFF
                        footerLabel.text = "x=$imgX, y=$imgY, value=($r, $g, $b, $a)"
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
            add(SaveAction())
            add(CopyAction())
            add(Separator())
            add(ZoomInAction())
            add(ZoomOutAction())
            add(FitToWindowAction())
            add(OriginalSizeAction())
            add(Separator())
            add(ToggleCheckerboardAction())
            add(TogglePixelGridAction())
        }

        return ActionManager.getInstance()
            .createActionToolbar("ImageViewerToolbar", actionGroup, true)
            .component
    }

    private inner class CopyAction : DumbAwareAction("Copy", "Copy image to clipboard", AllIcons.Actions.Copy) {
        override fun actionPerformed(e: AnActionEvent) {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(TransferableImage(displayedImage), null)
        }
    }

    private inner class SaveAction : DumbAwareAction("Save...", "Save image to file", AllIcons.Actions.MenuSaveall) {
        override fun actionPerformed(e: AnActionEvent) {
            val fileChooser = JFileChooser()
            fileChooser.dialogTitle = "Save Image"
            fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
            fileChooser.fileFilter = FileNameExtensionFilter("PNG (*.png)", "*.png")

            val userSelection = fileChooser.showSaveDialog(null)
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                val fileToSave = fileChooser.selectedFile
                ImageIO.write(displayedImage, "png", fileToSave)
            }
        }
    }

    private inner class ZoomInAction : DumbAwareAction("Zoom In", "Zoom in", AllIcons.General.ZoomIn) {
        override fun actionPerformed(e: AnActionEvent) {
            zoom(1.5)
        }
    }

    private inner class ZoomOutAction : DumbAwareAction("Zoom Out", "Zoom out", AllIcons.General.ZoomOut) {
        override fun actionPerformed(e: AnActionEvent) {
            zoom(1 / 1.5)
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

    private inner class ToggleCheckerboardAction : DumbAwareAction("Toggle Checkerboard", "Toggle checkerboard background", AllIcons.Gutter.Colors) {
        override fun actionPerformed(e: AnActionEvent) {
            checkerboardEnabled = !checkerboardEnabled
            updateImage()
        }
    }

    private inner class TogglePixelGridAction : DumbAwareAction("Toggle Pixel Grid", "Toggle pixel grid overlay", AllIcons.Graph.Grid) {
        override fun actionPerformed(e: AnActionEvent) {
            pixelGridEnabled = !pixelGridEnabled
            updateImage()
        }
    }

    private fun zoom(factor: Double) {
        currentZoom *= factor
        updateImage()
    }

    private fun fitToWindow() {
        val viewportSize = scrollPane.viewport.size
        val widthRatio = viewportSize.width.toDouble() / displayedImage.width
        val heightRatio = viewportSize.height.toDouble() / displayedImage.height
        currentZoom = min(widthRatio, heightRatio)
        updateImage()
    }

    private fun resetZoom() {
        currentZoom = 1.0
        updateImage()
    }

    private fun updateImage() {
        val dpiCorrectedZoom = currentZoom / JBUIScale.sysScale()

        val newWidth = (displayedImage.width * dpiCorrectedZoom).toInt()
        val newHeight = (displayedImage.height * dpiCorrectedZoom).toInt()

        val finalImage = ImageUtil.createImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB)
        val g2d = finalImage.createGraphics()

        if (checkerboardEnabled && displayedImage.colorModel.hasAlpha()) {
            drawCheckerboard(g2d, newWidth, newHeight)
        }

        g2d.drawImage(displayedImage, 0, 0, newWidth, newHeight, null)

        if (pixelGridEnabled) {
            drawPixelGrid(g2d, newWidth, newHeight)
        }

        g2d.dispose()

        imageLabel.icon = ImageIcon(finalImage)
        imageLabel.revalidate()
        imageLabel.repaint()
    }

    private fun drawCheckerboard(g2d: Graphics2D, width: Int, height: Int) {
        g2d.color = JBColor.LIGHT_GRAY
        g2d.fillRect(0, 0, width, height)

        g2d.color = JBColor.WHITE
        for (x in 0 until width step checkerboardSize * 2) {
            for (y in 0 until height step checkerboardSize * 2) {
                g2d.fillRect(x, y, checkerboardSize, checkerboardSize)
                g2d.fillRect(x + checkerboardSize, y + checkerboardSize, checkerboardSize, checkerboardSize)
            }
        }
    }

    private fun drawPixelGrid(g2d: Graphics2D, width: Int, height: Int) {
        val widthStepSize = width.toDouble() / displayedImage.width
        val heightStepSize = height.toDouble() / displayedImage.height

        if (widthStepSize <= 3 || heightStepSize <= 3) {
            // We don't want to draw lines if they would entirely cover the content
            return
        }

        g2d.color = JBColor.BLACK
        for (x in 1 until displayedImage.width) {
            val xPosition = (x * widthStepSize).toInt()
            g2d.drawLine(xPosition, 0, xPosition, height)
        }
        for (y in 1 until displayedImage.height) {
            val yPosition = (y * heightStepSize).toInt()
            g2d.drawLine(0, yPosition, width, yPosition)
        }
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