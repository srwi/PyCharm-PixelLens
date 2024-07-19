package com.github.srwi.pycharmpixelglance.actions

import com.github.srwi.pycharmpixelglance.data.NumpyImageProvider
import com.github.srwi.pycharmpixelglance.data.PillowImageProvider
import com.github.srwi.pycharmpixelglance.data.PytorchImageProvider
import com.github.srwi.pycharmpixelglance.data.TensorflowImageProvider
import com.github.srwi.pycharmpixelglance.dialogs.ImageViewerDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.xdebugger.impl.ui.tree.actions.XDebuggerTreeActionBase
import com.jetbrains.python.debugger.PyDebugValue

class ViewImageAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
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

        val displayableData = imageProvider.getImageByVariableName(frameAccessor, value.name)
        val image = displayableData.getBuffer()

        val viewerDialog = ImageViewerDialog(project, image)
        viewerDialog.showDialog()
    }
}