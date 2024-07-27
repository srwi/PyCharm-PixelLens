package com.github.srwi.pycharmpixelglance.actions

import com.github.srwi.pycharmpixelglance.dialogs.ImageViewer
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareToggleAction
import org.intellij.images.editor.actionSystem.ImageEditorActionUtil

internal class ToggleReverseChannelsAction : DumbAwareToggleAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val imageViewer = ImageEditorActionUtil.getImageComponentDecorator(e) as? ImageViewer
        if (imageViewer != null) {
            imageViewer.reverseChannelsEnabled = !imageViewer.reverseChannelsEnabled
        }
    }

    override fun isSelected(e: AnActionEvent): Boolean {
        val imageViewer = ImageEditorActionUtil.getImageComponentDecorator(e) as? ImageViewer
        if (imageViewer != null) {
            return imageViewer.reverseChannelsEnabled
        }
        return false
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        val imageViewer = ImageEditorActionUtil.getImageComponentDecorator(e) as? ImageViewer
        if (imageViewer != null) {
            imageViewer.reverseChannelsEnabled = state
        }
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val imageViewer = ImageEditorActionUtil.getImageComponentDecorator(e) as? ImageViewer
        if (imageViewer != null) {
            e.presentation.isEnabled = imageViewer.data.reverseChannelsApplicable || imageViewer.reverseChannelsEnabled
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}