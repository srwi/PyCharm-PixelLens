package com.github.srwi.pixellens.imageProviders

import com.jetbrains.python.debugger.PyDebugValue

class PillowImageProvider : ImageProvider() {
    override fun getDataPreparationCommand(variableName: String): String {
        return """
            import base64 as __tmp_base64
            import io as __tmp_io
            import json as __tmp_json

            __tmp_image = $variableName
            __tmp_image_bytes = __tmp_image.tobytes()
            $Base64DataVariableName = __tmp_base64.b64encode(__tmp_image_bytes).decode('utf-8')
            $MetadataVariableName = {
                'name': '$variableName',
                'length': len($Base64DataVariableName),
                'shape': (__tmp_image.size[1], __tmp_image.size[0], len(__tmp_image.getbands())),
                'dtype': __tmp_image.mode,
            }
            $MetadataVariableName = __tmp_json.dumps($MetadataVariableName)
        """.trimIndent()
    }

    override fun getCleanupCommand(): String {
        return """
            del __tmp_base64, __tmp_io, __tmp_json
            del __tmp_image, __tmp_image_bytes, $Base64DataVariableName, $MetadataVariableName
        """.trimIndent()
    }

    override fun shapeSupported(value: PyDebugValue): Boolean {
        return true
    }

    override fun typeSupported(value: PyDebugValue): Boolean {
        return true
    }
}