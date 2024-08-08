package com.github.srwi.pixellens.imageProviders

import com.jetbrains.python.debugger.PyDebugValue

class TensorflowImageProvider : ImageProvider() {
    override fun getDataPreparationCommand(variableName: String): String {
        return """
            import base64 as __tmp_base64
            import json as __tmp_json
            import tensorflow as __tmp_tf

            __tmp_image_tensor = $variableName
            __tmp_image_array = __tmp_image_tensor.numpy()
            __tmp_image_bytes = __tmp_image_array.tobytes()
            $Base64DataVariableName = __tmp_base64.b64encode(__tmp_image_bytes).decode('utf-8')
            $MetadataVariableName = {
                'name': '$variableName',
                'length': len($Base64DataVariableName),
                'shape': __tmp_image_array.shape,
                'dtype': str(__tmp_image_array.dtype)
            }
            $MetadataVariableName = __tmp_json.dumps($MetadataVariableName)
        """.trimIndent()
    }

    override fun getCleanupCommand(): String {
        return """
            del __tmp_base64, __tmp_json, __tmp_tf
            del __tmp_image_tensor, __tmp_image_array, __tmp_image_bytes, $Base64DataVariableName, $MetadataVariableName
        """.trimIndent()
    }

    override fun typeSupported(value: PyDebugValue): Boolean {
        return value.type == "EagerTensor" || value.type == "ResourceVariable"
    }
}
