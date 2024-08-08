package com.github.srwi.pixellens.imageProviders

import com.jetbrains.python.debugger.PyDebugValue

class PytorchImageProvider : ImageProvider() {
    override fun getDataPreparationCommand(variableName: String): String {
        return """
            import base64 as __tmp_base64
            import ctypes as __tmp_ctypes
            import json as __tmp_json
            import torch as __tmp_torch

            __tmp_image_tensor = $variableName
            __tmp_image_flattened = __tmp_image_tensor.detach().cpu().view(-1).contiguous()
            __tmp_image_bytes = __tmp_image_flattened.numel() * __tmp_image_flattened.element_size() * __tmp_ctypes.c_ubyte
            __tmp_image_bytes = __tmp_image_bytes.from_address(__tmp_image_flattened.data_ptr())
            $Base64DataVariableName = __tmp_base64.b64encode(bytes(__tmp_image_bytes)).decode('utf-8')
            $MetadataVariableName = {
                'name': '$variableName',
                'length': len($Base64DataVariableName),
                'shape': list(__tmp_image_tensor.shape),
                'dtype': str(__tmp_image_tensor.dtype)[6:]  # dtype starts with "torch."
            }
            $MetadataVariableName = __tmp_json.dumps($MetadataVariableName)
        """.trimIndent()
    }

    override fun getCleanupCommand(): String {
        return """
            del __tmp_base64, __tmp_json, __tmp_torch, __tmp_ctypes
            del __tmp_image_tensor, __tmp_tensor_flattened, __tmp_image_bytes, $Base64DataVariableName, $MetadataVariableName
        """.trimIndent()
    }

    override fun typeSupported(value: PyDebugValue): Boolean {
        return value.type == "Tensor" || value.type == "Parameter"
    }
}