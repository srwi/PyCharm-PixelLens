package com.github.srwi.pycharmpixelglance.actions

import com.github.srwi.pycharmpixelglance.dialogs.ImageViewer
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareToggleAction
import org.intellij.images.editor.actionSystem.ImageEditorActionUtil

internal class ToggleTransposeAction : DumbAwareToggleAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val imageViewer = ImageEditorActionUtil.getImageComponentDecorator(e) as? ImageViewer
        if (imageViewer != null) {
            imageViewer.processedData.transposeEnabled = !imageViewer.processedData.transposeEnabled
            imageViewer.updateImage()
        }
    }

    override fun isSelected(e: AnActionEvent): Boolean {
        val imageViewer = ImageEditorActionUtil.getImageComponentDecorator(e) as? ImageViewer
        if (imageViewer != null) {
            return imageViewer.processedData.transposeEnabled
        }
        return false
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        val imageViewer = ImageEditorActionUtil.getImageComponentDecorator(e) as? ImageViewer
        if (imageViewer != null) {
            imageViewer.processedData.transposeEnabled = state
            imageViewer.updateImage()
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}