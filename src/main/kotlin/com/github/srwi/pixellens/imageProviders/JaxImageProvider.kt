package com.github.srwi.pixellens.imageProviders

import com.jetbrains.python.debugger.PyDebugValue

class JaxImageProvider : ImageProvider() {
    override fun getDataPreparationFunction(functionName: String): String {
        return """
            def $functionName(variable):
                import base64
                import jax
                import json
    
                img_array = jax.device_put(variable, jax.devices("cpu")[0]).ravel()
                img_bytes = img_array.tobytes()
                img_b64 = base64.b64encode(bytes(img_bytes)).decode('utf-8')
                payload = {
                    'data': img_b64,
                    'shape': list(variable.shape),
                    'dtype': str(variable.dtype)
                }
                return json.dumps(payload)
        """.trimIndent()
    }

    override fun typeSupported(value: PyDebugValue): Boolean {
        return value.type == "ArrayImpl"
    }
}