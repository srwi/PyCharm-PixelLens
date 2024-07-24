package com.github.srwi.pycharmpixelglance.dialogs

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.ui.DialogWrapper
import java.awt.Dimension
import javax.swing.JComponent

abstract class PersistentDialogWrapper(
    canBeParent: Boolean = true,
    private val defaultWidth: Int = 800,
    private val defaultHeight: Int = 600
) : DialogWrapper(canBeParent) {

    companion object {
        private const val WINDOW_WIDTH_KEY = "PersistentDialogWrapper.WindowWidth"
        private const val WINDOW_HEIGHT_KEY = "PersistentDialogWrapper.WindowHeight"
    }

    init {
        val size = loadWindowSize()
        setSize(size.width, size.height)
    }

    private fun loadWindowSize(): Dimension {
        val properties = PropertiesComponent.getInstance()
        val width = properties.getInt(WINDOW_WIDTH_KEY, defaultWidth)
        val height = properties.getInt(WINDOW_HEIGHT_KEY, defaultHeight)
        return Dimension(width, height)
    }

    private fun saveWindowSize(size: Dimension) {
        val properties = PropertiesComponent.getInstance()
        properties.setValue(WINDOW_WIDTH_KEY, size.width, defaultWidth)
        properties.setValue(WINDOW_HEIGHT_KEY, size.height, defaultHeight)
    }

    override fun dispose() {
        saveWindowSize(size)
        super.dispose()
    }

    override fun createCenterPanel(): JComponent? {
        return null
    }
}
