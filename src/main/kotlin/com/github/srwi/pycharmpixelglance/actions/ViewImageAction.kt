package com.github.srwi.pycharmpixelglance.actions

import com.github.srwi.pycharmpixelglance.data.NumpyImageProvider
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.xdebugger.impl.ui.tree.actions.XDebuggerTreeActionBase
import com.jetbrains.python.debugger.PyDebugValue

class ViewImageAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val value = XDebuggerTreeActionBase.getSelectedValue(e.dataContext) as PyDebugValue? ?: return
        val frameAccessor = value.frameAccessor

        // if (is numpy type) {
            val numpyImageProvider = NumpyImageProvider()
            val imageArray = numpyImageProvider.getVariable(frameAccessor, value.name)
        // }

        val pause = 0
    }
}