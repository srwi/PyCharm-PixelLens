package com.github.srwi.pycharmpixelglance.dialogs

import org.intellij.images.editor.ImageZoomModel
import org.intellij.images.options.OptionsManager
import org.intellij.images.ui.ImageComponent
import java.awt.Dimension
import javax.swing.JScrollPane
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class ImageZoomModelImpl(private val imageComponent: ImageComponent) : ImageZoomModel {
    private var zoomLevelChanged = true
    private var zoomFactor = 1.0

    override fun getZoomFactor(): Double = zoomFactor

    override fun setZoomFactor(zoomFactor: Double) {
        val oldZoomFactor = this.zoomFactor
        if (oldZoomFactor != zoomFactor) {
            this.zoomFactor = zoomFactor
            repaintImageComponent()
            zoomLevelChanged = false
        }
    }

    private fun repaintImageComponent() {
        updateImageComponentSize()
        imageComponent.revalidate()
        imageComponent.repaint()
    }

    override fun fitZoomToWindow() {
        val image = imageComponent.document.value ?: return
        val scrollPane = (imageComponent.parent.parent.parent as JScrollPane)
        val verticalScrollBarWidth = scrollPane.verticalScrollBar.preferredSize.width
        val horizontalScrollBarHeight = scrollPane.horizontalScrollBar.preferredSize.height
        val adjustedWidth = scrollPane.viewport.width - verticalScrollBarWidth
        val adjustedHeight = scrollPane.viewport.height - horizontalScrollBarHeight
        val widthRatio = adjustedWidth.toDouble() / image.width
        val heightRatio = adjustedHeight.toDouble() / image.height
        val newZoomFactor = min(widthRatio, heightRatio)
        setZoomFactor(newZoomFactor)
        zoomLevelChanged = false
    }

    fun smartZoom(scrollPane: JScrollPane) {
        val image = imageComponent.document.value ?: return
        val options = OptionsManager.getInstance().options
        val zoomOptions = options.editorOptions.zoomOptions
        if (zoomOptions.isSmartZooming) {
            val prefferedSize = zoomOptions.prefferedSize
            if (prefferedSize.width > image.width && prefferedSize.height > image.height) {
                val factor = (prefferedSize.getWidth() / image.width.toDouble() + prefferedSize.getHeight() / image.height.toDouble()) / 2.0
                setZoomFactor(ceil(factor))
                zoomLevelChanged = true
                return
            } else if (image.width > scrollPane.viewport.width || image.height > scrollPane.viewport.height) {
                fitZoomToWindow()
                return
            }
        }

        setZoomFactor(1.0)
        repaintImageComponent()
        zoomLevelChanged = true
    }

    private val minimumZoomFactor: Double
        get() {
            val bounds = imageComponent.document.bounds
            val factor = bounds?.let { 1.0 / it.width } ?: 0.0
            return max(factor, ImageZoomModel.MICRO_ZOOM_LIMIT)
        }

    private val maximumZoomFactor: Double
        get() = min(ImageZoomModel.MACRO_ZOOM_LIMIT, Double.MAX_VALUE)

    override fun zoomOut() {
        setZoomFactor(getNextZoomOut())
        zoomLevelChanged = true
    }

    override fun zoomIn() {
        setZoomFactor(getNextZoomIn())
        zoomLevelChanged = true
    }

    private fun getNextZoomOut(): Double {
        var factor = zoomFactor
        if (factor > 1.0) {
            factor /= 2.0
            factor = max(factor, 1.0)
        } else {
            factor /= 1.5
        }
        return max(factor, minimumZoomFactor)
    }

    private fun getNextZoomIn(): Double {
        var factor = zoomFactor
        if (factor >= 1.0) {
            factor *= 2.0
        } else {
            factor *= 1.5
            factor = min(factor, 1.0)
        }
        return min(factor, maximumZoomFactor)
    }

    override fun setZoomLevelChanged(value: Boolean) {
        zoomLevelChanged = value
    }

    override fun canZoomOut(): Boolean {
        return zoomFactor - 1e-14 > minimumZoomFactor
    }

    override fun canZoomIn(): Boolean {
        return zoomFactor < maximumZoomFactor
    }

    override fun isZoomLevelChanged(): Boolean {
        return zoomLevelChanged
    }

    private fun updateImageComponentSize() {
        val image = imageComponent.document.value
        image?.let {
            val newWidth = (it.width * zoomFactor).toInt()
            val newHeight = (it.height * zoomFactor).toInt()
            imageComponent.canvasSize = Dimension(newWidth, newHeight)
        }
    }
}