package com.github.srwi.pixellens.imageProviders

import com.jetbrains.python.debugger.PyDebugValue

class TensorflowImageProvider : ImageProvider() {
    override fun getDataPreparationFunction(functionName: String): String {
        return """
            def $functionName(variable):
                import base64
                import json
                import tensorflow as tf
    
                img_array = variable.numpy()
                img_bytes = img_array.tobytes()
                img_b64 = base64.b64encode(img_bytes).decode('utf-8')
                payload = {
                    'data': img_b64,
                    'shape': img_array.shape,
                    'dtype': str(img_array.dtype)
                }
                return json.dumps(payload)
        """.trimIndent()
    }

    override fun typeSupported(value: PyDebugValue): Boolean {
        return value.type == "EagerTensor" || value.type == "ResourceVariable"
    }
}
