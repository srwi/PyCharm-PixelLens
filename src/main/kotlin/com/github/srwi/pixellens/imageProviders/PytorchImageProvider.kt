package com.github.srwi.pixellens.imageProviders

import com.jetbrains.python.debugger.PyDebugValue

class PytorchImageProvider : ImageProvider() {
    override fun getDataPreparationFunction(functionName: String, variableName: String): String {
        return """
            def $functionName(variable):
                import base64
                import ctypes
                import json
    
                img_flattened = variable.detach().cpu().contiguous().view(-1)
                img_bytes = img_flattened.numel() * img_flattened.element_size() * ctypes.c_ubyte
                img_bytes = img_bytes.from_address(img_flattened.data_ptr())
                img_b64 = base64.b64encode(bytes(img_bytes)).decode('utf-8')
                payload = {
                    'data': img_b64,
                    'metadata': {
                        'name': '$variableName',
                        'shape': list(variable.shape),
                        'dtype': str(variable.dtype)[6:]  # dtype starts with "torch."
                    }
                }
                return json.dumps(payload)
        """
    }

    override fun typeSupported(value: PyDebugValue): Boolean {
        return value.type == "Tensor" || value.type == "Parameter"
    }
}