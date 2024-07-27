package com.github.srwi.pycharmpixelglance.data.modifications

import com.github.srwi.pycharmpixelglance.data.DisplayableData
import org.jetbrains.kotlinx.multik.ndarray.data.D3
import org.jetbrains.kotlinx.multik.ndarray.data.NDArray
import org.jetbrains.kotlinx.multik.ndarray.operations.*

class NormalizeModification : ImageModification {
    override fun isApplicable(data: DisplayableData): Boolean = true

    override fun apply(data: DisplayableData): NDArray<Float, D3> {
        val min = data.image.min() ?: 0f
        val max = data.image.max() ?: 255f
        return (data.image - min) / (max - min) * 255f
    }
}