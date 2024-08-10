package com.github.srwi.pixellens.imageProviders

import com.jetbrains.python.debugger.PyDebugValue

class PytorchImageProvider : ImageProvider() {
    override fun getDataPreparationCommand(variableName: String, outputVariableName: String): String {
        return """
            import base64 as __tmp_base64
            import ctypes as __tmp_ctypes
            import json as __tmp_json

            __tmp_img_flattened = $variableName.detach().cpu().view(-1).contiguous()
            __tmp_img_bytes = __tmp_img_flattened.numel() * __tmp_img_flattened.element_size() * __tmp_ctypes.c_ubyte
            __tmp_img_bytes = __tmp_img_bytes.from_address(__tmp_img_flattened.data_ptr())
            __tmp_img_base64 = __tmp_base64.b64encode(bytes(__tmp_img_bytes)).decode('utf-8')
            $outputVariableName = {
                'data': __tmp_img_base64,
                'metadata': {
                    'name': '$variableName',
                    'shape': list($variableName.shape),
                    'dtype': str($variableName.dtype)[6:]  # dtype starts with "torch."
                }
            }
            $outputVariableName = __tmp_json.dumps($outputVariableName)
            
            del __tmp_base64, __tmp_ctypes, __tmp_json
            del __tmp_img_flattened, __tmp_img_bytes, __tmp_img_base64
        """.trimIndent()
    }

    override fun typeSupported(value: PyDebugValue): Boolean {
        return value.type == "Tensor" || value.type == "Parameter"
    }
}