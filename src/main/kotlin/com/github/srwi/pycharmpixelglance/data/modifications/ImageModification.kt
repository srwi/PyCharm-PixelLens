package com.github.srwi.pycharmpixelglance.data.modifications

import com.github.srwi.pycharmpixelglance.data.DisplayableData

interface ImageModification {
    fun isApplicable(data: DisplayableData): Boolean
    fun apply(data: DisplayableData): DisplayableData
}