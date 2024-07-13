package com.github.srwi.pycharmpixelglance.data

import org.jetbrains.kotlinx.multik.ndarray.data.DN
import org.jetbrains.kotlinx.multik.ndarray.data.NDArray
import org.jetbrains.kotlinx.multik.ndarray.operations.div
import org.jetbrains.kotlinx.multik.ndarray.operations.plus

class CustomImage(
    originalData: NDArray<Any, DN>,
    originalDataType: String
) {
    private var image: NDArray<Float, DN>

    init {
        image = preprocessArray(originalData, originalDataType)
    }

    private fun preprocessArray(array: NDArray<Any, DN>, dataType: String): NDArray<Float, DN> {
        val preprocessed = when (dataType) {
            "int8" -> {
                val floatArray = array.asType<Float>()
                (floatArray + 128f) / 255f
            }
            "uint8" -> {
                val floatArray = array.asType<Float>()
                floatArray / 255f
            }
            "uint16", "uint32", "uint64" -> {
                val doubleArray = array.asType<Double>()
                doubleArray / 256.0 / 255.0
            }
            "int16", "int32", "int64" -> {
                val doubleArray = array.asType<Double>()
                ((doubleArray / 256.0) + 128.0) / 255.0
            }
            "bool" -> {
                array.asType<Float>()
            }
            else -> throw IllegalArgumentException("Unsupported data type: $dataType")
        }

        return preprocessed.asType<Float>()
    }

//    fun getShape(): IntArray = shape
//
//    fun invert(): CustomImage {
//        val invertedImage = image.map { 1f - it } as NDArray<Any, DN>
//        return CustomImage(invertedImage)
//    }
//
//    fun extractChannel(channel: Int): CustomImage {
//        require(channel in 0 until channels) { "Invalid channel index" }
//        val extractedImage = if (channels == 1) {
//            image
//        } else {
//            image.slice(axes = listOf(0, 1), select = listOf(channel))
//        }
//        return CustomImage(extractedImage)
//    }
//
//    fun normalize(): CustomImage {
//        val min = image.min()
//        val max = image.max()
//        val range = max - min
//        val normalizedImage = image.map { (it - min) / range }
//        return CustomImage(normalizedImage)
//    }
//
//    fun reverseChannels(): CustomImage {
//        require(channels == 3) { "Reverse channels only applicable to 3-channel images" }
//        val reversedImage = mk.ndarray(image.toList().chunked(3) { it.reversed() }.flatten(), shape)
//        return CustomImage(reversedImage)
//    }
}