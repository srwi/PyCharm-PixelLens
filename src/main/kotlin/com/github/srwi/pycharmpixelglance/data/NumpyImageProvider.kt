package com.github.srwi.pycharmpixelglance.data

import com.github.srwi.pycharmpixelglance.interop.Python.evaluateExpression
import com.github.srwi.pycharmpixelglance.interop.Python.executeStatement
import com.jetbrains.python.debugger.PyFrameAccessor
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.ndarray
import org.jetbrains.kotlinx.multik.ndarray.data.D1
import org.jetbrains.kotlinx.multik.ndarray.data.DN
import org.jetbrains.kotlinx.multik.ndarray.data.NDArray
import java.nio.ByteBuffer
import java.util.*

class NumpyImageProvider : ImageProvider() {
    override fun getPayload(frameAccessor: PyFrameAccessor, name: String) : Payload {
        val command = ("""
            import base64
            import json
            import numpy as np
            
            image_array = """ + name + """
            image_bytes = image_array.tobytes()
            image_base64 = base64.b64encode(image_bytes).decode('utf-8')
            payload = {
                'imageData': image_base64,
                'metadata': {
                    'shape': image_array.shape,
                    'dtype': str(image_array.dtype)
                }
            }
            data = json.dumps(payload)
        """).trimIndent()

        executeStatement(frameAccessor, command)
        val data = evaluateExpression(frameAccessor, "data")?.value
        return deserializeJsonPayload(data as String)
    }

    override fun processImageData(payload: Payload): NDArray<Any, DN> {
        val imageBase64 = payload.imageData
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
            "float32" -> {
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
            "int32" -> {
                val array = IntArray(imageBytes.size / 4)
                imageBuffer.asIntBuffer().get(array)
                mk.ndarray(array)
            }
            "int64" -> {
                val array = LongArray(imageBytes.size / 8)
                imageBuffer.asLongBuffer().get(array)
                mk.ndarray(array)
            }
//            "uint8" -> {
//                val array = ByteArray(imageBytes.size)
//                imageBuffer.get(array)
//                val ubyteArray = array.map { (it and 0xFF.toByte()).toUByte() }.toUByteArray()
//                mk.ndarray(ubyteArray)
//            }
//            "uint16" -> {
//                val array = ShortArray(imageBytes.size / 2)
//                imageBuffer.asShortBuffer().get(array)
//                val ushortArray = array.map { it.toUShort() }.toUShortArray()
//                mk.ndarray(ushortArray)
//            }
//            "uint32" -> {
//                val array = IntArray(imageBytes.size / 4)
//                imageBuffer.asIntBuffer().get(array)
//                val uintArray = array.map { it.toUInt() }.toUIntArray()
//                mk.ndarray(uintArray)
//            }
//            "uint64" -> {
//                val array = LongArray(imageBytes.size / 8)
//                imageBuffer.asLongBuffer().get(array)
//                val ulongArray = array.map { it.toULong() }.toULongArray()
//                mk.ndarray(ulongArray)
//            }
//            "bool" -> {
//                val array = ByteArray(imageBytes.size)
//                imageBuffer.get(array)
//                val booleanArray = array.map { it != 0.toByte() }.toBooleanArray()
//                mk.ndarray(booleanArray)
//            }
            else -> throw IllegalArgumentException("Unsupported data type: $dtype")
        }

        @Suppress("UNCHECKED_CAST") val reshapedArray: NDArray<Any, DN> = when (shape.size) {
            1 -> multikArray.reshape(shape[0])
            2 -> multikArray.reshape(shape[0], shape[1])
            3 -> multikArray.reshape(shape[0], shape[1], shape[2])
            4 -> multikArray.reshape(shape[0], shape[1], shape[2], shape[3])
            else -> throw IllegalArgumentException("Unsupported shape size: ${shape.size}")
        } as NDArray<Any, DN>

        return reshapedArray
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
}