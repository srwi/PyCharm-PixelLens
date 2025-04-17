package com.github.srwi.pixellens.dialogs

import com.github.srwi.pixellens.data.Batch
import com.github.srwi.pixellens.settings.PixelLensSettingsState
import com.intellij.openapi.project.Project

enum class ViewerType {
    Dialog,
    Popup
}

object ImageViewerFactory {
    fun show(project: Project, batch: Batch) {
        return if (PixelLensSettingsState.instance.usePopupWindow) {
            ImageViewerPopup(project, batch).show()
        } else {
            ImageViewerDialog(project, batch).show()
        }
    }
}