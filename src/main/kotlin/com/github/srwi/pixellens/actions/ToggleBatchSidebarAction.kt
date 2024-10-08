package com.github.srwi.pixellens.actions

import com.github.srwi.pixellens.dialogs.ImageViewer
import com.github.srwi.pixellens.dialogs.SidebarType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareToggleAction
import org.intellij.images.editor.actionSystem.ImageEditorActionUtil

internal class ToggleBatchSidebarAction : DumbAwareToggleAction() {
    override fun isSelected(e: AnActionEvent): Boolean {
        val imageViewer = ImageEditorActionUtil.getImageComponentDecorator(e) as? ImageViewer
        return imageViewer?.activeSidebar == SidebarType.BatchSidebar
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        val imageViewer = ImageEditorActionUtil.getImageComponentDecorator(e) as? ImageViewer
        if (imageViewer != null) {
            if (state) {
                imageViewer.activeSidebar = SidebarType.BatchSidebar
            } else if (imageViewer.activeSidebar == SidebarType.BatchSidebar) {
                imageViewer.activeSidebar = null
            }
        }
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val imageViewer = ImageEditorActionUtil.getImageComponentDecorator(e) as? ImageViewer
        if (imageViewer != null) {
            val newState = imageViewer.batch.data.batchSize > 1
            e.presentation.isVisible = newState
            if (!newState) {
                setSelected(e, false)
            }
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}