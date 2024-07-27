package com.github.srwi.pycharmpixelglance.data.modifications

import com.github.srwi.pycharmpixelglance.data.DisplayableData
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.zeros
import org.jetbrains.kotlinx.multik.ndarray.data.*
import kotlin.math.pow

class ColormapModification : ImageModification {
    override fun isApplicable(data: DisplayableData): Boolean =
        data.channels == 1

    override fun apply(data: DisplayableData): NDArray<Float, D3> {
        val grayscaleImage = data.image.squeeze(2) as NDArray<Float, D2>
        val coloredImage = mk.zeros<Float>(data.height, data.width, 3)

        for (i in 0 until data.height) {
            for (j in 0 until data.width) {
                val value = grayscaleImage[i, j] / 255f
                val (r, g, b) = viridisColor(value)
                coloredImage[i, j, 0] = r
                coloredImage[i, j, 1] = g
                coloredImage[i, j, 2] = b
            }
        }

        return coloredImage
    }

    private fun viridisColor(value: Float): Triple<Float, Float, Float> {
        // Coefficients for the Viridis colormap approximation
        val c0 = listOf(0.2777273272234177, 0.005407344544966578, 0.3340998053353061)
        val c1 = listOf(0.1050930431085774, 1.404613529898575, 1.384590162594685)
        val c2 = listOf(-0.3308618287255563, 0.214847559468213, 0.09509516302823659)
        val c3 = listOf(-4.634230498983486, -5.799100973351585, -19.33244095627987)
        val c4 = listOf(6.228269936347081, 14.17993336680509, 56.69055260068105)
        val c5 = listOf(4.776384997670288, -13.74514537774601, -65.35303263337234)
        val c6 = listOf(-5.435455855934631, 4.645852612178535, 26.3124352495832)

        val v = value.coerceIn(0f, 1f)

        fun channel(i: Int): Float {
            return (c0[i] + v * (c1[i] + v * (c2[i] + v * (c3[i] + v * (c4[i] + v * (c5[i] + v * c6[i])))))).toFloat()
        }

        return Triple(
            channel(0).pow(2).times(255).coerceIn(0f, 255f),
            channel(1).pow(2).times(255).coerceIn(0f, 255f),
            channel(2).pow(2).times(255).coerceIn(0f, 255f)
        )
    }
}