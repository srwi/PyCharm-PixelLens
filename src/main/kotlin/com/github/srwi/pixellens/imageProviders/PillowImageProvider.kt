package com.github.srwi.pixellens.imageProviders

import com.jetbrains.python.debugger.PyDebugValue

class PillowImageProvider : ImageProvider() {
    override fun getDataPreparationCommand(variableName: String, outputVariableName: String): String {
        return """
            import base64
            import json

            img_bytes = $variableName.tobytes()
            img_b64 = base64.b64encode(img_bytes).decode('utf-8')
            $outputVariableName = {
                'data': img_b64,
                'metadata': {
                    'name': '$variableName',
                    'shape': ($variableName.size[1], $variableName.size[0], len($variableName.getbands())),
                    'dtype': $variableName.mode
                }
            }
            $outputVariableName = json.dumps($outputVariableName)
        """.trimIndent()
    }

    override fun shapeSupported(value: PyDebugValue): Boolean {
        return true
    }

    override fun typeSupported(value: PyDebugValue): Boolean {
        return true
    }
}