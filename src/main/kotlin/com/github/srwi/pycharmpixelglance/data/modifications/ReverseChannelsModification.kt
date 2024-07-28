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
        val reversedImage = mk.zeros<Float>(data.batchSize, data.height, data.width, data.channels)
        for (b in 0 until data.batchSize) {
            for (y in 0 until data.height) {
                for (x in 0 until data.width) {
                    reversedImage[b, y, x, 0] = data.batch[b, y, x, 2]
                    reversedImage[b, y, x, 1] = data.batch[b, y, x, 1]
                    reversedImage[b, y, x, 2] = data.batch[b, y, x, 0]
                    if (data.channels == 4) {
                        reversedImage[b, y, x, 3] = data.batch[b, y, x, 3]
                    }
                }
            }
        }
        return DisplayableData(reversedImage)
    }
}