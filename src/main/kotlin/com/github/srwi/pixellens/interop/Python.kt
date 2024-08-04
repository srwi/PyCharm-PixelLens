package com.github.srwi.pixellens.interop

import com.jetbrains.python.debugger.PyDebugValue
import com.jetbrains.python.debugger.PyDebuggerException
import com.jetbrains.python.debugger.PyFrameAccessor

object Python {
    fun evaluateExpression(frameAccessor: PyFrameAccessor, expression: String) : PyDebugValue? {
        return try {
            frameAccessor.evaluate(expression, false, false)
        } catch (_: PyDebuggerException) {
            null
        }
    }

    fun executeStatement(frameAccessor: PyFrameAccessor, statement: String) : PyDebugValue? {
        return try {
            frameAccessor.evaluate(statement, true, false)
        } catch (_: PyDebuggerException) {
            null
        }
    }
}