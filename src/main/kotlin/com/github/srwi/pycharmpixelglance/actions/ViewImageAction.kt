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
        val result3 = executeStatement(frameAccessor, "import time\ntime.sleep(1)\nbla = 1")
        val result1 = evaluateExpression(frameAccessor, "bla+3")
        var foo = 0
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
}