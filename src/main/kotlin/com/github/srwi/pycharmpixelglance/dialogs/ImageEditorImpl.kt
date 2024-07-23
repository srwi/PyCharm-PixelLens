package com.github.srwi.pycharmpixelglance.dialogs

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.intellij.images.editor.ImageDocument
import org.intellij.images.editor.ImageEditor
import org.intellij.images.editor.ImageZoomModel
import org.intellij.images.options.Options
import org.intellij.images.options.OptionsManager
import org.intellij.images.thumbnail.actionSystem.ThumbnailViewActions
import org.intellij.images.ui.ImageComponent
import java.awt.image.BufferedImage
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import kotlin.math.ceil

internal class ImageEditorImpl(private val project: Project, image: BufferedImage) : ImageEditor, Disposable {
    private val optionsChangeListener: PropertyChangeListener = OptionsChangeListener()
    private val editorUI: ImageEditorUI
    private var disposed = false

    init {
        val options = OptionsManager.getInstance().options
        editorUI = ImageEditorUI(project, this, options.editorOptions)
        options.addPropertyChangeListener(optionsChangeListener, this)
        setImage(image)
    }

    private fun setImage(image: BufferedImage) {
        val document: ImageDocument = editorUI.imageComponent.document
        try {
            val previousImage = document.value
            document.value = image
            val zoomModel = zoomModel
            if (previousImage == null || !zoomModel.isZoomLevelChanged) {
                val options = OptionsManager.getInstance().options
                val zoomOptions = options.editorOptions.zoomOptions
                zoomModel.zoomFactor = 1.0
                if (zoomOptions.isSmartZooming) {
                    val prefferedSize = zoomOptions.prefferedSize
                    if (prefferedSize.width > image.width && prefferedSize.height > image.height) {
                        val factor = (prefferedSize.getWidth() / image.width.toDouble() + prefferedSize.getHeight() / image.height.toDouble()) / 2.0
                        zoomModel.zoomFactor = ceil(factor)
                    }
                }
            }
        } catch (e: Exception) {
            document.value = null
        }
    }

    override fun isValid(): Boolean {
        val document: ImageDocument = editorUI.imageComponent.document
        return document.value != null
    }

    override fun getComponent(): JComponent {
        return editorUI.contentPane as JComponent
    }

    override fun getContentComponent(): JComponent {
        return editorUI.imageComponent
    }

    override fun getFile(): VirtualFile {
        throw UnsupportedOperationException("getFile() is not supported for BufferedImage-based editor")
    }

    override fun getProject(): Project {
        return project
    }

    override fun getDocument(): ImageDocument {
        return editorUI.imageComponent.document
    }

    override fun setTransparencyChessboardVisible(visible: Boolean) {
        editorUI.imageComponent.isTransparencyChessboardVisible = visible
        editorUI.contentPane.repaint()
    }

    override fun isTransparencyChessboardVisible(): Boolean {
        return editorUI.imageComponent.isTransparencyChessboardVisible
    }

    override fun isEnabledForActionPlace(place: String): Boolean {
        return ThumbnailViewActions.ACTION_PLACE != place
    }

    override fun setGridVisible(visible: Boolean) {
        editorUI.imageComponent.isGridVisible = visible
        editorUI.contentPane.repaint()
    }

    override fun isGridVisible(): Boolean {
        return editorUI.imageComponent.isGridVisible
    }

    override fun isDisposed(): Boolean {
        return disposed
    }

    override fun getZoomModel(): ImageZoomModel {
        return editorUI.zoomModel
    }

    override fun dispose() {
        editorUI.dispose()
        disposed = true
    }

    fun show() {
        editorUI.show()
    }

    private inner class OptionsChangeListener : PropertyChangeListener {
        override fun propertyChange(evt: PropertyChangeEvent) {
            val options = evt.source as Options
            val editorOptions = options.editorOptions
            val chessboardOptions = editorOptions.transparencyChessboardOptions
            val gridOptions = editorOptions.gridOptions
            val imageComponent: ImageComponent = editorUI.imageComponent
            imageComponent.transparencyChessboardCellSize = chessboardOptions.cellSize
            imageComponent.transparencyChessboardWhiteColor = chessboardOptions.whiteColor
            imageComponent.setTransparencyChessboardBlankColor(chessboardOptions.blackColor)
            imageComponent.gridLineZoomFactor = gridOptions.lineZoomFactor
            imageComponent.gridLineSpan = gridOptions.lineSpan
            imageComponent.gridLineColor = gridOptions.lineColor
        }
    }
}