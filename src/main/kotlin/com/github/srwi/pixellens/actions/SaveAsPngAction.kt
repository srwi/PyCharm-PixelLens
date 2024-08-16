package com.github.srwi.pixellens.actions

import com.github.srwi.pixellens.dialogs.ImageViewer
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import org.intellij.images.editor.actionSystem.ImageEditorActionUtil
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

class SaveAsPngAction : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val imageViewer = ImageEditorActionUtil.getImageComponentDecorator(e) as? ImageViewer ?: return
        val image = imageViewer.getDisplayedImage()
        val fileChooser = JFileChooser()
        fileChooser.dialogTitle = "Save Image"
        fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
        fileChooser.fileFilter = FileNameExtensionFilter("PNG (*.png)", "png")

        if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            var fileToSave = fileChooser.selectedFile
            if (!fileToSave.name.lowercase().endsWith(".png")) {
                fileToSave = File(fileToSave.parentFile, "${fileToSave.nameWithoutExtension}.png")
            }
            ImageIO.write(image, "png", fileToSave)
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}