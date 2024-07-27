package com.github.srwi.pycharmpixelglance.data

class CachedProcessingPipeline (private val data: DisplayableData) {
    private var needsUpdate = true
    private var lastData: DisplayableData = data

    var normalizeEnabled: Boolean = false
        get() = field
        set(value) {
            field = value
            needsUpdate = true
        }

    var transposeEnabled: Boolean = false
        get() = field
        set(value) {
            field = value
            needsUpdate = true
        }

    var reverseChannelsEnabled: Boolean = false
        get() = field
        set(value) {
            field = value
            needsUpdate = true
        }

    var applyColormapEnabled: Boolean = false
        get() = field
        set(value) {
            field = value
            needsUpdate = true
        }

    fun apply(): DisplayableData {
        if (!needsUpdate) {
            return lastData
        }

        lastData = data
        if (normalizeEnabled) {
            lastData = lastData.normalize()
        }
        if (reverseChannelsEnabled) {
            lastData = lastData.reverseChannels()
        }
        if (transposeEnabled) {
            lastData = lastData.reverseChannels()
        }
        if (applyColormapEnabled) {
            lastData = lastData.applyColormap()
        }

        needsUpdate = false

        return lastData
    }
}