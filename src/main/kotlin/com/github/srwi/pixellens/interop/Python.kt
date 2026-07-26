package com.github.srwi.pixellens.interop

import com.github.srwi.pixellens.debugger.DapDebugger
import com.jetbrains.python.console.PydevConsoleCommunication
import com.jetbrains.python.console.pydev.ConsoleCommunication
import com.jetbrains.python.debugger.PyDebugValue
import com.jetbrains.python.debugger.PyDebuggerException
import com.jetbrains.python.debugger.PyFrameAccessor
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object Python {
    fun evaluateExpression(frameAccessor: PyFrameAccessor, expression: String) : PyDebugValue {
        return frameAccessor.evaluate(expression, false, false)
    }

    fun evaluateStringExpression(frameAccessor: PyFrameAccessor, expression: String): String? {
        val value = evaluateExpression(frameAccessor, expression).value ?: return null
        // The debugpy backend returns string results as their repr instead of raw text
        return if (DapDebugger.isDapFrameAccessor(frameAccessor)) PythonStringLiteral.decodeIfLiteral(value) else value
    }

    fun executeStatement(frameAccessor: PyFrameAccessor, statement: String) {
        when {
            frameAccessor is PydevConsoleCommunication -> executeStatementInConsole(frameAccessor, statement)
            DapDebugger.isDapFrameAccessor(frameAccessor) -> frameAccessor.evaluate(statement, true, true)
            else -> frameAccessor.evaluate(statement, true, false)
        }
    }

    fun getInterpreterVersion(frameAccessor: PyFrameAccessor): String {
        return evaluateStringExpression(frameAccessor, "(lambda: __import__('platform').python_version())()")
            ?: throw PyDebuggerException("Failed to determine the Python interpreter version")
    }

    private fun executeStatementInConsole(frameAccessor: PyFrameAccessor, pythonStatement: String) {
        val consoleComm = frameAccessor as PydevConsoleCommunication

        runBlocking {
            suspendCoroutine { continuation ->
                consoleComm.execInterpreter(
                    ConsoleCommunication.ConsoleCodeFragment(pythonStatement, false)
                ) { response ->
                    if (response.more || response.need_input) {
                        throw PyDebuggerException("Got unexpected result when executing Python statement")
                    }

                    continuation.resume(Unit)
                }
            }
        }
    }
}