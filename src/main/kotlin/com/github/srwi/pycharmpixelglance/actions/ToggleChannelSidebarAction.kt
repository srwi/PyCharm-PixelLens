package com.github.srwi.pycharmpixelglance.actions

import com.github.srwi.pycharmpixelglance.dialogs.ImageViewer
import com.github.srwi.pycharmpixelglance.dialogs.SidebarType
import com.github.srwi.pycharmpixelglance.icons.ImageViewerIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareToggleAction
import org.intellij.images.editor.actionSystem.ImageEditorActionUtil

internal class ToggleChannelSidebarAction : DumbAwareToggleAction("Toggle Channel Sidebar", "", ImageViewerIcons.Channels) {
    override fun isSelected(e: AnActionEvent): Boolean {
        val imageViewer = ImageEditorActionUtil.getImageComponentDecorator(e) as? ImageViewer
        return imageViewer?.activeSidebar == SidebarType.ChannelSidebar
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        val imageViewer = ImageEditorActionUtil.getImageComponentDecorator(e) as? ImageViewer
        if (imageViewer != null) {
            imageViewer.activeSidebar = if (state) SidebarType.ChannelSidebar else null
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}