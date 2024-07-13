package com.github.srwi.pycharmpixelglance.data

import com.jetbrains.python.debugger.PyFrameAccessor
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


@Serializable
data class Metadata(val shape: List<Int>, val dtype: String)

@Serializable
data class Payload(val imageData: String, val metadata: Metadata)

abstract class ImageProvider {
    fun getImageByVariableName(frameAccessor: PyFrameAccessor, name: String) : DisplayableData {
        val payload = getPayload(frameAccessor, name)
        val image = processImageData(payload)
        return image
    }

    fun deserializeJsonPayload(jsonPayloadString: String): Payload {
        val json = Json { ignoreUnknownKeys = true }
        return json.decodeFromString<Payload>(jsonPayloadString)
    }

    abstract fun getPayload(frameAccessor: PyFrameAccessor, name: String) : Payload

    abstract fun processImageData(payload: Payload) : DisplayableData
}
