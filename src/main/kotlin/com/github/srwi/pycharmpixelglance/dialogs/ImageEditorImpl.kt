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


// TODO: make image watchable and react to image changes
internal class ImageEditorImpl(private val project: Project, image: BufferedImage) : ImageEditor, Disposable {
    private val optionsChangeListener: PropertyChangeListener = OptionsChangeListener()
    private val editorUI: ImageEditorUI
    private var disposed = false

    init {
        // Options
        val options = OptionsManager.getInstance().options
        editorUI = ImageEditorUI(this, options.editorOptions)
        options.addPropertyChangeListener(optionsChangeListener, this)
        setValue(image)
    }

    private fun setValue(image: BufferedImage) {
        val document: ImageDocument = editorUI.imageComponent.document
        try {
            val previousImage = document.value
            document.value = image
//            document.format = IfsUtil.getFormat(file)
            val zoomModel = zoomModel
            if (previousImage == null || !zoomModel.isZoomLevelChanged) {
                // Set smart zooming behaviour on open
                val options = OptionsManager.getInstance().options
                val zoomOptions = options.editorOptions.zoomOptions
                // Open as actual size
                zoomModel.zoomFactor = 1.0
                if (zoomOptions.isSmartZooming) {
                    val prefferedSize = zoomOptions.prefferedSize
                    if (prefferedSize.width > image.width && prefferedSize.height > image.height) {
                        // Resize to preffered size
                        // Calculate zoom factor
                        val factor = (prefferedSize.getWidth() / image.width.toDouble() + prefferedSize.getHeight() / image.height.toDouble()) / 2.0
                        zoomModel.zoomFactor = ceil(factor)
                    }
                }
            }
        } catch (e: Exception) {
            // Error loading image file
            document.value = null
        }
    }

    override fun isValid(): Boolean {
        val document: ImageDocument = editorUI.imageComponent.getDocument()
        return document.value != null
    }

    override fun getComponent(): JComponent {
        return editorUI
    }

    override fun getContentComponent(): JComponent {
        return editorUI.imageComponent
    }

    override fun getFile(): VirtualFile {
        // TODO: this should never be called because we are using BufferedImage instead of VirtualFile
        return file
    }

    override fun getProject(): Project {
        return project
    }

    override fun getDocument(): ImageDocument {
        return editorUI.imageComponent.getDocument()
    }

    override fun setTransparencyChessboardVisible(visible: Boolean) {
        editorUI.imageComponent.setTransparencyChessboardVisible(visible)
        editorUI.repaint()
    }

    override fun isTransparencyChessboardVisible(): Boolean {
        return editorUI.imageComponent.isTransparencyChessboardVisible()
    }

    override fun isEnabledForActionPlace(place: String): Boolean {
        // Disable for thumbnails action
        return ThumbnailViewActions.ACTION_PLACE != place
    }

    override fun setGridVisible(visible: Boolean) {
        editorUI.imageComponent.setGridVisible(visible)
        editorUI.repaint()
    }

    override fun isGridVisible(): Boolean {
        return editorUI.imageComponent.isGridVisible()
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