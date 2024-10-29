package com.github.srwi.pixellens.data

import com.github.srwi.pixellens.UserSettings
import com.github.srwi.pixellens.actions.CopyAndReportExceptionAction
import com.github.srwi.pixellens.actions.ReportToGithubAction
import com.github.srwi.pixellens.imageProviders.ImageProviderFactory
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XDebugSessionListener
import com.intellij.xdebugger.XDebuggerManager
import com.jetbrains.python.debugger.PyDebugValue
import java.util.concurrent.atomic.AtomicReference

class DebugImageValue(
    private val project: Project, private val pyDebugValue: PyDebugValue
) {
    private val observers = mutableListOf<BatchChangeObserver>()

    private val debugListener = object : XDebugSessionListener {
        override fun sessionPaused() {
            update()
        }
    }

    private var batch: Batch? = null

    // TODO: better solution for !!
    val data: BatchData get() = batch!!.data
    val expression: String get() = batch!!.expression
    val shape: List<Int> get() = batch!!.shape
    val dtype: String get() = batch!!.dtype

    var normalizeEnabled: Boolean = UserSettings.normalizeEnabled
    var transposeEnabled: Boolean = UserSettings.transposeEnabled
    var reverseChannelsEnabled: Boolean = UserSettings.reverseChannelsEnabled
    var applyColormapEnabled: Boolean = UserSettings.applyColormapEnabled

    private val currentIndicator = AtomicReference<ProgressIndicator?>(null)

    init {
        registerDebugListener()
        update()
    }

    fun addObserver(observer: BatchChangeObserver) {
        observers.add(observer)
    }

    fun removeObserver(observer: BatchChangeObserver) {
        observers.remove(observer)
    }

    private fun notifyObservers() {
        observers.forEach { it.onValueChanged(this) }
    }

    private fun registerDebugListener() {
        val debuggerManager = XDebuggerManager.getInstance(project)
        val session = debuggerManager.currentSession ?: return
        session.addSessionListener(debugListener)
    }

    fun unregisterDebugListener() {
        val debuggerManager = XDebuggerManager.getInstance(project)
        val session = debuggerManager.currentSession ?: return
        session.removeSessionListener(debugListener)
    }

    private fun update() {
        val previousIndicator = currentIndicator.getAndSet(null)
        previousIndicator?.cancel()

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Loading image...", true) {
            override fun run(progressIndicator: ProgressIndicator) {
                currentIndicator.set(progressIndicator)
                try {
                    if (progressIndicator.isCanceled) return

                    val imageProvider = ImageProviderFactory.getImageProvider(pyDebugValue.typeQualifier as String)
                    val expression = getExpression(pyDebugValue)
                    batch = imageProvider.getBatchByExpression(pyDebugValue.frameAccessor, progressIndicator, expression)

                    if (!progressIndicator.isCanceled) {
                        ApplicationManager.getApplication().invokeLater {
                            notifyObservers()
                        }
                    }
                } catch (_: InterruptedException) {
                    // Operation cancelled by user
                } catch (_: OutOfMemoryError) {
                    if (!progressIndicator.isCanceled) {
                        ApplicationManager.getApplication().invokeLater {
                            Notifications.Bus.notify(
                                Notification(
                                    "notificationGroup.error",
                                    "Out of memory",
                                    "The IDE ran out of memory while trying to view the image. Please try again with a smaller slice of the data.",
                                    NotificationType.ERROR
                                ),
                                project
                            )
                        }
                    }
                } catch (e: Throwable) {
                    if (!progressIndicator.isCanceled) {
                        ApplicationManager.getApplication().invokeLater {
                            val formattedException = e.message + "\n" + e.stackTrace.joinToString("\n")
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
                } finally {
                    currentIndicator.compareAndSet(progressIndicator, null)
                }
            }
        })
    }

    private fun getExpression(value: PyDebugValue): String {
        // Usually we would use 'evaluationExpression' to get the full path of the variable.
        // Inside the evaluate expression window however the result will be assigned to a temporary
        // variable and 'name' will be the full evaluation expression instead.
        return if (value.parent == null) value.name else value.evaluationExpression
    }
}

interface BatchChangeObserver {
    fun onValueChanged(debugImageValue: DebugImageValue)
}