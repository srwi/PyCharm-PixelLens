package com.github.srwi.pixellens.actions

import com.github.srwi.pixellens.dialogs.ImageViewer
import com.github.srwi.pixellens.imageProviders.ImageProviderFactory
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.xdebugger.impl.ui.tree.actions.XDebuggerTreeActionBase
import com.jetbrains.python.debugger.PyDebugValue

class ViewAsImageAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val value = XDebuggerTreeActionBase.getSelectedValue(e.dataContext) as PyDebugValue? ?: return
        ImageViewer(project, value).show()
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        try {
            val value = XDebuggerTreeActionBase.getSelectedValue(e.dataContext) as PyDebugValue
            val imageProvider = ImageProviderFactory.getImageProvider(value.typeQualifier as String)
            e.presentation.isVisible = imageProvider.typeSupported(value)
            e.presentation.isEnabled = imageProvider.shapeSupported(value)
        } catch (_: Exception) {
            e.presentation.isEnabledAndVisible = false
        }
    }

    private fun getExpression(value: PyDebugValue): String {
        // Usually we would use 'evaluationExpression' to get the full path of the variable.
        // Inside the evaluate expression window however the result will be assigned to a temporary
        // variable and 'name' will be the full evaluation expression instead.
        return if (value.parent == null) value.name else value.evaluationExpression
    }
}