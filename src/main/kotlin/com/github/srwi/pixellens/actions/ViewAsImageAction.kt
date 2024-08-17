package com.github.srwi.pixellens.actions

import com.github.srwi.pixellens.UserSettings
import com.github.srwi.pixellens.dialogs.ImageViewer
import com.github.srwi.pixellens.imageProviders.ImageProviderFactory
import com.intellij.notification.*
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.xdebugger.impl.ui.tree.actions.XDebuggerTreeActionBase
import com.jetbrains.python.debugger.PyDebugProcess
import com.jetbrains.python.debugger.PyDebugValue
import javax.swing.SwingUtilities

class ViewAsImageAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val value = XDebuggerTreeActionBase.getSelectedValue(e.dataContext) as PyDebugValue? ?: return

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Loading image...", true) {
            override fun run(progressIndicator: ProgressIndicator) {
                try {
                    val imageProvider = ImageProviderFactory.getImageProvider(value.typeQualifier as String)
                    val batch = imageProvider.getDataByVariableName(value.frameAccessor, progressIndicator, value.name)
                    batch.data.normalized = UserSettings.normalizeEnabled
                    batch.data.channelsFirst = UserSettings.transposeEnabled
                    batch.data.reversedChannels = UserSettings.reverseChannelsEnabled
                    batch.data.grayscaleColormap = UserSettings.applyColormapEnabled

                    SwingUtilities.invokeLater {
                        ImageViewer(project, batch).show()
                    }
                } catch (e: InterruptedException) {
                    // Operation cancelled by user
                } catch (e: OutOfMemoryError) {
                    Notifications.Bus.notify(
                        Notification(
                            "PixelLens",
                            "Out of memory",
                            "The IDE ran out of memory while trying to view the image. Please try again with a smaller slice of the data.",
                            NotificationType.ERROR
                        ),
                        project
                    )
                } catch (e: Throwable) {
                    val formattedException = e.message + "\n" + e.stackTrace.joinToString("\n")
                    Notifications.Bus.notify(
                        Notification(
                            "PixelLens",
                            "Unexpected error",
                            "PixelLens encountered an unexpected error. If possible, please report this issue on GitHub.",
                            NotificationType.ERROR
                        ).addAction(CopyAndReportExceptionAction("Copy exception", formattedException))
                            .addAction(ReportToGithubAction("Report on GitHub", formattedException)),
                        project
                    )
                }
            }
        })
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