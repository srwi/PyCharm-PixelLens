package com.github.srwi.pycharmpixelglance.data

import com.github.srwi.pycharmpixelglance.interop.Python.evaluateExpression
import com.github.srwi.pycharmpixelglance.interop.Python.executeStatement
import com.jetbrains.python.debugger.PyFrameAccessor

class PillowImageProvider : ImageProvider() {
    override fun getPayload(frameAccessor: PyFrameAccessor, name: String) : Payload {
        val command = ("""
            import base64
            import io
            import json
        
            image = """ + name + """
            image_bytes = image.tobytes()
            image_base64 = base64.b64encode(image_bytes).decode('utf-8')
        
            payload = {
                'imageData': image_base64,
                'metadata': {
                    'shape': (image.size[1], image.size[0], len(image.getbands())),
                    'dtype': image.mode,
                }
            }
        
            data = json.dumps(payload)
        """).trimIndent()

        executeStatement(frameAccessor, command)
        val data = evaluateExpression(frameAccessor, "data")?.value
        return deserializeJsonPayload(data as String)
    }
}