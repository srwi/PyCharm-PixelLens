package com.github.srwi.pycharmpixelglance.dialogs

import com.github.srwi.pycharmpixelglance.data.DisplayableData
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.ImageUtil
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Graphics2D
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
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
    private var autoResizeEnabled = false

    private var currentZoom = 1.0
        set(value) {
            field = min(MAX_ZOOM, max(MIN_ZOOM, value))
        }

    private lateinit var zoomInAction: ZoomInAction
    private lateinit var zoomOutAction: ZoomOutAction
    private lateinit var fitToWindowAction: FitToWindowAction
    private lateinit var originalSizeAction: OriginalSizeAction

    companion object {
        private const val MAX_ZOOM = 30.0
        private const val MIN_ZOOM = 0.1
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

        panel.add(scrollPane, BorderLayout.CENTER)
        panel.add(footerLabel, BorderLayout.SOUTH)

        addMouseMotionListener()
        addResizeListener()

        return panel
    }

    private fun createToolbar(): JComponent {
        zoomInAction = ZoomInAction()
        zoomOutAction = ZoomOutAction()
        fitToWindowAction = FitToWindowAction()
        originalSizeAction = OriginalSizeAction()

        val actionGroup = DefaultActionGroup().apply {
            add(SaveAction())
            add(CopyAction())
            add(Separator())
            add(zoomInAction)
            add(zoomOutAction)
            add(fitToWindowAction)
            add(originalSizeAction)
            add(Separator())
            add(ToggleCheckerboardAction())
            add(TogglePixelGridAction())
        }

        return ActionManager.getInstance()
            .createActionToolbar("ImageViewerToolbar", actionGroup, true)
            .component
    }

    private fun addMouseMotionListener() {
        imageLabel.addMouseMotionListener(object : MouseMotionListener {
            override fun mouseDragged(e: MouseEvent) {
                updateFooter(e)
            }

            override fun mouseMoved(e: MouseEvent) {
                updateFooter(e)
            }

            private fun updateFooter(e: MouseEvent) {
                val icon = imageLabel.icon as ImageIcon
                val iconWidth = icon.iconWidth
                val iconHeight = icon.iconHeight

                val viewPosition = scrollPane.viewport.viewPosition

                val xOffset = max((scrollPane.viewport.width - iconWidth) / 2, 0)
                val yOffset = max((scrollPane.viewport.height - iconHeight) / 2, 0)

                val imgX = ((e.x + viewPosition.x - xOffset) / currentZoom).toInt()
                val imgY = ((e.y + viewPosition.y - yOffset) / currentZoom).toInt()

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
        })
    }

    private fun addResizeListener() {
        window.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                if (autoResizeEnabled) {
                    fitToWindow()
                }
            }
        })
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
            disableAutoResize()
            zoom(1.5)
        }

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = currentZoom < MAX_ZOOM
        }
    }

    private inner class ZoomOutAction : DumbAwareAction("Zoom Out", "Zoom out", AllIcons.General.ZoomOut) {
        override fun actionPerformed(e: AnActionEvent) {
            disableAutoResize()
            zoom(1 / 1.5)
        }

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = currentZoom > MIN_ZOOM
        }
    }

    private inner class FitToWindowAction : DumbAwareAction("Fit to Window", "Fit image to window size", AllIcons.General.FitContent) {
        override fun actionPerformed(e: AnActionEvent) {
            autoResizeEnabled = true
            fitToWindow()
        }

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = !autoResizeEnabled
        }
    }

    private inner class OriginalSizeAction : DumbAwareAction("Original Size", "Reset to original size", AllIcons.General.ActualZoom) {
        override fun actionPerformed(e: AnActionEvent) {
            disableAutoResize()
            resetZoom()
        }

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = currentZoom != 1.0
        }
    }

    private inner class ToggleCheckerboardAction : ToggleAction("Toggle Checkerboard", "Toggle checkerboard background", AllIcons.Gutter.Colors) {
        override fun isSelected(e: AnActionEvent): Boolean = checkerboardEnabled

        override fun setSelected(e: AnActionEvent, state: Boolean) {
            checkerboardEnabled = state
            updateImage()
        }
    }

    private inner class TogglePixelGridAction : ToggleAction("Toggle Pixel Grid", "Toggle pixel grid overlay", AllIcons.Graph.Grid) {
        override fun isSelected(e: AnActionEvent): Boolean = pixelGridEnabled

        override fun setSelected(e: AnActionEvent, state: Boolean) {
            pixelGridEnabled = state
            updateImage()
        }
    }

    private fun disableAutoResize() {
        autoResizeEnabled = false
        updateActionStates()
    }

    private fun zoom(factor: Double) {
        currentZoom *= factor
        updateImage()
        updateActionStates()
    }

    private fun fitToWindow() {
        val viewportSize = scrollPane.viewport.size
        val widthRatio = viewportSize.width.toDouble() / displayedImage.width
        val heightRatio = viewportSize.height.toDouble() / displayedImage.height
        currentZoom = min(widthRatio, heightRatio)
        updateImage()
        updateActionStates()
    }

    private fun resetZoom() {
        currentZoom = 1.0
        updateImage()
        updateActionStates()
    }

    private fun updateActionStates() {
        zoomInAction.templatePresentation.isEnabled = currentZoom < MAX_ZOOM
        zoomOutAction.templatePresentation.isEnabled = currentZoom > MIN_ZOOM
        originalSizeAction.templatePresentation.isEnabled = currentZoom != 1.0
        fitToWindowAction.templatePresentation.isEnabled = !autoResizeEnabled
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
        imageLabel.preferredSize = Dimension(newWidth, newHeight)
        scrollPane.viewport.revalidate()
        scrollPane.viewport.repaint()
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