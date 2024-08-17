package com.github.srwi.pixellens.dataTransmitters

import com.github.srwi.pixellens.interop.Python
import com.intellij.openapi.progress.ProgressIndicator
import com.jetbrains.python.debugger.PyFrameAccessor
import java.io.IOException
import kotlin.math.ceil
import kotlin.math.min

class EvaluateDataTransmitter : DataTransmitter() {
    override fun getJsonData(frameAccessor: PyFrameAccessor, progressIndicator: ProgressIndicator, variableName: String): String {
        val chunkSize = 2_000_000

        progressIndicator.text = "Receiving data..."
        progressIndicator.fraction = 0.0

        val totalSize = getTotalDataSize(frameAccessor, variableName)
        val totalChunks = ceil(totalSize.toDouble() / chunkSize).toInt()

        val base64Data = StringBuilder(totalSize)
        for (i in 0 until totalChunks) {
            if (progressIndicator.isCanceled) {
                throw InterruptedException("Transmission cancelled")
            }

            progressIndicator.fraction = i.toDouble() / totalChunks

            val start = i * chunkSize
            val end = min(start + chunkSize, totalSize)
            val chunk = getChunk(frameAccessor, variableName, start, end)
            base64Data.append(chunk)
        }
        progressIndicator.fraction = 1.0

        return base64Data.toString()
    }

    private fun getChunk(frameAccessor: PyFrameAccessor, variableName: String, start: Int, end: Int): String {
        return Python.evaluateExpression(frameAccessor, "$variableName[$start:$end]").value
            ?: throw IOException("Failed to transmit chunk")
    }

    private fun getTotalDataSize(frameAccessor: PyFrameAccessor, variableName: String): Int {
        return Python.evaluateExpression(frameAccessor, "len($variableName)").value?.toInt()
            ?: throw IOException("Failed to transmit chunk")
    }
}
