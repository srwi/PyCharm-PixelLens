package com.github.srwi.pycharmpixelglance.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.intellij.images.editor.actionSystem.ImageEditorActionUtil
import org.intellij.images.options.OptionsManager

internal class FitZoomToWindowAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val decorator = ImageEditorActionUtil.getImageComponentDecorator(e)
        if (decorator != null) {
            val zoomModel = decorator.zoomModel
            zoomModel.fitZoomToWindow()
        }
    }

    override fun update(e: AnActionEvent) {
        if (ImageEditorActionUtil.setEnabled(e)) {
            val options = OptionsManager.getInstance().options
            val zoomOptions = options.editorOptions.zoomOptions
            val decorator = ImageEditorActionUtil.getImageComponentDecorator(e)
            val zoomModel = decorator.zoomModel
            e.presentation.isEnabled = zoomModel.isZoomLevelChanged || !zoomOptions.isSmartZooming
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}