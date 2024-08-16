package com.github.srwi.pixellens.actions

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ide.CopyPasteManager
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException

class CopyAndReportExceptionAction(text: String, private val exceptionText: String) : NotificationAction(text) {
    override fun actionPerformed(e: AnActionEvent, notification: Notification) {
        val transferable = object : Transferable {
            override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(DataFlavor.stringFlavor)
            override fun isDataFlavorSupported(flavor: DataFlavor): Boolean = DataFlavor.stringFlavor.equals(flavor)
            override fun getTransferData(flavor: DataFlavor): Any {
                if (isDataFlavorSupported(flavor)) return exceptionText
                throw UnsupportedFlavorException(flavor)
            }
        }
        CopyPasteManager.getInstance().setContents(transferable)

        notification.expire()
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}