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
import java.io.ByteArrayInputStream
import java.util.*
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class ImageViewDialog(
    project: Project?,
    base64ImageString: String,
    title: String = "Image Viewer"
) : DialogWrapper(project) {

    private val imageLabel: JLabel

    init {
        this.title = title
        imageLabel = JLabel(ImageIcon(base64ToImage(base64ImageString)))
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())

        val toolbar = createToolbar()
        panel.add(toolbar, BorderLayout.NORTH)

        val scrollPane = JBScrollPane(imageLabel)
        scrollPane.preferredSize = Dimension(600, 400)

        val viewport = JBViewport()
        viewport.add(scrollPane, BorderLayout.CENTER)

        panel.add(viewport, BorderLayout.CENTER)

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
        }
    }

    private inner class SaveAction : DumbAwareAction("Save...", "Save image to file", AllIcons.Actions.MenuSaveall) {
        override fun actionPerformed(e: AnActionEvent) {
        }
    }

    private inner class ZoomInAction : DumbAwareAction("Zoom In", "Zoom in", AllIcons.General.ZoomIn) {
        override fun actionPerformed(e: AnActionEvent) {
        }
    }

    private inner class ZoomOutAction : DumbAwareAction("Zoom Out", "Zoom out", AllIcons.General.ZoomOut) {
        override fun actionPerformed(e: AnActionEvent) {
        }
    }

    private inner class FitToWindowAction : DumbAwareAction("Fit to Window", "Fit image to window size", AllIcons.General.FitContent) {
        override fun actionPerformed(e: AnActionEvent) {
        }
    }

    private inner class OriginalSizeAction : DumbAwareAction("Original Size", "Reset to original size", AllIcons.General.ActualZoom) {
        override fun actionPerformed(e: AnActionEvent) {
        }
    }

    private fun base64ToImage(base64String: String): Image {
        val imageBytes = Base64.getDecoder().decode(base64String)
        return ByteArrayInputStream(imageBytes).use {
            ImageIO.read(it)
        }
    }
}