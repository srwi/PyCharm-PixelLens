package com.github.srwi.pycharmpixelglance.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareAction
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.image.BufferedImage

class CopyToClipboardAction(private val getImage: () -> BufferedImage?) : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val image = getImage() ?: return
        val transferable = object : Transferable {
            override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(DataFlavor.imageFlavor)
            override fun isDataFlavorSupported(flavor: DataFlavor): Boolean = DataFlavor.imageFlavor.equals(flavor)
            override fun getTransferData(flavor: DataFlavor): Any {
                if (isDataFlavorSupported(flavor)) return image
                throw UnsupportedFlavorException(flavor)
            }
        }
        CopyPasteManager.getInstance().setContents(transferable)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}