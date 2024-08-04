package com.github.srwi.pixellens.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

class SaveAsPngAction(private val getImage: () -> BufferedImage?) : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val image = getImage() ?: return
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