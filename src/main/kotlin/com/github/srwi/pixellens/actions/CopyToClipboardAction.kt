package com.github.srwi.pixellens.actions

import com.github.srwi.pixellens.dialogs.ImageViewer
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareAction
import org.intellij.images.editor.actionSystem.ImageEditorActionUtil
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException

class CopyToClipboardAction : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val imageViewer = ImageEditorActionUtil.getImageComponentDecorator(e) as? ImageViewer ?: return
        val image = imageViewer.getDisplayedImage()
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