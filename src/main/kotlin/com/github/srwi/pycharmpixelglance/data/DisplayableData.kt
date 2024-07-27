package com.github.srwi.pycharmpixelglance.data

import com.github.srwi.pycharmpixelglance.data.modifications.*
import org.jetbrains.kotlinx.multik.ndarray.data.*
import org.jetbrains.kotlinx.multik.ndarray.operations.*
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte

class DisplayableData private constructor(
    val image: NDArray<Float, D3>
) {
    companion object {
        fun fromNDArray(originalData: NDArray<Any, DN>, originalDataType: String): DisplayableData {
            val adjusted = adjustValueRange(originalData, originalDataType)
            val reshaped = reshapeData(adjusted)
            return DisplayableData(reshaped)
        }

        private fun reshapeData(array: NDArray<Float, DN>): NDArray<Float, D3> {
            // TODO: don't squeeze last dimensions as it is unlikely they will not have any meaning
            // TODO: reshape to always have batch and channel dimensions
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

    private fun applyModificationIfApplicable(modification: ImageModification): DisplayableData {
        if (!modification.isApplicable(this)) {
            return this
        }

        val modifiedImage = modification.apply(this)
        return DisplayableData(modifiedImage)
    }

    fun transpose() = applyModificationIfApplicable(TransposeModification())
    fun reverseChannels() = applyModificationIfApplicable(ReverseChannelsModification())
    fun normalize() = applyModificationIfApplicable(NormalizeModification())
    fun applyColormap() = applyModificationIfApplicable(ColormapModification())

    val transposeApplicable: Boolean get() = TransposeModification().isApplicable(this)
    val reverseChannelsApplicable: Boolean get() = ReverseChannelsModification().isApplicable(this)
    val normalizeApplicable: Boolean get() = NormalizeModification().isApplicable(this)
    val applyColormapApplicable: Boolean get() = ColormapModification().isApplicable(this)
}