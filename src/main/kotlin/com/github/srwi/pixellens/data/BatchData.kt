package com.github.srwi.pixellens.data

import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.zeros
import org.jetbrains.kotlinx.multik.ndarray.data.*
import org.jetbrains.kotlinx.multik.ndarray.operations.*
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import kotlin.math.pow

class BatchData (
    originalData: NDArray<Any, DN>, private val originalDataType: String
) {
    private var data: NDArray<Any, D4>
    private var unsqueezedLastDim: Boolean = false

    init {
        val reshaped = reshapeData(originalData)
        data = reshaped
    }

    var channelsFirst: Boolean = false
        set(value) {
            if (value != field) {
                data = if (value) {
                    data.transpose(0, 3, 1, 2)
                } else {
                    data.transpose(0, 2, 3, 1)
                }
                field = value
            }
        }

    var normalized: Boolean = false

    var grayscaleColormap: Boolean = false

    var reversedChannels: Boolean = false

    val batchSize: Int get() = data.shape[0]

    val height: Int get() = data.shape[1]

    val width: Int get() = data.shape[2]

    val channels: Int get() = data.shape[3]

    private fun reshapeData(array: NDArray<Any, DN>): NDArray<Any, D4> {
        when (array.shape.size) {
            1, 2 -> {
                // Create width or channel dimension
                val unsqueezed = array.unsqueeze(array.shape.size)
                unsqueezedLastDim = true
                return reshapeData(unsqueezed)
            }
            3 -> {
                // Create batch dimension
                val unsqueezed = array.unsqueeze(0)
                return reshapeData(unsqueezed)
            }
            4 -> {
                return array as NDArray<Any, D4>
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

    private fun adjustValueRange(array: NDArray<Any, D3>, dataType: String): NDArray<Float, D3> {
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

    fun supportsMultiChannelDisplay(): Boolean {
        return channels == 1 || channels == 3 || channels == 4
    }

    fun getValue(batchIndex: Int, x: Int, y: Int, channel: Int?) : Any {
        require(x in 0 until width) { "Invalid x coordinate: $x" }
        require(y in 0 until height) { "Invalid y coordinate: $y" }

        val imageData = data[batchIndex] as NDArray<Any, D3>

        if (channel == null) {
            return if (unsqueezedLastDim) {
                imageData[y, x, 0]
            } else {
                imageData[y, x]
            }
        } else {
            val adjustedChannel = if (reversedChannels) channels - channel - 1 else channel
            return imageData[y, x, adjustedChannel]
        }
    }

    fun getImage(batchIndex: Int = 0, channel: Int? = null): BufferedImage {
        require(batchIndex in 0 until batchSize) { "Invalid batch index: $batchIndex" }
        require(channel in 0 until channels || channel == null) { "Invalid channel index: $channel" }

        val imageData = data[batchIndex] as NDArray<Any, D3>

        val channelData = if (channel == null) {
            imageData.deepCopy()
        } else {
            val adjustedChannel = if (reversedChannels) channels - channel - 1 else channel
            // unsqueeze will create a copy
            imageData[0 until imageData.shape[0], 0 until imageData.shape[1], adjustedChannel].unsqueeze(2)
        } as NDArray<Any, D3>

        var rescaledChannelData = adjustValueRange(channelData, originalDataType)

        if (normalized) {
            rescaledChannelData = normalizeData(rescaledChannelData)
        }
        if (grayscaleColormap && rescaledChannelData.shape[2] == 1) {
            rescaledChannelData = applyColormap(rescaledChannelData)
        }

        return getBufferedImage(rescaledChannelData)
    }

    private fun normalizeData(someData: NDArray<Float, D3>): NDArray<Float, D3> {
        val min = someData.min() ?: 0f
        val max = someData.max() ?: 255f
        return (someData - min) / (max - min) * 255f
    }

    private fun applyColormap(someData: NDArray<Float, D3>): NDArray<Float, D3> {
        require(someData.shape[2] == 1) { "Data must have only one channel for colormap." }

        val (redOffset, greenOffset, blueOffset) = channelIndexOffsets()
        val coloredImage = mk.zeros<Float>(height, width, 3)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val value = someData[y, x, 0] / 255f
                val (red, green, blue) = viridisColor(value)
                coloredImage[y, x, redOffset] = red
                coloredImage[y, x, greenOffset] = green
                coloredImage[y, x, blueOffset] = blue
            }
        }

        return coloredImage
    }

    private fun getBufferedImage(someData: NDArray<Float, D3>): BufferedImage {
        val bufferChannels = someData.shape[2]
        val bufferedImage = when (bufferChannels) {
            1 -> BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY)
            3 -> BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR)
            4 -> BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR)
            else -> throw Exception("Unsupported number of channels: $channels")
        }

        val byteData = someData.clip(0f, 255f).asType<Byte>().flatten()
        val buffer = (bufferedImage.raster.dataBuffer as DataBufferByte).data
        val (redOffset, greenOffset, blueOffset) = channelIndexOffsets()
        when (bufferChannels) {
            1 -> {
                for (i in buffer.indices) {
                    buffer[i] = byteData[i]
                }
            }

            3 -> {
                for (i in 0 until height * width) {
                    buffer[i * 3] = byteData[i * 3 + blueOffset]
                    buffer[i * 3 + 1] = byteData[i * 3 + greenOffset]
                    buffer[i * 3 + 2] = byteData[i * 3 + redOffset]
                }
            }

            4 -> {
                for (i in 0 until height * width) {
                    buffer[i * 4] = byteData[i * 4 + 3]  // alpha
                    buffer[i * 4 + 1] = byteData[i * 4 + blueOffset]
                    buffer[i * 4 + 2] = byteData[i * 4 + greenOffset]
                    buffer[i * 4 + 3] = byteData[i * 4 + redOffset]
                }
            }
        }
        return bufferedImage
    }

    private fun channelIndexOffsets() = when (reversedChannels) {
        true -> Triple(2, 1, 0)
        false -> Triple(0, 1, 2)
    }

    // TODO: Convert to lookup table
    private fun viridisColor(value: Float): Triple<Float, Float, Float> {
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