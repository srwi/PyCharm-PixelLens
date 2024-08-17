package com.github.srwi.pixellens.imageProviders

import com.jetbrains.python.debugger.PyDebugValue

class TensorflowImageProvider : ImageProvider() {
    override fun getDataPreparationCommand(variableName: String, outputVariableName: String): String {
        return """
            import base64
            import json
            import tensorflow tf

            img_array = $variableName.numpy()
            img_bytes = img_array.tobytes()
            img_b64 = base64.b64encode(img_bytes).decode('utf-8')
            $outputVariableName = {
                'data': img_b64,
                'metadata': {
                    'name': '$variableName',
                    'shape': img_array.shape,
                    'dtype': str(img_array.dtype)
                }
            }
            $outputVariableName = json.dumps($outputVariableName)
        """.trimIndent()
    }

    override fun typeSupported(value: PyDebugValue): Boolean {
        return value.type == "EagerTensor" || value.type == "ResourceVariable"
    }
}
