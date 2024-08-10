package com.github.srwi.pixellens.imageProviders

import com.jetbrains.python.debugger.PyDebugValue

class TensorflowImageProvider : ImageProvider() {
    override fun getDataPreparationCommand(variableName: String, outputVariableName: String): String {
        return """
            import base64 as __tmp_base64
            import json as __tmp_json
            import tensorflow as __tmp_tf

            __tmp_img_array = $variableName.numpy()
            __tmp_img_bytes = __tmp_img_array.tobytes()
            __tmp_img_base64 = __tmp_base64.b64encode(__tmp_img_bytes).decode('utf-8')
            $outputVariableName = {
                'data': __tmp_img_base64,
                'metadata': {
                    'name': '$variableName',
                    'shape': __tmp_img_array.shape,
                    'dtype': str(__tmp_img_array.dtype)
                }
            }
            $outputVariableName = __tmp_json.dumps($outputVariableName)
            
            del __tmp_base64, __tmp_json, __tmp_tf
            del __tmp_img_array, __tmp_img_bytes, __tmp_img_base64
        """.trimIndent()
    }

    override fun typeSupported(value: PyDebugValue): Boolean {
        return value.type == "EagerTensor" || value.type == "ResourceVariable"
    }
}
