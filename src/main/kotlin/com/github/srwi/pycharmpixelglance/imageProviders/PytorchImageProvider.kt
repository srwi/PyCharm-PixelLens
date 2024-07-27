package com.github.srwi.pycharmpixelglance.imageProviders

import com.github.srwi.pycharmpixelglance.interop.Python.evaluateExpression
import com.github.srwi.pycharmpixelglance.interop.Python.executeStatement
import com.jetbrains.python.debugger.PyFrameAccessor

class PytorchImageProvider : ImageProvider() {
    override fun getPayload(frameAccessor: PyFrameAccessor, name: String): Payload {
        val command = """
            import base64 as __tmp_base64
            import json as __tmp_json
            import torch as __tmp_torch
            
            __tmp_image_tensor = $name
            __tmp_image_bytes = __tmp_image_tensor.detach().cpu().contiguous().view(-1).tolist()
            __tmp_image_bytes = bytes(int(b) for b in __tmp_image_bytes)
            __tmp_image_base64 = __tmp_base64.b64encode(__tmp_image_bytes).decode('utf-8')
            __tmp_payload = {
                'imageData': __tmp_image_base64,
                'metadata': {
                    'shape': list(__tmp_image_tensor.shape),
                    'dtype': str(__tmp_image_tensor.dtype)[6:]  # dtype starts with "torch."
                }
            }
            __tmp_data = __tmp_json.dumps(__tmp_payload)
        """.trimIndent()

        val cleanupCommand = """
            del __tmp_base64, __tmp_json, __tmp_torch
            del __tmp_image_tensor, __tmp_image_bytes, __tmp_image_base64, __tmp_payload, __tmp_data
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