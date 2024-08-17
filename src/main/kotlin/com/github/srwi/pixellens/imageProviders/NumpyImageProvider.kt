package com.github.srwi.pixellens.imageProviders

import com.jetbrains.python.debugger.PyDebugValue

class NumpyImageProvider : ImageProvider() {
    override fun getDataPreparationCommand(variableName: String, outputVariableName: String): String {
        return """
            import json
            import base64
            import numpy as np
    
            img_bytes = np.ascontiguousarray($variableName).tobytes()
            img_b64 = base64.b64encode(img_bytes).decode('utf-8')
            
            $outputVariableName = {
                'data': img_b64,
                'metadata': {
                    'name': '$variableName',
                    'shape': $variableName.shape,
                    'dtype': str($variableName.dtype)
                }
            }
            $outputVariableName = json.dumps($outputVariableName)
        """.trimIndent()
    }

    override fun typeSupported(value: PyDebugValue): Boolean {
        return value.type == "ndarray"
    }
}