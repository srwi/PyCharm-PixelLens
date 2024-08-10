package com.github.srwi.pixellens.interop

import com.jetbrains.python.debugger.PyDebugValue
import com.jetbrains.python.debugger.PyFrameAccessor

object Python {
    fun evaluateExpression(frameAccessor: PyFrameAccessor, expression: String) : PyDebugValue {
        return frameAccessor.evaluate(expression, false, false)
    }

    fun executeStatement(frameAccessor: PyFrameAccessor, statement: String) : PyDebugValue {
        return frameAccessor.evaluate(statement, true, false)
    }
}