package com.github.srwi.pixellens.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.*

object Base64ByteArraySerializer : KSerializer<ByteArray> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Base64ByteArray", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ByteArray) {
        val base64String = Base64.getEncoder().encodeToString(value)
        encoder.encodeString(base64String)
    }

    override fun deserialize(decoder: Decoder): ByteArray {
        val base64String = decoder.decodeString()
        return Base64.getDecoder().decode(base64String)
    }
}

@Serializable
data class BatchDataPayload(
    val shape: List<Int>,
    val dtype: String,
    @Serializable(with = Base64ByteArraySerializer::class) val data: ByteArray
)