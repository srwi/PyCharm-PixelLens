package com.github.srwi.pixellens.imageProviders

import com.jetbrains.python.debugger.PyDebugValue

class PytorchImageProvider : ImageProvider() {
    override fun getDataPreparationCommand(variableName: String, outputVariableName: String): String {
        return """
            import base64
            import ctypes
            import json

            img_flattened = $variableName.detach().cpu().view(-1).contiguous()
            img_bytes = img_flattened.numel() * img_flattened.element_size() * ctypes.c_ubyte
            img_bytes = img_bytes.from_address(img_flattened.data_ptr())
            img_b64 = base64.b64encode(bytes(img_bytes)).decode('utf-8')
            $outputVariableName = {
                'data': img_b64,
                'metadata': {
                    'name': '$variableName',
                    'shape': list($variableName.shape),
                    'dtype': str($variableName.dtype)[6:]  # dtype starts with "torch."
                }
            }
            $outputVariableName = json.dumps($outputVariableName)
        """.trimIndent()
    }

    override fun typeSupported(value: PyDebugValue): Boolean {
        return value.type == "Tensor" || value.type == "Parameter"
    }
}