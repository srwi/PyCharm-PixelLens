package com.github.srwi.pixellens.imageProviders

import com.jetbrains.python.debugger.PyDebugValue

class NumpyImageProvider : ImageProvider() {
    override fun getDataPreparationCommand(variableName: String, outputVariableName: String): String {
        return """
            import json as __tmp_json
            import base64 as __tmp_base64
            import numpy as __tmp_np
    
            __tmp_img_bytes = __tmp_np.ascontiguousarray($variableName).tobytes()
            __tmp_img_base64 = __tmp_base64.b64encode(__tmp_img_bytes).decode('utf-8')
            
            $outputVariableName = {
                'data': __tmp_img_base64,
                'metadata': {
                    'name': '$variableName',
                    'shape': $variableName.shape,
                    'dtype': str($variableName.dtype)
                }
            }
            $outputVariableName = __tmp_json.dumps($outputVariableName)
            
            del __tmp_np, __tmp_json, __tmp_base64
            del __tmp_img_bytes, __tmp_img_base64
        """.trimIndent()
    }

    override fun typeSupported(value: PyDebugValue): Boolean {
        return value.type == "ndarray"
    }
}