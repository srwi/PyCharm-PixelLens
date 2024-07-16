package com.github.srwi.pycharmpixelglance.data

import com.github.srwi.pycharmpixelglance.interop.Python.evaluateExpression
import com.github.srwi.pycharmpixelglance.interop.Python.executeStatement
import com.jetbrains.python.debugger.PyFrameAccessor

class NumpyImageProvider : ImageProvider() {
    override fun getPayload(frameAccessor: PyFrameAccessor, name: String): Payload {
        val command = """
            import base64 as __tmp_base64
            import json as __tmp_json
            import numpy as __tmp_np
            
            __tmp_img_array = $name
            __tmp_img_bytes = __tmp_img_array.tobytes()
            __tmp_img_base64 = __tmp_base64.b64encode(__tmp_img_bytes).decode('utf-8')
            __tmp_img_payload = {
                'imageData': __tmp_img_base64,
                'metadata': {
                    'shape': __tmp_img_array.shape,
                    'dtype': str(__tmp_img_array.dtype)
                }
            }
            __tmp_img_data = __tmp_json.dumps(__tmp_img_payload)
        """.trimIndent()

        val cleanupCommand = """
            del __tmp_base64, __tmp_json, __tmp_np
            del __tmp_img_array, __tmp_img_bytes, __tmp_img_base64, __tmp_img_payload, __tmp_img_data
        """.trimIndent()

        try {
            executeStatement(frameAccessor, command)
            val data = evaluateExpression(frameAccessor, "__tmp_img_data")?.value
            return deserializeJsonPayload(data ?: throw IllegalStateException("Failed to retrieve data"))
        } finally {
            executeStatement(frameAccessor, cleanupCommand)
        }
    }
}