package com.github.srwi.pixellens.imageProviders

import com.github.srwi.pixellens.data.BatchData
import com.github.srwi.pixellens.interop.Python
import com.intellij.openapi.progress.ProgressIndicator
import com.jetbrains.python.debugger.PyDebugValue
import com.jetbrains.python.debugger.PyFrameAccessor
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ndarray
import org.jetbrains.kotlinx.multik.api.zeros
import org.jetbrains.kotlinx.multik.ndarray.data.*
import org.jetbrains.kotlinx.multik.ndarray.operations.div
import org.jetbrains.kotlinx.multik.ndarray.operations.plus
import org.jetbrains.kotlinx.multik.ndarray.operations.times
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

@Serializable
data class ImageAndMetadata(val bytes: ByteArray, val metadata: Metadata)

@Serializable
data class ImageBase64AndMetadata(val data: String, val metadata: Metadata)

@Serializable
data class Metadata(val name: String, val shape: List<Int>, val dtype: String)

data class Batch(val name: String, val data: BatchData, val metadata: Metadata)

abstract class ImageProvider {

    fun getDataByVariableName(frameAccessor: PyFrameAccessor, progressIndicator: ProgressIndicator, name: String): Batch {
        val imageAndMetadata = getImageAndMetadata(progressIndicator, frameAccessor, name)
        return processImageData(progressIndicator, imageAndMetadata)
    }

    private fun getImageAndMetadata(progressIndicator: ProgressIndicator, frameAccessor: PyFrameAccessor, variableName: String): ImageAndMetadata {
        val jsonDataVariableName = prepareData(frameAccessor, variableName)
        val jsonData = SocketDataTransmitter().getJsonData(frameAccessor, progressIndicator, jsonDataVariableName)
        cleanUpData(frameAccessor, jsonDataVariableName)

        val imageDataWithMetadata = Json.decodeFromString<ImageBase64AndMetadata>(jsonData)
        val imageBytes = Base64.getDecoder().decode(imageDataWithMetadata.data)

        return ImageAndMetadata(imageBytes, imageDataWithMetadata.metadata)
    }

    private fun prepareData(frameAccessor: PyFrameAccessor, variableName: String): String {
        val jsonDataVariableName = "__tmp_json_data"
        val command = getDataPreparationCommand(variableName, jsonDataVariableName)
        Python.executeStatement(frameAccessor, command)
        return jsonDataVariableName
    }

    private fun cleanUpData(frameAccessor: PyFrameAccessor, variableName: String) {
        Python.executeStatement(frameAccessor, "del $variableName")
    }

    private fun processImageData(progressIndicator: ProgressIndicator, imageAndMetadata: ImageAndMetadata): Batch {
        progressIndicator.text = "Processing image data..."
        progressIndicator.fraction = 0.0

        val metadata = imageAndMetadata.metadata
        val bytes = imageAndMetadata.bytes
        val shape = metadata.shape.toIntArray()
        val dtype = metadata.dtype
        val array: NDArray<*, D1> = convertBytesToNDArray(bytes, dtype)

        progressIndicator.fraction = 0.33

        val reshapedArray: NDArray<Any, DN> = reshapeArray(array, shape)

        progressIndicator.fraction = 0.67

        val rescaledArray: NDArray<Float, DN> = rescaleValues(reshapedArray, dtype)

        progressIndicator.fraction = 1.0

        return Batch(metadata.name, BatchData(reshapedArray, rescaledArray), metadata)
    }

    private fun reshapeArray(
        multikArray: NDArray<*, D1>,
        shape: IntArray
    ): NDArray<Any, DN> {
        // Unfortunately multik forces us to handle common numbers of dimensions explicitly
        @Suppress("UNCHECKED_CAST") val reshapedArray: NDArray<Any, DN> = when (shape.size) {
            1 -> multikArray.reshape(shape[0])
            2 -> multikArray.reshape(shape[0], shape[1])
            3 -> multikArray.reshape(shape[0], shape[1], shape[2])
            4 -> multikArray.reshape(shape[0], shape[1], shape[2], shape[3])
            else -> multikArray.reshape(shape[0], shape[1], shape[2], shape[3], *shape.slice(4 until shape.size).toIntArray())
        } as NDArray<Any, DN>
        return reshapedArray
    }

    private fun rescaleValues(array: NDArray<Any, DN>, dataType: String): NDArray<Float, DN> {
        val rescaled = when (dataType) {
            "int8" -> array.asType<Float>() + 128f
            "uint8", "RGBA", "RGB", "L", "P" -> array.asType<Float>()
            "uint16", "uint32" -> array.asType<Float>() / 256f
            "uint64" -> array.asType<Double>() / 256.0
            "int16", "int32" -> (array.asType<Float>() / 256f) + 128f
            "int64", "I" -> (array.asType<Double>() / 256.0) + 128.0
            "float16", "float32" -> array.asType<Float>() * 255f
            "float64", "F" -> array.asType<Double>() * 255.0
            "bool" -> array.asType<Float>() * 255f
            else -> throw IllegalArgumentException("Unsupported data type: $dataType")
        }

        val rescaledFloat = if (rescaled.dtype == DataType.FloatDataType) {
            rescaled as NDArray<Float, DN>
        } else {
            rescaled.asType<Float>()
        }

        return rescaledFloat
    }

    private fun convertBytesToNDArray(bytes: ByteArray, dtype: String): NDArray<*, D1> {
        val imageBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        return when (dtype) {
            "float16" -> {
                val size = bytes.size / 2
                val array = ShortArray(size)
                imageBuffer.asShortBuffer().get(array)
                mk.zeros<Float>(size).apply {
                    for (i in array.indices) {
                        this[i] = Float16.fromBits(array[i])
                    }
                }
            }
            "float32", "F" -> {
                val array = FloatArray(bytes.size / 4)
                imageBuffer.asFloatBuffer().get(array)
                mk.ndarray(array)
            }
            "float64" -> {
                val array = DoubleArray(bytes.size / 8)
                imageBuffer.asDoubleBuffer().get(array)
                mk.ndarray(array)
            }
            "int8" -> {
                val array = ByteArray(bytes.size)
                imageBuffer.get(array)
                mk.ndarray(array)
            }
            "int16" -> {
                val array = ShortArray(bytes.size / 2)
                imageBuffer.asShortBuffer().get(array)
                mk.ndarray(array)
            }
            "int32", "I" -> {
                val array = IntArray(bytes.size / 4)
                imageBuffer.asIntBuffer().get(array)
                mk.ndarray(array)
            }
            "int64" -> {
                val array = LongArray(bytes.size / 8)
                imageBuffer.asLongBuffer().get(array)
                mk.ndarray(array)
            }
            "uint8", "RGBA", "RGB", "L", "P" -> {
                val array = ByteArray(bytes.size)
                imageBuffer.get(array)
                mk.zeros<Short>(bytes.size).apply {
                    for (i in array.indices) {
                        this[i] = array[i].toUByte().toShort()
                    }
                }
            }
            "uint16" -> {
                val size = bytes.size / 2
                val array = ShortArray(size)
                imageBuffer.asShortBuffer().get(array)
                mk.zeros<Int>(size).apply {
                    for (i in array.indices) {
                        this[i] = array[i].toUShort().toInt()
                    }
                }
            }
            "uint32" -> {
                val size = bytes.size / 4
                val array = IntArray(size)
                imageBuffer.asIntBuffer().get(array)
                mk.zeros<Long>(size).apply {
                    for (i in array.indices) {
                        this[i] = array[i].toUInt().toLong()
                    }
                }
            }
            "uint64" -> {
                val size = bytes.size / 8
                val array = LongArray(size)
                imageBuffer.asLongBuffer().get(array)
                mk.zeros<Double>(size).apply {
                    for (i in array.indices) {
                        this[i] = array[i].toULong().toDouble()
                    }
                }
            }
            "bool" -> {
                val array = ByteArray(bytes.size)
                imageBuffer.get(array)
                mk.ndarray(array)
            }
            "1", "CMYK", "YCbCr", "LAB", "HSV" -> {
                throw NotImplementedError("PIL data type $dtype not implemented yet.")
            }
            else -> throw IllegalArgumentException("Unsupported data type: $dtype")
        }
    }

    private fun convertStringToShapeList(shapeString: String): List<Int> {
        return Regex("\\d+").findAll(shapeString).map { it.value.toInt() }.toList()
    }

    object Float16 {
        fun fromBits(someBits: Short): Float {
            val s = (someBits.toInt() shr 15) and 0x1
            var e = (someBits.toInt() shr 10) and 0x1f
            var m = someBits.toInt() and 0x3ff

            if (e == 0x1f) {
                return if (m == 0) {
                    if (s == 1) Float.NEGATIVE_INFINITY else Float.POSITIVE_INFINITY
                } else {
                    Float.NaN
                }
            }

            if (e == 0) {
                if (m == 0) return if (s == 1) -0.0f else 0.0f
                while (m and 0x400 == 0) {
                    m = m shl 1
                    e -= 1
                }
                e += 1
                m = m and 0x3ff
            }

            val exp = e + 112
            val sig = m shl 13
            val bits = (s shl 31) or (exp shl 23) or sig
            return Float.fromBits(bits)
        }
    }

    open fun shapeSupported(value: PyDebugValue): Boolean {
        val variableName = value.name
        val shape = Python.evaluateExpression(value.frameAccessor, "$variableName.shape").value ?: return false
        val shapeList = convertStringToShapeList(shape)
        if (shapeList.size <= 4) return true
        for (i in 0 until (shapeList.size - 4)) {
            // Only dimensions of size 1 can be squeezed
            if (shapeList[i] != 1) return false
        }
        return true
    }

    abstract fun typeSupported(value: PyDebugValue): Boolean

    abstract fun getDataPreparationCommand(variableName: String, outputVariableName: String): String
}
