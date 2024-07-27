package com.github.srwi.pycharmpixelglance.data.modifications

import com.github.srwi.pycharmpixelglance.data.DisplayableData
import org.jetbrains.kotlinx.multik.ndarray.data.D3
import org.jetbrains.kotlinx.multik.ndarray.data.NDArray

class TransposeModification : ImageModification {
    override fun isApplicable(data: DisplayableData): Boolean =
        data.channels == 1 || data.channels == 3 || data.channels == 4

    override fun apply(data: DisplayableData): NDArray<Float, D3> =
        data.image.transpose(2, 0, 1)
}