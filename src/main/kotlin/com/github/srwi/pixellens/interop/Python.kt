package com.github.srwi.pixellens.interop

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

    fun executeStatement(frameAccessor: PyFrameAccessor, statement: String) {
        if (frameAccessor is PydevConsoleCommunication) {
            executeStatementInConsole(frameAccessor, statement)
        } else {
            frameAccessor.evaluate(statement, true, false)
        }
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