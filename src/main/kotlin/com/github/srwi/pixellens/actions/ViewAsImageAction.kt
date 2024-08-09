package com.github.srwi.pixellens.actions

import com.github.srwi.pixellens.UserSettings
import com.github.srwi.pixellens.dialogs.ErrorMessageDialog
import com.github.srwi.pixellens.dialogs.ImageViewer
import com.github.srwi.pixellens.imageProviders.ImageProviderFactory
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.xdebugger.impl.ui.tree.actions.XDebuggerTreeActionBase
import com.jetbrains.python.debugger.PyDebugValue
import javax.swing.SwingUtilities

class ViewAsImageAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val value = XDebuggerTreeActionBase.getSelectedValue(e.dataContext) as PyDebugValue? ?: return
        val imageProvider = ImageProviderFactory.getImageProvider(value.typeQualifier as String)
        val futureBatch = imageProvider.getDataByVariableName(project, value.frameAccessor, value.name)

        futureBatch.thenAccept { batch ->
            batch.data.normalized = UserSettings.normalizeEnabled
            batch.data.channelsFirst = UserSettings.transposeEnabled
            batch.data.reversedChannels = UserSettings.reverseChannelsEnabled
            batch.data.grayscaleColormap = UserSettings.applyColormapEnabled

            SwingUtilities.invokeLater {
                ImageViewer(project, batch).show()
            }
        }.exceptionally { throwable ->
            val formattedException = throwable.message + "\n" + throwable.stackTrace.joinToString("\n")
            SwingUtilities.invokeLater {
                ErrorMessageDialog(
                    project,
                    "Error",
                    "The selected data could not be viewed as image.",
                    formattedException
                ).show()
            }
            null
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        try {
            val value = XDebuggerTreeActionBase.getSelectedValue(e.dataContext) as PyDebugValue? ?: return
            val processSupported = value.frameAccessor is PyDebugProcess  // Currently only regular debugging is supported
            val imageProvider = ImageProviderFactory.getImageProvider(value.typeQualifier as String)
            e.presentation.isVisible = processSupported && imageProvider.typeSupported(value)
            e.presentation.isEnabled = processSupported && imageProvider.shapeSupported(value)
        } catch (_: Exception) {
            e.presentation.isVisible = false
        }
    }
}