package com.github.srwi.pycharmpixelglance.actions

import com.github.srwi.pycharmpixelglance.dialogs.ImageViewer
import com.github.srwi.pycharmpixelglance.icons.ImageViewerIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.project.DumbAware

class ToggleSidebarAction(private val imageViewer: ImageViewer) : ToggleAction("Toggle Sidebar", "Show or hide the sidebar", ImageViewerIcons.ToggleSidebar), DumbAware {
    override fun isSelected(e: AnActionEvent): Boolean {
        return imageViewer.isSidebarVisible
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        imageViewer.isSidebarVisible = state
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}