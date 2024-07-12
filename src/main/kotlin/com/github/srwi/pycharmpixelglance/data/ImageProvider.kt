package com.github.srwi.pycharmpixelglance.data

import CustomImage
import com.jetbrains.python.debugger.PyFrameAccessor
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.kotlinx.multik.ndarray.data.DN
import org.jetbrains.kotlinx.multik.ndarray.data.NDArray


@Serializable
data class Metadata(val shape: List<Int>, val dtype: String)

@Serializable
data class Payload(val imageData: String, val metadata: Metadata)

@Suppress("IMPLICIT_CAST_TO_ANY")
abstract class ImageProvider {
    fun getVariable(frameAccessor: PyFrameAccessor, name: String) : Any? {
        val payload = getPayload(frameAccessor, name)
        val imageArray = processImageData(payload)
        val image = CustomImage(imageArray)
        return image
    }

    fun deserializeJsonPayload(jsonPayloadString: String): Payload {
        val json = Json { ignoreUnknownKeys = true }
        return json.decodeFromString<Payload>(jsonPayloadString)
    }

    abstract fun getPayload(frameAccessor: PyFrameAccessor, name: String) : Payload

    abstract fun processImageData(payload: Payload) : NDArray<Any, DN>
}
