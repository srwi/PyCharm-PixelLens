package com.github.srwi.pixellens.imageProviders

import com.jetbrains.python.debugger.PyDebugValue

class PillowImageProvider : ImageProvider() {
    override fun getDataPreparationFunction(functionName: String): String {
        return """
            def $functionName(variable):
                import base64
                import json
    
                img_bytes = variable.tobytes()
                img_b64 = base64.b64encode(img_bytes).decode('utf-8')
                payload = {
                    'data': img_b64,
                    'shape': (variable.size[1], variable.size[0], len(variable.getbands())),
                    'dtype': variable.mode
                }
                return json.dumps(payload)
        """
    }

    override fun shapeSupported(value: PyDebugValue): Boolean {
        return true
    }

    override fun typeSupported(value: PyDebugValue): Boolean {
        return true
    }
}