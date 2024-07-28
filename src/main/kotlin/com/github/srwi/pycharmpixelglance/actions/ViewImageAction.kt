package com.github.srwi.pycharmpixelglance.actions

import com.github.srwi.pycharmpixelglance.dialogs.ImageViewer
import com.github.srwi.pycharmpixelglance.imageProviders.NumpyImageProvider
import com.github.srwi.pycharmpixelglance.imageProviders.PillowImageProvider
import com.github.srwi.pycharmpixelglance.imageProviders.PytorchImageProvider
import com.github.srwi.pycharmpixelglance.imageProviders.TensorflowImageProvider
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.xdebugger.impl.ui.tree.actions.XDebuggerTreeActionBase
import com.jetbrains.python.debugger.PyDebugValue

class ViewImageAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val value = XDebuggerTreeActionBase.getSelectedValue(e.dataContext) as PyDebugValue? ?: return
        val frameAccessor = value.frameAccessor
        val typeQualifier = value.typeQualifier as String
        val imageProvider = when {
            typeQualifier == "numpy" -> NumpyImageProvider()
            typeQualifier == "torch" -> PytorchImageProvider()
            typeQualifier == "PIL.Image" -> PillowImageProvider()
            typeQualifier.startsWith("tensorflow") -> TensorflowImageProvider()
            else -> throw IllegalArgumentException("Unsupported type qualifier: $typeQualifier")
        }
        val displayableData = imageProvider.getDataByVariableName(frameAccessor, value.name)
        val viewer = ImageViewer(displayableData)
        viewer.show()
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val value = XDebuggerTreeActionBase.getSelectedValue(e.dataContext) as PyDebugValue? ?: return
        val typeQualifier = value.typeQualifier as String
        // TODO: check for shape compatibility
        val isSupported = when (typeQualifier) {
            "numpy", "torch", "PIL.Image" -> true
            else -> value.typeQualifier!!.startsWith("tensorflow")
        }
        e.presentation.isVisible = isSupported
    }
}