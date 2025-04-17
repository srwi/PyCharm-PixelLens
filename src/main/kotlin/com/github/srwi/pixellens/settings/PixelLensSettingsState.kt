package com.github.srwi.pixellens.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(name = "PixelLensSettings", storages = [Storage("PixelLensSettings.xml")])
class PixelLensSettingsState : PersistentStateComponent<PixelLensSettingsState> {
    var alwaysUseEvaluateTransmission: Boolean = false
    var usePopupWindow: Boolean = false

    override fun getState(): PixelLensSettingsState {
        return this
    }

    override fun loadState(state: PixelLensSettingsState) {
        this.alwaysUseEvaluateTransmission = state.alwaysUseEvaluateTransmission
        this.usePopupWindow = state.usePopupWindow
    }

    companion object {
        val instance: PixelLensSettingsState
            get() = ApplicationManager.getApplication().getService(PixelLensSettingsState::class.java)
    }
}