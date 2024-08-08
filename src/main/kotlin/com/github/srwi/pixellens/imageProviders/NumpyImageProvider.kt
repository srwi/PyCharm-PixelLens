package com.github.srwi.pixellens.imageProviders

import com.jetbrains.python.debugger.PyDebugValue

class NumpyImageProvider : ImageProvider() {
    override fun getDataPreparationCommand(variableName: String): String {
        return """
            import base64 as __tmp_base64
            import json as __tmp_json
            import numpy as __tmp_np
            
            __tmp_img_array = $variableName
            __tmp_img_bytes = __tmp_np.ascontiguousarray(__tmp_img_array).tobytes()
            $Base64DataVariableName = __tmp_base64.b64encode(__tmp_img_bytes).decode('utf-8')
            $MetadataVariableName = {
                'name': '$variableName',
                'length': len($Base64DataVariableName),
                'shape': __tmp_img_array.shape,
                'dtype': str(__tmp_img_array.dtype)
            }
            $MetadataVariableName = __tmp_json.dumps($MetadataVariableName)
        """.trimIndent()
    }

    override fun getCleanupCommand(): String {
        return """
            del __tmp_base64, __tmp_json, __tmp_np
            del __tmp_img_array, __tmp_img_bytes, $MetadataVariableName, $Base64DataVariableName
        """.trimIndent()
    }

    override fun typeSupported(value: PyDebugValue): Boolean {
        return value.type == "ndarray"
    }
}