package com.github.srwi.pixellens

import com.intellij.ide.util.PropertiesComponent

class UserSettings {
    companion object {
        private fun getPropertiesComponent(): PropertiesComponent {
            return PropertiesComponent.getInstance()
        }

        var normalizeEnabled: Boolean
            get() = getPropertiesComponent().getBoolean("normalizeEnabled", false)
            set(value) = getPropertiesComponent().setValue("normalizeEnabled", value)

        var transposeEnabled: Boolean
            get() = getPropertiesComponent().getBoolean("transposeEnabled", false)
            set(value) = getPropertiesComponent().setValue("transposeEnabled", value)

        var reverseChannelsEnabled: Boolean
            get() = getPropertiesComponent().getBoolean("reverseChannelsEnabled", false)
            set(value) = getPropertiesComponent().setValue("reverseChannelsEnabled", value)

        var applyColormapEnabled: Boolean
            get() = getPropertiesComponent().getBoolean("applyColormapEnabled", false)
            set(value) = getPropertiesComponent().setValue("applyColormapEnabled", value)
    }
}