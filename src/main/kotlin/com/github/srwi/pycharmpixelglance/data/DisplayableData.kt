package com.github.srwi.pycharmpixelglance.data

import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.zeros
import org.jetbrains.kotlinx.multik.ndarray.data.*
import org.jetbrains.kotlinx.multik.ndarray.operations.*
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import kotlin.math.pow

class DisplayableData private constructor(
    private var image: NDArray<Float, D3>
) {
    companion object {
        fun fromNDArray(originalData: NDArray<Any, DN>, originalDataType: String): DisplayableData {
            val adjusted = adjustValueRange(originalData, originalDataType)
            val reshaped = reshapeData(adjusted)
            return DisplayableData(reshaped)
        }

        private fun reshapeData(array: NDArray<Float, DN>): NDArray<Float, D3> {
            when (array.shape.size) {
                1, 2 -> {
                    val unsqueezed = array.unsqueeze(array.shape.size)
                    return reshapeData(unsqueezed)
                }
                3 -> {
                    return array as NDArray<Float, D3>
                }
                else -> {
                    if (array.shape[0] == 1) {
                        val squeezed = array.squeeze(0)
                        return reshapeData(squeezed)
                    }
                    else if (array.shape[array.shape.size - 1] == 1) {
                        val squeezed = array.squeeze(array.shape.size - 1)
                        return reshapeData(squeezed)
                    }
                }
            }

            throw Exception("Shape ${array.shape.contentToString()} not supported.")
        }

        private fun adjustValueRange(array: NDArray<Any, DN>, dataType: String): NDArray<Float, DN> {
            val preprocessed = when (dataType) {
                "int8" -> array.asType<Float>() + 128f
                "uint8", "RGBA", "RGB", "L", "P" -> array.asType<Float>()
                "uint16", "uint32", "uint64" -> array.asType<Double>() / 256.0
                "int16", "int32", "int64", "I" -> (array.asType<Double>() / 256.0) + 128.0
                "float16", "float32", "float64", "F" -> array.asType<Double>() * 255.0
                "bool" -> array.asType<Float>() * 255f
                else -> throw IllegalArgumentException("Unsupported data type: $dataType")
            }

            return preprocessed.asType<Float>().clip(0f, 255f)
        }
    }

    val height: Int get() = image.shape[0]

    val width: Int get() = image.shape[1]

    val channels: Int get() = image.shape[2]

    fun getValue(x: Int, y: Int): MultiArray<Float, D1> {
        return image[x, y]
    }

    fun getBuffer(): BufferedImage {
        val bufferedImage = when (channels) {
            1 -> BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY)
            3 -> BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR)
            4 -> BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR)
            else -> throw IllegalArgumentException("Unsupported number of channels: $channels")
        }

        val intImage = image.asType<Int>().clip(0, 255).flatten()

        when (channels) {
            1 -> {
                val buffer = (bufferedImage.raster.dataBuffer as DataBufferByte).data
                for (i in buffer.indices) {
                    buffer[i] = intImage[i].toByte()
                }
            }
            3 -> {
                val buffer = (bufferedImage.raster.dataBuffer as DataBufferByte).data
                for (i in 0 until height * width) {
                    buffer[i * 3] = intImage[i * 3 + 2].toByte()     // Blue
                    buffer[i * 3 + 1] = intImage[i * 3 + 1].toByte() // Green
                    buffer[i * 3 + 2] = intImage[i * 3].toByte()     // Red
                }
            }
            4 -> {
                val buffer = (bufferedImage.raster.dataBuffer as DataBufferByte).data
                for (i in 0 until height * width) {
                    buffer[i * 4] = intImage[i * 4 + 3].toByte()     // Alpha
                    buffer[i * 4 + 1] = intImage[i * 4 + 2].toByte() // Blue
                    buffer[i * 4 + 2] = intImage[i * 4 + 1].toByte() // Green
                    buffer[i * 4 + 3] = intImage[i * 4].toByte()     // Red
                }
            }
        }

        return bufferedImage
    }

    fun normalize(): DisplayableData {
        val min = image.min() ?: 0f
        val max = image.max() ?: 255f
        val normalizedImage = (image - min) / (max - min) * 255f
        return DisplayableData(normalizedImage)
    }

    val reverseChannelsPossible: Boolean
        get() = channels == 3 || channels == 4

    fun reverseChannels(): DisplayableData {
        if (!reverseChannelsPossible) {
            return this
        }

        val reversedImage = mk.zeros<Float>(image.shape[0], image.shape[1], image.shape[2])

        for (i in 0 until height) {
            for (j in 0 until width) {
                reversedImage[i, j, 0] = image[i, j, 2]
                reversedImage[i, j, 1] = image[i, j, 1]
                reversedImage[i, j, 2] = image[i, j, 0]
                if (channels == 4) {
                    reversedImage[i, j, 3] = image[i, j, 3]
                }
            }
        }

        return DisplayableData(reversedImage)
    }

    val transposePossible: Boolean
        get() = channels == 1 || channels == 3 || channels == 4

    fun transpose(): DisplayableData {
        if (!transposePossible) {
            return this
        }

        val transposedImage = image.transpose(2, 0, 1)
        return DisplayableData(transposedImage)
    }

    val applyColormapPossible: Boolean
        get() = channels == 1

    fun applyColormap(): DisplayableData {
        if (!applyColormapPossible) {
            return this
        }

        val grayscaleImage = image.squeeze(2) as NDArray<Float, D2>
        val coloredImage = mk.zeros<Float>(image.shape[0], image.shape[1], 3)

        for (i in 0 until height) {
            for (j in 0 until width) {
                val value = grayscaleImage[i, j] / 255f
                val (r, g, b) = viridisColor(value)
                coloredImage[i, j, 0] = r
                coloredImage[i, j, 1] = g
                coloredImage[i, j, 2] = b
            }
        }

        return DisplayableData(coloredImage)
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