package com.github.srwi.pixellens.actions

import com.github.srwi.pixellens.UserSettings
import com.github.srwi.pixellens.dialogs.ImageViewerFactory
import com.github.srwi.pixellens.imageProviders.ImageProviderFactory
import com.github.srwi.pixellens.interop.Python
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.impl.ui.tree.actions.XDebuggerTreeActionBase
import com.jetbrains.python.debugger.PyDebugValue
import com.jetbrains.python.debugger.PyFrameAccessor
import javax.swing.SwingUtilities

class ViewAsImageAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val value = XDebuggerTreeActionBase.getSelectedValue(e.dataContext) as PyDebugValue? ?: return

        if (!checkPythonCompatibility(value.frameAccessor, project)) {
            return
        }

        // It may be possible to view as image without the debugger, but for now we warn the user instead
        if (catchJupyterVarFrame(value.frameAccessor, project)) {
            return
        }

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
                        ImageViewerFactory.show(project, batch)
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

    private fun checkPythonCompatibility(frameAccessor: PyFrameAccessor, project: Project): Boolean {
        val pythonVersion = Python.getInterpreterVersion(frameAccessor)

        // https://github.com/srwi/PyCharm-PixelLens/issues/36
        val isCompatible = pythonVersion != "3.13.0"

        if (!isCompatible) {
            Notifications.Bus.notify(
                Notification(
                    "notificationGroup.error",
                    "Incompatible python version",
                    "You are currently using Python 3.13.0, which is affected by a known bug that prevents compatibility with PixelLens. " +
                            "Please update to Python 3.13.1 or later to continue using PixelLens.",
                    NotificationType.WARNING
                ),
                project
            )
        }

        return isCompatible
    }

    private fun catchJupyterVarFrame(frameAccessor: PyFrameAccessor, project: Project): Boolean {
        val isJupyter = frameAccessor.javaClass.simpleName == "JupyterVarsFrameAccessor"

        if (isJupyter) {
            Notifications.Bus.notify(
                Notification(
                    "notificationGroup.error",
                    "Run cell in debugger to view as image",
                    "To view variables as images, this cell must be executed within the debugger. " +
                            "Please rerun the cell using the debugger and try viewing the variable as image again.",
                    NotificationType.WARNING
                ),
                project
            )
        }

        return isJupyter
    }
}