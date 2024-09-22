package com.github.srwi.pixellens.dataTransmitters

import com.intellij.openapi.application.ApplicationInfo
import com.github.srwi.pixellens.interop.Python
import com.intellij.openapi.progress.ProgressIndicator
import com.jetbrains.python.debugger.PyFrameAccessor
import java.io.IOException
import kotlin.math.ceil
import kotlin.math.min

class EvaluateDataTransmitter : DataTransmitter() {
    override fun getJsonData(frameAccessor: PyFrameAccessor, progressIndicator: ProgressIndicator, variableName: String): String {
        val chunkSize = getChunkSize()

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

    private fun getChunkSize(): Int
    {
        // Dirty fix for https://youtrack.jetbrains.com/issue/PY-75568/Large-strings-truncated-when-displayed-in-debug-output-or-evaluate-windows-again
        // TODO: Once the bug is fixed, this workaround should be limited to the affected versions
        val appInfo = ApplicationInfo.getInstance()
        val fullVersion = appInfo.fullVersion

        return if (fullVersion == "2024.2.1" || fullVersion == "2024.2.2") {
            256
        } else {
            2_000_000
        }
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
