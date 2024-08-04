package com.github.srwi.pixellens.imageProviders

import com.github.srwi.pixellens.interop.Python.evaluateExpression
import com.github.srwi.pixellens.interop.Python.executeStatement
import com.jetbrains.python.debugger.PyDebugValue
import com.jetbrains.python.debugger.PyFrameAccessor

class TensorflowImageProvider : ImageProvider() {
    override fun getPayload(frameAccessor: PyFrameAccessor, name: String): Payload {
        val command = """
            import base64 as __tmp_base64
            import json as __tmp_json
            import tensorflow as __tmp_tf
            
            __tmp_image_tensor = $name
            __tmp_image_array = __tmp_image_tensor.numpy()
            __tmp_image_bytes = __tmp_image_array.tobytes()
            __tmp_image_base64 = __tmp_base64.b64encode(__tmp_image_bytes).decode('utf-8')
            __tmp_payload = {
                'name': '$name',
                'data': __tmp_image_base64,
                'metadata': {
                    'shape': __tmp_image_array.shape,
                    'dtype': str(__tmp_image_array.dtype)
                }
            }
            __tmp_data = __tmp_json.dumps(__tmp_payload)
        """.trimIndent()

        val cleanupCommand = """
            del __tmp_base64, __tmp_json, __tmp_tf
            del __tmp_image_tensor, __tmp_image_array, __tmp_image_bytes, __tmp_image_base64, __tmp_payload, __tmp_data
        """.trimIndent()

        try {
            executeStatement(frameAccessor, command)
            val data = evaluateExpression(frameAccessor, "__tmp_data")?.value
            return deserializeJsonPayload(data ?: throw IllegalStateException("Failed to retrieve data"))
        } finally {
            executeStatement(frameAccessor, cleanupCommand)
        }
    }

    override fun typeSupported(value: PyDebugValue): Boolean {
        return value.type == "EagerTensor" || value.type == "ResourceVariable"
    }
}
