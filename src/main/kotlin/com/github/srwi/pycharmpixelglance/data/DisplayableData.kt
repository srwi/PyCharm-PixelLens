package com.github.srwi.pycharmpixelglance.data

import org.jetbrains.kotlinx.multik.ndarray.data.*
import org.jetbrains.kotlinx.multik.ndarray.operations.*
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte

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
        val height = image.shape[0]
        val width = image.shape[1]
        val channels = image.shape[2]

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

    fun invert(): DisplayableData {
        val invertedImage = 255f - image
        return DisplayableData(invertedImage)
    }

//    fun extractChannel(channel: Int): DisplayableData {
//        require(channel in 0 until image.shape[2]) { "Invalid channel index" }
//        val extractedImage: NDArray<Float, D3> = image.slice(2..2, channel).unsqueeze(2)
//        return DisplayableData(extractedImage)
//    }

    fun normalize(): DisplayableData {
        val min = image.min() ?: 0f
        val max = image.max() ?: 255f
        val normalizedImage = (image - min) / (max - min) * 255f
        return DisplayableData(normalizedImage)
    }

//    fun reverseChannels(): DisplayableData {
//        val reversedImage = image.slice(2, (image.shape[2] - 1 downTo 0).toList())
//        return DisplayableData(reversedImage)
//    }
}