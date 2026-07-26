package com.github.srwi.pixellens.debugger

import com.intellij.xdebugger.frame.XValue
import com.jetbrains.python.debugger.PyDebugValue
import com.jetbrains.python.debugger.PyFrameAccessor

object DapDebugger {
    private const val VALUE_CLASS = "com.intellij.python.dap.xdebugger.PythonDapValue"

    private val FRAME_ACCESSOR_CLASSES = setOf(
        "com.intellij.python.dap.PythonDapFrameAccessor",
        "com.intellij.python.dap.xdebugger.PythonDapDebugProcess"
    )

    fun isDapFrameAccessor(frameAccessor: PyFrameAccessor): Boolean {
        // PythonDapDebugProcess is subclassed (e.g. by the Jupyter plugin), so check the whole hierarchy
        return generateSequence<Class<*>>(frameAccessor.javaClass) { it.superclass }
            .any { it.name in FRAME_ACCESSOR_CLASSES }
    }

    fun asPyDebugValue(value: XValue?): PyDebugValue? {
        if (value == null || value.javaClass.name != VALUE_CLASS) return null

        val frameAccessor = call<PyFrameAccessor>(value, "getFrameAccessor") ?: return null
        // debugpy hands out the full path of a variable, so the converted value needs no
        // parent chain to stay resolvable in the current frame.
        val expression = call<String>(value, "getEvaluationExpression") ?: return null

        val type = call<String>(value, "getType")
        return PyDebugValue(
            expression,
            type,
            getTypeQualifier(call(value, "getQualifiedType"), type),
            null,
            call<String>(value, "getShape"),
            call<String>(value, "getDtype"),
            false,
            false,
            false,
            "",
            frameAccessor
        )
    }

    private fun getTypeQualifier(qualifiedType: String?, type: String?): String {
        if (qualifiedType == null || type == null) return ""
        return qualifiedType.removeSuffix(type).removeSuffix(".")
    }

    private inline fun <reified T> call(receiver: Any, methodName: String): T? {
        return try {
            receiver.javaClass.getMethod(methodName).invoke(receiver) as? T
        } catch (_: Exception) {
            null
        }
    }
}
