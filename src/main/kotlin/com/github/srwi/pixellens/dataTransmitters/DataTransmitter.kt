package com.github.srwi.pixellens.dataTransmitters

import com.intellij.openapi.progress.ProgressIndicator
import com.jetbrains.python.debugger.PyFrameAccessor

abstract class DataTransmitter {
    abstract fun getJsonData(frameAccessor: PyFrameAccessor, progressIndicator: ProgressIndicator, variableName: String): String
}