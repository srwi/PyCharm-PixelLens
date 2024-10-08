package com.github.srwi.pixellens.actions

import com.github.srwi.pixellens.dialogs.ImageViewer
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import org.intellij.images.editor.actionSystem.ImageEditorActionUtil
import org.intellij.images.options.OptionsManager

internal class FitZoomToWindowAction : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val imageViewer = ImageEditorActionUtil.getImageComponentDecorator(e) as? ImageViewer
        if (imageViewer != null) {
            val zoomModel = imageViewer.zoomModel
            zoomModel.fitZoomToWindow()
        }
    }

    override fun update(e: AnActionEvent) {
        if (ImageEditorActionUtil.setEnabled(e)) {
            val imageViewer = ImageEditorActionUtil.getImageComponentDecorator(e) as? ImageViewer
            if (imageViewer == null) {
                e.presentation.isEnabled = false
            } else {
                val options = OptionsManager.getInstance().options
                val zoomOptions = options.editorOptions.zoomOptions
                val zoomModel = imageViewer.zoomModel
                e.presentation.isEnabled = zoomModel.isZoomLevelChanged || !zoomOptions.isSmartZooming
            }
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}