package com.github.srwi.pycharmpixelglance.data.modifications

import com.github.srwi.pycharmpixelglance.data.DisplayableData

class TransposeModification : ImageModification {
    override fun isApplicable(data: DisplayableData): Boolean = true

    override fun apply(data: DisplayableData): DisplayableData {
        // BCHW -> BHWC
        val newData = data.batch.transpose(0, 2, 3, 1)
        return DisplayableData(newData)
    }
}