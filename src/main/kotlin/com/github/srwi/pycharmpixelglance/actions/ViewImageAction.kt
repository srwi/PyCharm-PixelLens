package com.github.srwi.pycharmpixelglance.actions

import com.github.srwi.pycharmpixelglance.dialogs.ImageViewDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.xdebugger.impl.ui.tree.actions.XDebuggerTreeActionBase
import com.jetbrains.python.debugger.PyDebugValue
import com.jetbrains.python.debugger.PyDebuggerException
import com.jetbrains.python.debugger.PyFrameAccessor


class ViewImageAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val value = XDebuggerTreeActionBase.getSelectedValue(e.dataContext) as PyDebugValue? ?: return
        val frameAccessor = value.frameAccessor

        val base64Array = getNumpyArray(frameAccessor, value.name)
        if (base64Array != null) {
            val diag = ImageViewDialog(project, base64Array)
            diag.show()
        }
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
        val command = ("""
            import cv2 as pixelglance_cv2
            import base64 as pixelglance_base64
            pixelglance_success, pixelglance_buffer = pixelglance_cv2.imencode('.png', """ + name + """)
            pixelglance_encoded = pixelglance_base64.b64encode(pixelglance_buffer).decode('utf-8')
        """).trimIndent()
        executeStatement(frameAccessor, command) ?: return null

        val success = evaluateExpression(frameAccessor, "pixelglance_success")?.value.toBoolean()
        if (!success) {
            return null
        }

        val encoded = evaluateExpression(frameAccessor, "pixelglance_encoded")?.value

        val cleanupCommand = ("""
            del pixelglance_cv2, pixelglance_base64
            del pixelglance_success, pixelglance_buffer, pixelglance_encoded
        """).trimIndent()
        executeStatement(frameAccessor, cleanupCommand) ?: return null

        return encoded
    }
}