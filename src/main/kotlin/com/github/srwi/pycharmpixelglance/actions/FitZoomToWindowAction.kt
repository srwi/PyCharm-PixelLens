package com.github.srwi.pycharmpixelglance.actions

import com.github.srwi.pycharmpixelglance.dialogs.ImageViewer
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.intellij.images.editor.actionSystem.ImageEditorActionUtil
import org.intellij.images.options.OptionsManager
import org.intellij.images.ui.ImageComponentDecorator
import javax.swing.JScrollPane

internal class FitZoomToWindowAction : AnAction() {
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