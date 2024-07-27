package com.github.srwi.pycharmpixelglance.data.modifications

import com.github.srwi.pycharmpixelglance.data.DisplayableData
import org.jetbrains.kotlinx.multik.ndarray.data.D3
import org.jetbrains.kotlinx.multik.ndarray.data.NDArray

interface ImageModification {
    fun isApplicable(data: DisplayableData): Boolean
    fun apply(data: DisplayableData): NDArray<Float, D3>
}