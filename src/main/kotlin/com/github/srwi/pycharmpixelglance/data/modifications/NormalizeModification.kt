package com.github.srwi.pycharmpixelglance.data.modifications

import com.github.srwi.pycharmpixelglance.data.DisplayableData
import org.jetbrains.kotlinx.multik.ndarray.operations.*

class NormalizeModification : ImageModification {
    override fun isApplicable(data: DisplayableData): Boolean = true

    override fun apply(data: DisplayableData): DisplayableData {
        val min = data.batch.min() ?: 0f
        val max = data.batch.max() ?: 255f
        val normalized = (data.batch - min) / (max - min) * 255f
        return DisplayableData(normalized)
    }
}