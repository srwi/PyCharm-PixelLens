package com.github.srwi.pycharmpixelglance.data.modifications

import com.github.srwi.pycharmpixelglance.data.DisplayableData
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.zeros
import org.jetbrains.kotlinx.multik.ndarray.data.get
import org.jetbrains.kotlinx.multik.ndarray.data.set

class ReverseChannelsModification : ImageModification {
    override fun isApplicable(data: DisplayableData): Boolean =
        data.channels == 3 || data.channels == 4

    override fun apply(data: DisplayableData): DisplayableData {
        val reversedImage = mk.zeros<Float>(data.height, data.width, data.channels)
        for (i in 0 until data.height) {
            for (j in 0 until data.width) {
                reversedImage[i, j, 0] = data.image[i, j, 2]
                reversedImage[i, j, 1] = data.image[i, j, 1]
                reversedImage[i, j, 2] = data.image[i, j, 0]
                if (data.channels == 4) {
                    reversedImage[i, j, 3] = data.image[i, j, 3]
                }
            }
        }
        return DisplayableData(reversedImage)
    }
}