package com.github.srwi.pycharmpixelglance.actions

import com.github.srwi.pycharmpixelglance.UserSettings
import com.github.srwi.pycharmpixelglance.dialogs.ErrorMessageDialog
import com.github.srwi.pycharmpixelglance.dialogs.ImageViewer
import com.github.srwi.pycharmpixelglance.imageProviders.ImageProviderFactory
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.xdebugger.impl.ui.tree.actions.XDebuggerTreeActionBase
import com.jetbrains.python.debugger.PyDebugValue

class ViewAsImageAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        try {
            val value = XDebuggerTreeActionBase.getSelectedValue(e.dataContext) as PyDebugValue? ?: return
            val imageProvider = ImageProviderFactory.getImageProvider(value.typeQualifier as String)
            val batch = imageProvider.getDataByVariableName(value.frameAccessor, value.name)
            batch.data.normalized = UserSettings.normalizeEnabled
            batch.data.channelsFirst = UserSettings.transposeEnabled
            batch.data.reversedChannels = UserSettings.reverseChannelsEnabled
            batch.data.grayscaleColormap = UserSettings.applyColormapEnabled

            ImageViewer(project, batch).show()
        } catch (ex: Exception) {
            val formattedException = ex.message + "\n" + ex.stackTrace.joinToString("\n")
            ErrorMessageDialog(
                project,
                "Error",
                "The selected data could not be viewed as image.",
                formattedException
            ).show()
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        try {
            val value = XDebuggerTreeActionBase.getSelectedValue(e.dataContext) as PyDebugValue? ?: return
            val imageProvider = ImageProviderFactory.getImageProvider(value.typeQualifier as String)
            e.presentation.isVisible = imageProvider.typeSupported(value)
            e.presentation.isEnabled = imageProvider.shapeSupported(value)
        } catch (_: Exception) {
            e.presentation.isVisible = false
        }
    }
}