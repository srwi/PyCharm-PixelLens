package com.github.srwi.pycharmpixelglance.data.modifications

import com.github.srwi.pycharmpixelglance.data.DisplayableData

class TransposeModification : ImageModification {
    override fun isApplicable(data: DisplayableData): Boolean = true

    override fun apply(data: DisplayableData): DisplayableData {
        val newData = data.image.transpose(1, 2, 0)
        return DisplayableData(newData)
    }
}