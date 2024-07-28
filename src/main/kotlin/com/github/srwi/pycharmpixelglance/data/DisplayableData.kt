package com.github.srwi.pycharmpixelglance.data

import com.github.srwi.pycharmpixelglance.data.modifications.*
import org.jetbrains.kotlinx.multik.ndarray.data.*
import org.jetbrains.kotlinx.multik.ndarray.operations.clip
import org.jetbrains.kotlinx.multik.ndarray.operations.div
import org.jetbrains.kotlinx.multik.ndarray.operations.plus
import org.jetbrains.kotlinx.multik.ndarray.operations.times
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte

class DisplayableData (
    val batch: NDArray<Float, D4>
) {
    companion object {
        fun fromNDArray(originalData: NDArray<Any, DN>, originalDataType: String): DisplayableData {
            val adjusted = adjustValueRange(originalData, originalDataType)
            val reshaped = reshapeData(adjusted)
            return DisplayableData(reshaped)
        }

        private fun reshapeData(array: NDArray<Float, DN>): NDArray<Float, D4> {
            when (array.shape.size) {
                1, 2 -> {
                    // Create width or channel dimension
                    val unsqueezed = array.unsqueeze(array.shape.size)
                    return reshapeData(unsqueezed)
                }
                3 -> {
                    // Create batch dimension
                    val unsqueezed = array.unsqueeze(0)
                    return reshapeData(unsqueezed)
                }
                4 -> {
                    return array as NDArray<Float, D4>
                }
                else -> {
                    // Remove empty first dimension
                    if (array.shape[0] == 1) {
                        val squeezed = array.squeeze(0)
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

    val batchSize: Int get() = batch.shape[0]

    val height: Int get() = batch.shape[1]

    val width: Int get() = batch.shape[2]

    val channels: Int get() = batch.shape[3]

    fun getBuffer(batchIndex: Int = 0, channel: Int? = null): BufferedImage {
        require(batchIndex in 0 until batchSize) { "Invalid batch index: $batchIndex" }
        require(channel in 0 until channels || channel == null) { "Invalid channel index: $channel" }

        val imageData = batch[batchIndex] as NDArray<Float, D3>

        val channelData = if (channel == null) {
            imageData.deepCopy()
        } else {
            // unsqueeze will create a copy
            imageData[0 until imageData.shape[0], 0 until imageData.shape[1], channel].unsqueeze(2)
        } as NDArray<Float, D3>

        val bufferChannels = channelData.shape[2]
        val bufferedImage = when (bufferChannels) {
            1 -> BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY)
            3 -> BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR)
            4 -> BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR)
            else -> throw Exception("Unsupported number of channels: $channels")
        }

        val intImage = channelData.clip(0f, 255f).asType<Byte>().flatten()
        val buffer = (bufferedImage.raster.dataBuffer as DataBufferByte).data
        when (bufferChannels) {
            1 -> {
                for (i in buffer.indices) {
                    buffer[i] = intImage[i]
                }
            }
            3 -> {
                for (i in 0 until height * width) {
                    buffer[i * 3] = intImage[i * 3 + 2]     // Blue
                    buffer[i * 3 + 1] = intImage[i * 3 + 1] // Green
                    buffer[i * 3 + 2] = intImage[i * 3]     // Red
                }
            }
            4 -> {
                for (i in 0 until height * width) {
                    buffer[i * 4] = intImage[i * 4 + 3]     // Alpha
                    buffer[i * 4 + 1] = intImage[i * 4 + 2] // Blue
                    buffer[i * 4 + 2] = intImage[i * 4 + 1] // Green
                    buffer[i * 4 + 3] = intImage[i * 4]     // Red
                }
            }
        }

        return bufferedImage
    }

    private fun applyModificationIfApplicable(modification: ImageModification): DisplayableData {
        if (modification.isApplicable(this)) {
            return modification.apply(this)
        }

        return this
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