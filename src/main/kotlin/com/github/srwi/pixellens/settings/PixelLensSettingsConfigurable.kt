package com.github.srwi.pixellens.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.TitledSeparator
import com.intellij.util.ui.FormBuilder
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel

class PixelLensSettingsConfigurable : Configurable {
    private var evaluateCheckBox: JCheckBox? = null

    override fun createComponent(): JComponent? {
        evaluateCheckBox = JCheckBox("Always use evaluate transmission (slower)")

        return FormBuilder.createFormBuilder()
            .addComponent(TitledSeparator("Advanced"))
            .setFormLeftIndent(20)
            .addComponent(evaluateCheckBox!!, 1)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    override fun isModified(): Boolean {
        val settings = PixelLensSettingsState.instance
        return settings.alwaysUseEvaluateTransmission != evaluateCheckBox!!.isSelected
    }

    override fun apply() {
        val settings = PixelLensSettingsState.instance
        settings.alwaysUseEvaluateTransmission = evaluateCheckBox!!.isSelected
    }

    override fun reset() {
        val settings = PixelLensSettingsState.instance
        evaluateCheckBox!!.isSelected = settings.alwaysUseEvaluateTransmission
    }

    override fun getDisplayName(): String {
        return "PixelLens"
    }
}