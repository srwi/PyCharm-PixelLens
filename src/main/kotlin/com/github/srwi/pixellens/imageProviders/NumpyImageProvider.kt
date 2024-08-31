package com.github.srwi.pixellens.imageProviders

import com.jetbrains.python.debugger.PyDebugValue

class NumpyImageProvider : ImageProvider() {
    override fun getDataPreparationFunction(functionName: String): String {
        return """
            def $functionName(variable):
                import json
                import base64
                import numpy as np
        
                img_bytes = np.ascontiguousarray(variable).tobytes()
                img_b64 = base64.b64encode(img_bytes).decode('utf-8')
                
                payload = {
                    'data': img_b64,
                    'shape': variable.shape,
                    'dtype': str(variable.dtype)
                }
                return json.dumps(payload)
        """
    }

    override fun typeSupported(value: PyDebugValue): Boolean {
        return value.type == "ndarray"
    }
}