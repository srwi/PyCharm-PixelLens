package com.github.srwi.pycharmpixelglance.data

import com.github.srwi.pycharmpixelglance.interop.Python.evaluateExpression
import com.github.srwi.pycharmpixelglance.interop.Python.executeStatement
import com.jetbrains.python.debugger.PyFrameAccessor

class PytorchImageProvider : ImageProvider() {
    override fun getPayload(frameAccessor: PyFrameAccessor, name: String) : Payload {
        val command = ("""
            import base64
            import json
            import torch
            
            image_tensor = """ + name + """
            image_bytes = image_tensor.detach().cpu().contiguous().view(-1).tolist()
            image_bytes = bytes(int(b) for b in image_bytes)
            image_base64 = base64.b64encode(image_bytes).decode('utf-8')
            payload = {
                'imageData': image_base64,
                'metadata': {
                    'shape': list(image_tensor.shape),
                    'dtype': str(image_tensor.dtype)[6:]  # dtype starts with "torch."
                }
            }
            data = json.dumps(payload)
        """).trimIndent()

        executeStatement(frameAccessor, command)
        val data = evaluateExpression(frameAccessor, "data")?.value
        return deserializeJsonPayload(data as String)
    }
}