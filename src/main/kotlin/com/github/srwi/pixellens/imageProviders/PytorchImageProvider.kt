package com.github.srwi.pixellens.imageProviders

import com.github.srwi.pixellens.interop.Python.evaluateExpression
import com.github.srwi.pixellens.interop.Python.executeStatement
import com.jetbrains.python.debugger.PyDebugValue
import com.jetbrains.python.debugger.PyFrameAccessor

class PytorchImageProvider : ImageProvider() {
    override fun getPayload(frameAccessor: PyFrameAccessor, name: String): Payload {
        val command = """
            import base64 as __tmp_base64
            import ctypes as __tmp_ctypes
            import json as __tmp_json
            import torch as __tmp_torch
            
            __tmp_image_tensor = $name
            __tmp_image_flattened = __tmp_image_tensor.detach().cpu().view(-1).contiguous()
            __tmp_image_bytes = __tmp_image_flattened.numel() * __tmp_image_flattened.element_size() * __tmp_ctypes.c_ubyte
            __tmp_image_bytes = __tmp_image_bytes.from_address(__tmp_image_flattened.data_ptr())
            __tmp_image_base64 = __tmp_base64.b64encode(bytes(__tmp_image_bytes)).decode('utf-8')
            __tmp_payload = {
                'name': '$name',
                'data': __tmp_image_base64,
                'metadata': {
                    'shape': list(__tmp_image_tensor.shape),
                    'dtype': str(__tmp_image_tensor.dtype)[6:]  # dtype starts with "torch."
                }
            }
            __tmp_data = __tmp_json.dumps(__tmp_payload)
        """.trimIndent()

        val cleanupCommand = """
            del __tmp_base64, __tmp_json, __tmp_torch, __tmp_ctypes
            del __tmp_image_tensor, __tmp_tensor_flattened, __tmp_image_bytes, __tmp_image_base64, __tmp_payload, __tmp_data
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
        return value.type == "Tensor" || value.type == "Parameter"
    }
}