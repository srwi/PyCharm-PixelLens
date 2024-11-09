package com.github.srwi.pixellens.actions

import com.github.srwi.pixellens.UserSettings
import com.github.srwi.pixellens.dialogs.ImageViewer
import com.github.srwi.pixellens.imageProviders.ImageProviderFactory
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.xdebugger.impl.ui.tree.actions.XDebuggerTreeActionBase
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
                    val expression = getExpression(value)
                    val batch = imageProvider.getBatchByExpression(value.frameAccessor, progressIndicator, expression)
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
                            "notificationGroup.error",
                            "Out of memory",
                            "The IDE ran out of memory while trying to view the image. Please try again with a smaller slice of the data.",
                            NotificationType.ERROR
                        ),
                        project
                    )
                } catch (e: Throwable) {
                    val formattedException = e.toString() + "\n" + e.stackTrace.joinToString("\n")
                    Notifications.Bus.notify(
                        Notification(
                            "notificationGroup.error",
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
            val value = XDebuggerTreeActionBase.getSelectedValue(e.dataContext) as PyDebugValue
            val imageProvider = ImageProviderFactory.getImageProvider(value.typeQualifier as String)
            e.presentation.isVisible = imageProvider.typeSupported(value)
            e.presentation.isEnabled = imageProvider.shapeSupported(value)
        } catch (_: Exception) {
            e.presentation.isEnabledAndVisible = false
        }
    }

    private fun getExpression(value: PyDebugValue): String {
        // Usually we would use 'evaluationExpression' to get the full path of the variable.
        // Inside the evaluate expression window however the result will be assigned to a temporary
        // variable and 'name' will be the full evaluation expression instead.
        return if (value.parent == null) value.name else value.evaluationExpression
    }
}