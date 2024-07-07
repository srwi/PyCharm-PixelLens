import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.xdebugger.impl.ui.tree.actions.XDebuggerTreeActionBase
import com.jetbrains.python.debugger.PyDebugValue
import com.jetbrains.python.debugger.PyDebuggerException
import com.jetbrains.python.debugger.PyFrameAccessor


class ViewImageAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val value = XDebuggerTreeActionBase.getSelectedValue(e.dataContext) as PyDebugValue? ?: return
        val frameAccessor = value.frameAccessor
        val base64Array = getNumpyArray(frameAccessor, value.name)
    }

    private fun evaluateExpression(frameAccessor: PyFrameAccessor, expression: String) : PyDebugValue? {
        return try {
            frameAccessor.evaluate(expression, false, false)
        } catch (_: PyDebuggerException) {
            null
        }
    }

    private fun executeStatement(frameAccessor: PyFrameAccessor, statement: String) : PyDebugValue? {
        return try {
            frameAccessor.evaluate(statement, true, false)
        } catch (_: PyDebuggerException) {
            null
        }
    }

    private fun getNumpyArray(frameAccessor: PyFrameAccessor, name: String) : String? {
        // TODO: Get rid of cv2 requirement
        val command = """
            import cv2
            import base64
            success, buffer = cv2.imencode('.png', image)
            encoded = base64.b64encode(buffer).decode('utf-8')
        """.trimIndent()

        // TODO: Clean up Python environment; We don't want to cause any side effects in the debug session

        executeStatement(frameAccessor, command) ?: return null
        return evaluateExpression(frameAccessor, "encoded")?.value
    }
}