package com.github.srwi.pixellens.actions

import com.github.srwi.pixellens.UserSettings
import com.github.srwi.pixellens.dialogs.ImageViewer
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareToggleAction
import org.intellij.images.editor.actionSystem.ImageEditorActionUtil

internal class ToggleReverseChannelsAction : DumbAwareToggleAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val imageViewer = ImageEditorActionUtil.getImageComponentDecorator(e) as? ImageViewer
        if (imageViewer != null) {
            val newValue = !imageViewer.reverseChannelsEnabled
            UserSettings.reverseChannelsEnabled = newValue
            imageViewer.reverseChannelsEnabled = newValue
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
        if (imageViewer != null && imageViewer.batchData != null) {
            val numChannels = imageViewer.batchData!!.channels
            val newState = numChannels == 3 || numChannels == 4
            e.presentation.isEnabled = newState
            if (!newState) {
                setSelected(e, false)
            }
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}