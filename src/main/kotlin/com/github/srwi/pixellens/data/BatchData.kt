package com.github.srwi.pixellens.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.zeros
import org.jetbrains.kotlinx.multik.ndarray.data.*
import org.jetbrains.kotlinx.multik.ndarray.operations.*
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte


class RenderedImage(
    val image: BufferedImage,
    val valuesClipped: Boolean,
)

class BatchData (
    originalData: NDArray<Any, DN>, rescaledData: NDArray<Float, DN>
) {
    private var originalData: NDArray<Any, D4>
    private var rescaledData: NDArray<Float, D4>
    private var unsqueezedLastDim: Boolean = false

    init {
        this.originalData = expandDimensions(originalData) as NDArray<Any, D4>
        this.rescaledData = expandDimensions(rescaledData) as NDArray<Float, D4>
    }

    var channelsFirst: Boolean = false
        set(value) {
            if (value != field) {
                rescaledData = if (value) {
                    rescaledData.transpose(0, 2, 3, 1)
                } else {
                    rescaledData.transpose(0, 3, 1, 2)
                }
                originalData = if (value) {
                    originalData.transpose(0, 2, 3, 1)
                } else {
                    originalData.transpose(0, 3, 1, 2)
                }
                field = value
            }
        }

    var normalized: Boolean = false

    var grayscaleColormap: Boolean = false

    var reversedChannels: Boolean = false

    val batchSize: Int get() = rescaledData.shape[0]

    val height: Int get() = rescaledData.shape[1]

    val width: Int get() = rescaledData.shape[2]

    val channels: Int get() = rescaledData.shape[3]

    private fun expandDimensions(array: NDArray<*, DN>): NDArray<*, D4> {
        when (array.shape.size) {
            1, 2 -> {
                // Create width or channel dimension
                val unsqueezed = array.unsqueeze(array.shape.size)
                unsqueezedLastDim = true
                return expandDimensions(unsqueezed)
            }
            3 -> {
                // Create batch dimension
                val unsqueezed = array.unsqueeze(0)
                return expandDimensions(unsqueezed)
            }
            4 -> {
                return array as NDArray<Any, D4>
            }
            else -> {
                // Remove empty first dimension
                if (array.shape[0] == 1) {
                    val squeezed = array.squeeze(0)
                    return expandDimensions(squeezed)
                }
            }
        }

        throw Exception("Shape ${array.shape.contentToString()} not supported.")
    }

    fun supportsMultiChannelDisplay(): Boolean {
        return channels == 1 || channels == 3 || channels == 4
    }

    fun getValue(batchIndex: Int, x: Int, y: Int, channel: Int?) : Any {
        require(x in 0 until width) { "Invalid x coordinate: $x" }
        require(y in 0 until height) { "Invalid y coordinate: $y" }

        val imageData = originalData[batchIndex] as NDArray<Any, D3>

        if (channel == null) {
            return if (unsqueezedLastDim) {
                imageData[y, x, 0]
            } else {
                imageData[y, x]
            }
        } else {
            return imageData[y, x, adjustChannelOrder(channel, numChannels = channels)]
        }
    }

    suspend fun getImage(batchIndex: Int = 0, channel: Int? = null): RenderedImage = withContext(Dispatchers.Default) {
        require(batchIndex in 0 until batchSize) { "Invalid batch index: $batchIndex" }
        require(channel in 0 until channels || channel == null) { "Invalid channel index: $channel" }

        val imageData = rescaledData[batchIndex] as NDArray<Float, D3>

        var channelData = if (channel == null) {
            imageData
        } else {
            val adjustedChannel = adjustChannelOrder(channel, numChannels = channels)
            imageData[0 until imageData.shape[0], 0 until imageData.shape[1], adjustedChannel until (adjustedChannel + 1)]
        } as NDArray<Float, D3>

        var valuesClipped = false
        if (normalized) {
            channelData = normalizeData(channelData)
            ensureActive()
        } else {
            val clippedData = clipValues(channelData, 0f, 255f)
            channelData = clippedData.first
            valuesClipped = clippedData.second
            ensureActive()
        }
        if (grayscaleColormap && channelData.shape[2] == 1) {
            channelData = applyColormap(channelData)
            ensureActive()
        }

        RenderedImage(getBufferedImage(channelData), valuesClipped)
    }

    private fun normalizeData(someData: NDArray<Float, D3>): NDArray<Float, D3> {
        // TODO: Cache min and max values as it can be quite slow to calculate
        val min = someData.min() ?: 0f
        val max = someData.max() ?: 255f
        val scaleFactor = 255f / (max - min)
        return (someData - min) * scaleFactor
    }

    private fun clipValues(array: NDArray<Float, D3>, min: Float, max: Float): Pair<NDArray<Float, D3>, Boolean> {
        val clippedArray = array.deepCopy()
        var valuesClipped = false
        for (i in 0 until clippedArray.shape[0]) {
            for (j in 0 until clippedArray.shape[1]) {
                for (k in 0 until clippedArray.shape[2]) {
                    val value = clippedArray[i, j, k]
                    if (value < min) {
                        clippedArray[i, j, k] = min
                        valuesClipped = true
                    } else if (value > max) {
                        clippedArray[i, j, k] = max
                        valuesClipped = true
                    }
                }
            }
        }
        return Pair(clippedArray, valuesClipped)
    }

    private fun applyColormap(someData: NDArray<Float, D3>): NDArray<Float, D3> {
        require(someData.shape[2] == 1) { "Data must have only one channel for colormap." }

        val coloredImage = mk.zeros<Float>(height, width, 3)
        val redOffset = adjustChannelOrder(0, numChannels = 3)
        val blueOffset = adjustChannelOrder(2, numChannels = 3)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val (red, green, blue) = Colormap.viridis[someData[y, x, 0].toInt()]
                coloredImage[y, x, redOffset] = red
                coloredImage[y, x, 1] = green
                coloredImage[y, x, blueOffset] = blue
            }
        }

        return coloredImage
    }

    private suspend fun getBufferedImage(someData: NDArray<Float, D3>): BufferedImage = withContext(Dispatchers.Default) {
        val bufferChannels = someData.shape[2]
        val bufferedImage = when (bufferChannels) {
            1 -> BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY)
            3 -> BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR)
            4 -> BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR)
            else -> throw Exception("Unsupported number of channels: $channels")
        }

        val buffer = (bufferedImage.raster.dataBuffer as DataBufferByte).data
        for (y in 0 until height) {
            val yOffset = y * width
            for (x in 0 until width) {
                val baseOffset = (yOffset + x) * bufferChannels
                for (c in 0 until bufferChannels) {
                    val adjustedChannel = adjustChannelOrder(bufferChannels - 1 - c, bufferChannels)
                    buffer[baseOffset + c] = someData[y, x, adjustedChannel].toInt().toByte()
                }
            }
            ensureActive()
        }

        bufferedImage
    }

    private fun adjustChannelOrder(channel: Int, numChannels: Int): Int {
        if (!reversedChannels || (numChannels != 3 && numChannels != 4))
            return channel

        return when (channel) {
            0, 2 -> 2 - channel
            else -> channel
        }
    }
}