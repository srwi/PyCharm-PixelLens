package com.github.srwi.pycharmpixelglance.data

import com.github.srwi.pycharmpixelglance.interop.Python.evaluateExpression
import com.github.srwi.pycharmpixelglance.interop.Python.executeStatement
import com.jetbrains.python.debugger.PyFrameAccessor

class NumpyImageProvider : ImageProvider() {
    override fun getPayload(frameAccessor: PyFrameAccessor, name: String) : Payload {
        val command = ("""
            import base64
            import json
            import numpy as np
            
            image_array = """ + name + """
            image_bytes = image_array.tobytes()
            image_base64 = base64.b64encode(image_bytes).decode('utf-8')
            payload = {
                'imageData': image_base64,
                'metadata': {
                    'shape': image_array.shape,
                    'dtype': str(image_array.dtype)
                }
            }
            data = json.dumps(payload)
        """).trimIndent()

        executeStatement(frameAccessor, command)
        val data = evaluateExpression(frameAccessor, "data")?.value
        return deserializeJsonPayload(data as String)
    }
}