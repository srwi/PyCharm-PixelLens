package com.github.srwi.pixellens.debugger

import com.github.srwi.pixellens.data.Batch
import com.github.srwi.pixellens.imageProviders.ImageProviderFactory
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebugSessionListener
import com.intellij.xdebugger.XDebuggerManager
import com.jetbrains.python.debugger.PyDebugValue
import java.util.concurrent.atomic.AtomicReference

object DebugValueLoader {
    fun loadBatch(value: PyDebugValue, progressIndicator: ProgressIndicator): Batch {
        val imageProvider = ImageProviderFactory.getImageProvider(value.typeQualifier as String)
        return imageProvider.getBatchByExpression(value.frameAccessor, progressIndicator, getExpression(value))
    }

    private fun getExpression(value: PyDebugValue): String {
        // Usually we would use 'evaluationExpression' to get the full path of the variable.
        // Inside the evaluate expression window however the result will be assigned to a temporary
        // variable and 'name' will be the full evaluation expression instead.
        return if (value.parent == null) value.name else value.evaluationExpression
    }
}

class DebugValueWatcher(
    private val project: Project,
    private val value: PyDebugValue,
    private val onBatchLoaded: (Batch) -> Unit,
) : Disposable {

    private val currentIndicator = AtomicReference<ProgressIndicator?>(null)
    private var session: XDebugSession? = null
    private var started = false

    private val sessionListener = object : XDebugSessionListener {
        override fun sessionPaused() {
            reload()
        }
    }

    fun start() {
        if (started) return
        started = true
        // Console and other non-debugger frame accessors have no session and therefore no stepping
        // to react to. In that case the watcher simply stays idle.
        session = XDebuggerManager.getInstance(project).currentSession
        session?.addSessionListener(sessionListener)
    }

    private fun reload() {
        currentIndicator.getAndSet(null)?.cancel()

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Updating image...", true) {
            override fun run(progressIndicator: ProgressIndicator) {
                currentIndicator.set(progressIndicator)
                try {
                    if (progressIndicator.isCanceled) return

                    val batch = DebugValueLoader.loadBatch(value, progressIndicator)

                    if (!progressIndicator.isCanceled) {
                        ApplicationManager.getApplication().invokeLater {
                            onBatchLoaded(batch)
                        }
                    }
                } catch (_: InterruptedException) {
                    // Superseded by a newer reload or canceled while disposing.
                } catch (_: Throwable) {
                    // The variable may be out of scope or unreadable in the new frame.
                    // Keep the previously displayed image rather than interrupting the user.
                } finally {
                    currentIndicator.compareAndSet(progressIndicator, null)
                }
            }
        })
    }

    override fun dispose() {
        currentIndicator.getAndSet(null)?.cancel()
        session?.removeSessionListener(sessionListener)
        session = null
    }
}
