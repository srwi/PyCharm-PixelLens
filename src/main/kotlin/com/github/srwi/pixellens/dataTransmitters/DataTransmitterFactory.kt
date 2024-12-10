package com.github.srwi.pixellens.dataTransmitters

import com.github.srwi.pixellens.settings.PixelLensSettingsState
import com.intellij.openapi.projectRoots.Sdk
import com.jetbrains.python.console.PydevConsoleCommunication
import com.jetbrains.python.debugger.PyDebugProcess
import com.jetbrains.python.debugger.PyFrameAccessor
import com.jetbrains.python.run.PyDebugProcessHandler
import com.jetbrains.python.sdk.PythonSdkUtil

class DataTransmitterFactory {
    companion object {
        fun getDataTransmitter(frameAccessor: PyFrameAccessor): DataTransmitter {
            val settings = PixelLensSettingsState.instance
            return if (settings.alwaysUseEvaluateTransmission || isRemoteSession(frameAccessor)) {
                EvaluateDataTransmitter()
            } else {
                SocketDataTransmitter()
            }
        }

        private fun isRemoteSession(frameAccessor: PyFrameAccessor): Boolean {
            // JupyterDebugProcess is not available in the Community Edition, therefore we check for the class name
            if (frameAccessor is PydevConsoleCommunication || frameAccessor.javaClass.simpleName == "JupyterDebugProcess") {
                // TODO: How to check the selected Sdk instead of all Sdks?
                // For now, we assume that if any Sdk is remote, then the session is assumed to be remote
                return PythonSdkUtil.getAllSdks().any { sdk: Sdk ->
                    PythonSdkUtil.isRemote(sdk)
                }
            } else if (frameAccessor is PyDebugProcess) {
                // PyRemoteProcessHandler is marked Internal, therefore we check for PyDebugProcessHandler instead
                return frameAccessor.processHandler !is PyDebugProcessHandler
            }

            // When in doubt, remote is the safer assumption
            return true
        }
    }
}