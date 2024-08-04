package com.github.srwi.pixellens.dialogs

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JPanel

class ErrorMessageDialog(project: Project?, dialogTitle: String, private val errorMessage: String, private val exceptionText: String) : DialogWrapper(project) {
    init {
        title = dialogTitle
        init()
    }

    override fun createCenterPanel(): JComponent {
        val dialogPanel = JPanel(BorderLayout())

        val label = JBLabel(errorMessage)

        dialogPanel.add(label, BorderLayout.CENTER)
        dialogPanel.border = JBUI.Borders.empty(10)

        return dialogPanel
    }

    override fun createActions(): Array<Action> {
        val okAction = okAction
        val copyAction = object : DialogWrapperAction("Copy Error") {
            override fun doAction(e: java.awt.event.ActionEvent) {
                val selection = StringSelection(exceptionText)
                Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
            }
        }
        return arrayOf(okAction, copyAction)
    }
}