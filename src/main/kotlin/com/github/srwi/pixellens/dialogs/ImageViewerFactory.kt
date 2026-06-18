package com.github.srwi.pixellens.dialogs

import com.github.srwi.pixellens.data.Batch
import com.github.srwi.pixellens.settings.PixelLensSettingsState
import com.intellij.openapi.project.Project
import com.jetbrains.python.debugger.PyDebugValue

enum class ViewerType {
    Dialog,
    Popup
}

object ImageViewerFactory {
    fun show(project: Project, value: PyDebugValue, batch: Batch) {
        return if (PixelLensSettingsState.instance.usePopupWindow) {
            ImageViewerPopup(project, value, batch).show()
        } else {
            ImageViewerDialog(project, value, batch).show()
        }
    }
}