package com.github.srwi.pixellens.dialogs

import com.github.srwi.pixellens.data.Batch
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import org.intellij.images.ui.ImageComponentDecorator

class ImageViewerPopup(val project: Project, batch: Batch) : DataProvider, Disposable {
    val imageViewer = ImageViewer(batch)
    private val popup: JBPopup

    init {
        val contentPanel = imageViewer.createContentPanel()
        popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(contentPanel, null)
            .setTitle(batch.expression)
            .setResizable(true)
            .setMovable(true)
            .setProject(project)
            .setDimensionServiceKey(project, "com.github.srwi.pixellens.dialogs.ImageViewerPopup", true)
            .setCancelOnClickOutside(true)
            .setCancelOnWindowDeactivation(true)
            .setFocusable(true)
            .setRequestFocus(true)
            .createPopup()
        popup.setMinimumSize(contentPanel.minimumSize)
        popup.setDataProvider(this)
    }

    fun show() {
        popup.showInFocusCenter()
    }

    override fun getData(dataId: String): Any? {
        if (ImageComponentDecorator.DATA_KEY.`is`(dataId)) {
            return imageViewer
        }
        return null
    }

    override fun dispose() {
        imageViewer.dispose()
        popup.dispose()
    }
}