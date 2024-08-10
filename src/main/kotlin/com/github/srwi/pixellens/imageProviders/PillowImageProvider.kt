package com.github.srwi.pixellens.imageProviders

import com.jetbrains.python.debugger.PyDebugValue

class PillowImageProvider : ImageProvider() {
    override fun getDataPreparationCommand(variableName: String, outputVariableName: String): String {
        return """
            import base64 as __tmp_base64
            import io as __tmp_io
            import json as __tmp_json

            __tmp_img_bytes = $variableName.tobytes()
            __tmp_img_base64 = __tmp_base64.b64encode(__tmp_img_bytes).decode('utf-8')
            $outputVariableName = {
                'data': __tmp_img_base64,
                'metadata': {
                    'name': '$variableName',
                    'shape': ($variableName.size[1], $variableName.size[0], len($variableName.getbands())),
                    'dtype': $variableName.mode
                }
            }
            $outputVariableName = __tmp_json.dumps($outputVariableName)
            
            del __tmp_base64, __tmp_io, __tmp_json
            del __tmp_img_bytes, __tmp_img_base64
        """.trimIndent()
    }

    override fun shapeSupported(value: PyDebugValue): Boolean {
        return true
    }

    override fun typeSupported(value: PyDebugValue): Boolean {
        return true
    }
}