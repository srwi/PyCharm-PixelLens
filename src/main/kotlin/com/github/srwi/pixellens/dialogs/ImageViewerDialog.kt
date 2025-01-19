package com.github.srwi.pixellens.dialogs

import com.github.srwi.pixellens.data.Batch
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.ui.JBUI
import org.intellij.images.ui.ImageComponentDecorator
import javax.swing.JComponent
import javax.swing.border.Border

class ImageViewerDialog(project: Project, batch: Batch) : DialogWrapper(project), DataProvider {
    val imageViewer = ImageViewer(batch)

    init {
        title = batch.expression
        isModal = false

        init()
    }

    override fun createCenterPanel(): JComponent {
        return imageViewer.createContentPanel()
    }

    override fun createNorthPanel(): JComponent? {
        return null
    }

    override fun createSouthPanel(): JComponent? {
        return null
    }

    override fun createContentPaneBorder(): Border {
        return JBUI.Borders.empty()
    }

    override fun getDimensionServiceKey() = "com.github.srwi.pixellens.dialogs.ImageViewerDialog"

    override fun getData(dataId: String): Any? {
        if (ImageComponentDecorator.DATA_KEY.`is`(dataId)) {
            return imageViewer
        }
        return null
    }

    override fun dispose() {
        imageViewer.dispose()
        super.dispose()
    }
}