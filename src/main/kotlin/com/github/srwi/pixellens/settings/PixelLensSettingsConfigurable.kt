package com.github.srwi.pixellens.settings

import com.github.srwi.pixellens.dialogs.ViewerType
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.TitledSeparator
import com.intellij.util.ui.FormBuilder
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel

class PixelLensSettingsConfigurable : Configurable {
    private var evaluateCheckBox: JCheckBox? = null
    private var viewerTypeComboBox: ComboBox<ViewerType>? = null

    override fun createComponent(): JComponent? {
        evaluateCheckBox = JCheckBox("Always use evaluate transmission (slower)")
        viewerTypeComboBox = ComboBox(ViewerType.entries.toTypedArray())

        return FormBuilder.createFormBuilder()
            .addLabeledComponent("Viewer type:", viewerTypeComboBox!!)
            .addComponent(TitledSeparator("Advanced"))
            .setFormLeftIndent(20)
            .addComponent(evaluateCheckBox!!, 1)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    override fun isModified(): Boolean {
        val settings = PixelLensSettingsState.instance
        return settings.alwaysUseEvaluateTransmission != evaluateCheckBox!!.isSelected ||
                settings.usePopupWindow != (viewerTypeComboBox!!.selectedItem == ViewerType.Popup)
    }

    override fun apply() {
        val settings = PixelLensSettingsState.instance
        settings.alwaysUseEvaluateTransmission = evaluateCheckBox!!.isSelected
        settings.usePopupWindow = (viewerTypeComboBox!!.selectedItem == ViewerType.Popup)
    }

    override fun reset() {
        val settings = PixelLensSettingsState.instance
        evaluateCheckBox!!.isSelected = settings.alwaysUseEvaluateTransmission
        viewerTypeComboBox!!.selectedItem = if (settings.usePopupWindow) ViewerType.Popup else ViewerType.Dialog
    }

    override fun getDisplayName(): String {
        return "PixelLens"
    }
}