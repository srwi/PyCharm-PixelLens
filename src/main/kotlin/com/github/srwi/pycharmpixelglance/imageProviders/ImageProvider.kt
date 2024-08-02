package com.github.srwi.pycharmpixelglance.imageProviders

import com.github.srwi.pycharmpixelglance.data.BatchData
import com.jetbrains.python.debugger.PyFrameAccessor
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ndarray
import org.jetbrains.kotlinx.multik.ndarray.data.D1
import org.jetbrains.kotlinx.multik.ndarray.data.DN
import org.jetbrains.kotlinx.multik.ndarray.data.NDArray
import java.nio.ByteBuffer
import java.util.*


@Serializable
data class Metadata(val shape: List<Int>, val dtype: String)

@Serializable
data class Payload(val name: String, val data: String, val metadata: Metadata)

data class Batch(val name: String, val data: BatchData, val metadata: Metadata)

abstract class ImageProvider {
    fun getDataByVariableName(frameAccessor: PyFrameAccessor, name: String) : Batch {
        val payload = getPayload(frameAccessor, name)
        val batchData = processImageData(payload)
        return batchData
    }

    fun deserializeJsonPayload(jsonPayloadString: String): Payload {
        val json = Json { ignoreUnknownKeys = true }
        return json.decodeFromString<Payload>(jsonPayloadString)
    }

    private fun processImageData(payload: Payload): Batch {
        val imageBase64 = payload.data
        val shape = payload.metadata.shape.toIntArray()
        val dtype = payload.metadata.dtype

        val imageBytes = Base64.getDecoder().decode(imageBase64)
        val imageBuffer = ByteBuffer.wrap(imageBytes).order(java.nio.ByteOrder.LITTLE_ENDIAN)

        val multikArray: NDArray<*, D1> = when (dtype) {
            "float16" -> {
                val array = ShortArray(imageBytes.size / 2)
                imageBuffer.asShortBuffer().get(array)
                val floatArray = array.map { Float16.fromBits(it) }.toFloatArray()
                mk.ndarray(floatArray)
            }
            "float32", "F" -> {
                val array = FloatArray(imageBytes.size / 4)
                imageBuffer.asFloatBuffer().get(array)
                mk.ndarray(array)
            }
            "float64" -> {
                val array = DoubleArray(imageBytes.size / 8)
                imageBuffer.asDoubleBuffer().get(array)
                mk.ndarray(array)
            }
            "int8" -> {
                val array = ByteArray(imageBytes.size)
                imageBuffer.get(array)
                mk.ndarray(array)
            }
            "int16" -> {
                val array = ShortArray(imageBytes.size / 2)
                imageBuffer.asShortBuffer().get(array)
                mk.ndarray(array)
            }
            "int32", "I" -> {
                val array = IntArray(imageBytes.size / 4)
                imageBuffer.asIntBuffer().get(array)
                mk.ndarray(array)
            }
            "int64" -> {
                val array = LongArray(imageBytes.size / 8)
                imageBuffer.asLongBuffer().get(array)
                mk.ndarray(array)
            }
            "uint8", "RGBA", "RGB", "L", "P" -> {
                val array = ByteArray(imageBytes.size)
                imageBuffer.get(array)
                val shortArray = array.map { it.toUByte().toShort() }
                mk.ndarray(shortArray)
            }
            "uint16" -> {
                val array = ShortArray(imageBytes.size / 2)
                imageBuffer.asShortBuffer().get(array)
                val intArray = array.map { it.toUShort().toInt() }
                mk.ndarray(intArray)
            }
            "uint32" -> {
                val array = IntArray(imageBytes.size / 4)
                imageBuffer.asIntBuffer().get(array)
                val longArray = array.map { it.toUInt().toLong() }
                mk.ndarray(longArray)
            }
            "uint64" -> {
                val array = LongArray(imageBytes.size / 8)
                imageBuffer.asLongBuffer().get(array)
                val doubleArray = array.map { it.toULong().toDouble() }
                mk.ndarray(doubleArray)
            }
            "bool" -> {
                val array = ByteArray(imageBytes.size)
                imageBuffer.get(array)
                mk.ndarray(array)
            }
            "1", "CMYK", "YCbCr", "LAB", "HSV" -> {
                throw NotImplementedError("PIL data type $dtype not implemented yet.")
            }
            else -> throw IllegalArgumentException("Unsupported data type: $dtype")
        }

        // Unfortunately multik forces us to handle common numbers of dimensions explicitly
        @Suppress("UNCHECKED_CAST") val reshapedArray: NDArray<Any, DN> = when (shape.size) {
            1 -> multikArray.reshape(shape[0])
            2 -> multikArray.reshape(shape[0], shape[1])
            3 -> multikArray.reshape(shape[0], shape[1], shape[2])
            4 -> multikArray.reshape(shape[0], shape[1], shape[2], shape[3])
            else -> multikArray.reshape(shape[0], shape[1], shape[2], shape[3], *shape.slice(4 until shape.size).toIntArray())
        } as NDArray<Any, DN>

        return Batch(payload.name, BatchData(reshapedArray, dtype), payload.metadata)
    }

    object Float16 {
        fun fromBits(bits: Short): Float {
            val s = (bits.toInt() shr 15) and 0x1
            var e = (bits.toInt() shr 10) and 0x1f
            var m = bits.toInt() and 0x3ff

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

    abstract fun getPayload(frameAccessor: PyFrameAccessor, name: String) : Payload
}
