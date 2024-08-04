package com.github.srwi.pixellens

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.CachedSingletonsRegistry
import java.util.function.Supplier

class UserSettings {
    companion object {
        private val propertiesSupplier: Supplier<PropertiesComponent> = CachedSingletonsRegistry.lazy { PropertiesComponent.getInstance() }

        var normalizeEnabled: Boolean
            get() = propertiesSupplier.get().getBoolean("normalizeEnabled", false)
            set(value) = propertiesSupplier.get().setValue("normalizeEnabled", value)

        var transposeEnabled: Boolean
            get() = propertiesSupplier.get().getBoolean("transposeEnabled", false)
            set(value) = propertiesSupplier.get().setValue("transposeEnabled", value)

        var reverseChannelsEnabled: Boolean
            get() = propertiesSupplier.get().getBoolean("reverseChannelsEnabled", false)
            set(value) = propertiesSupplier.get().setValue("reverseChannelsEnabled", value)

        var applyColormapEnabled: Boolean
            get() = propertiesSupplier.get().getBoolean("applyColormapEnabled", false)
            set(value) = propertiesSupplier.get().setValue("applyColormapEnabled", value)
    }
}
