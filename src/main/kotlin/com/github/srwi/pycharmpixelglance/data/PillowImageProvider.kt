package com.github.srwi.pycharmpixelglance.data

import com.github.srwi.pycharmpixelglance.interop.Python.evaluateExpression
import com.github.srwi.pycharmpixelglance.interop.Python.executeStatement
import com.jetbrains.python.debugger.PyFrameAccessor

class PillowImageProvider : ImageProvider() {
    override fun getPayload(frameAccessor: PyFrameAccessor, name: String): Payload {
        val command = """
            import base64 as __tmp_base64
            import io as __tmp_io
            import json as __tmp_json
        
            __tmp_image = $name
            __tmp_image_bytes = __tmp_image.tobytes()
            __tmp_image_base64 = __tmp_base64.b64encode(__tmp_image_bytes).decode('utf-8')
        
            __tmp_payload = {
                'imageData': __tmp_image_base64,
                'metadata': {
                    'shape': (__tmp_image.size[1], __tmp_image.size[0], len(__tmp_image.getbands())),
                    'dtype': __tmp_image.mode,
                }
            }
        
            __tmp_data = __tmp_json.dumps(__tmp_payload)
        """.trimIndent()

        val cleanupCommand = """
            del __tmp_base64, __tmp_io, __tmp_json
            del __tmp_image, __tmp_image_bytes, __tmp_image_base64, __tmp_payload, __tmp_data
        """.trimIndent()

        try {
            executeStatement(frameAccessor, command)
            val data = evaluateExpression(frameAccessor, "__tmp_data")?.value
            return deserializeJsonPayload(data ?: throw IllegalStateException("Failed to retrieve data"))
        } finally {
            executeStatement(frameAccessor, cleanupCommand)
        }
    }
}