package com.github.srwi.pixellens.dataTransmitters

import com.github.srwi.pixellens.interop.Python
import com.github.srwi.pixellens.settings.PixelLensSettingsState
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.jetbrains.python.debugger.PyFrameAccessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicLong

class SocketDataTransmitter : DataTransmitter() {
    companion object {
        const val BUFFER_SIZE: Int = 8192
        const val TIMEOUT_IN_S: Long = 10
        const val ERROR_READ_TIMEOUT_IN_S: Long = 5
        const val ERROR_VARIABLE_BASE_NAME = "__srwi_pixellens_transmission_error"
        private val transmissionCounter = AtomicLong()
    }

    override fun getJsonData(frameAccessor: PyFrameAccessor, progressIndicator: ProgressIndicator, variableName: String): String {
        // A unique name per attempt so that a leftover error of a previous attempt can never
        // be mistaken for the current one
        val errorVariableName = "${ERROR_VARIABLE_BASE_NAME}_${transmissionCounter.incrementAndGet()}"

        var serverSocket: AsynchronousServerSocketChannel? = null
        var clientChannel: AsynchronousSocketChannel? = null
        var throwable: Throwable? = null
        var bytes: ByteArray? = null

        try {
            progressIndicator.text = "Setting up connection..."
            progressIndicator.fraction = 0.0

            serverSocket = createServerSocket()
            triggerTransmission(frameAccessor, variableName, errorVariableName, getServerSocketPort(serverSocket))

            progressIndicator.text = "Waiting for connection..."
            progressIndicator.fraction = 0.5

            clientChannel = waitForClientConnection(serverSocket)

            progressIndicator.text = "Connection established"
            progressIndicator.fraction = 1.0

            val totalSize = readTotalDataSize(clientChannel)
            bytes = receiveData(clientChannel, totalSize, progressIndicator)
        } catch (t: Throwable) {
            throwable = t
        } finally {
            // Close before any error handling so the Python side fails fast instead of
            // blocking on a socket nobody reads from anymore
            clientChannel?.close()
            serverSocket?.close()
        }

        throwable?.let {
            if (it is TimeoutException || it is TransmissionIncompleteException) {
                throw enrichTransmissionException(it, frameAccessor, errorVariableName, progressIndicator)
            }
            clearTransmissionErrorAsync(frameAccessor, errorVariableName)
            throw it
        }

        return String(bytes!!, Charsets.UTF_8)
    }

    private fun createServerSocket(): AsynchronousServerSocketChannel {
        return AsynchronousServerSocketChannel.open().bind(InetSocketAddress("127.0.0.1", 0))
    }

    private fun getServerSocketPort(serverSocket: AsynchronousServerSocketChannel): Int {
        return (serverSocket.localAddress as InetSocketAddress).port
    }

    private fun waitForClientConnection(serverSocket: AsynchronousServerSocketChannel): AsynchronousSocketChannel {
        return serverSocket.accept().get(TIMEOUT_IN_S, TimeUnit.SECONDS)
            ?: throw IOException("Failed to establish a connection with the client")
    }

    private fun enrichTransmissionException(
        cause: Exception,
        frameAccessor: PyFrameAccessor,
        errorVariableName: String,
        progressIndicator: ProgressIndicator
    ): Throwable {
        if (progressIndicator.isCanceled) {
            clearTransmissionErrorAsync(frameAccessor, errorVariableName)
            return InterruptedException("Transmission cancelled")
        }

        val pythonError = readTransmissionErrorWithTimeout(frameAccessor, errorVariableName)
        if (pythonError != null) {
            return IOException("Data transmission failed on the Python side:\n$pythonError", cause)
        }

        return when (cause) {
            is TimeoutException -> IOException(
                "Timed out waiting for the socket connection. " +
                        "The transmission command did not run or did not finish within $TIMEOUT_IN_S seconds.", cause
            )
            else -> cause
        }
    }

    private fun readTransmissionErrorWithTimeout(frameAccessor: PyFrameAccessor, errorVariableName: String): String? {
        // The debuggee executes commands one at a time, so this read may be queued behind
        // the still-running transmission command; give up after a short budget instead of
        // blocking the failing request for the debugger's full evaluate timeout
        val future = ApplicationManager.getApplication().executeOnPooledThread<String?> {
            readAndClearTransmissionError(frameAccessor, errorVariableName)
        }
        return try {
            future.get(ERROR_READ_TIMEOUT_IN_S, TimeUnit.SECONDS)
        } catch (_: Exception) {
            null
        }
    }

    private fun readAndClearTransmissionError(frameAccessor: PyFrameAccessor, errorVariableName: String): String? {
        return try {
            // locals() covers debugger frames, globals() covers the console; absence yields "None"
            val expression = "locals().get('$errorVariableName', globals().get('$errorVariableName'))"
            val value = Python.evaluateStringExpression(frameAccessor, expression)
            if (value == null || value == "None") null else value
        } catch (_: Exception) {
            null
        } finally {
            try {
                Python.executeStatement(frameAccessor, deleteStatement(errorVariableName))
            } catch (_: Exception) {
                // Cleanup is best-effort; a leftover variable must not mask the original error
            }
        }
    }

    private fun clearTransmissionErrorAsync(frameAccessor: PyFrameAccessor, errorVariableName: String) {
        // The debuggee sets the error variable on any failure, not only on timeouts, so it
        // has to be cleaned up on every failure path. The debuggee executes commands in
        // order, so this delete always runs after the transmission command has finished.
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                Python.executeStatement(frameAccessor, deleteStatement(errorVariableName))
            } catch (_: Exception) {
                // Best effort; the debug session may already be gone
            }
        }
    }

    private fun deleteStatement(variableName: String): String {
        return """
            try:
                del $variableName
            except NameError:
                pass
        """.trimIndent()
    }

    private fun triggerTransmission(frameAccessor: PyFrameAccessor, variableName: String, errorVariableName: String, port: Int) {
        val settings = PixelLensSettingsState.instance
        val onException = if (settings.verboseMode) "raise" else "pass"
        val command = """
            import socket as __tmp_socket
            import struct as __tmp_struct
            import traceback as __tmp_traceback

            __tmp_bytes = None
            __tmp_a_socket = None

            try:
                __tmp_bytes = $variableName.encode('utf-8')
                with __tmp_socket.socket(__tmp_socket.AF_INET, __tmp_socket.SOCK_STREAM) as __tmp_a_socket:
                    __tmp_a_socket.settimeout($TIMEOUT_IN_S)
                    __tmp_a_socket.connect(('127.0.0.1', $port))
                    __tmp_a_socket.sendall(__tmp_struct.pack('>Q', len(__tmp_bytes)))
                    __tmp_a_socket.sendall(__tmp_bytes)
            except:
                $errorVariableName = __tmp_traceback.format_exc()
                $onException
            finally:
                del __tmp_socket, __tmp_struct, __tmp_traceback
                del __tmp_bytes, __tmp_a_socket
        """.trimIndent()
        val coroutineScope = CoroutineScope(Dispatchers.Default)
        coroutineScope.launch { Python.executeStatement(frameAccessor, command) }
    }

    private fun readTotalDataSize(clientChannel: AsynchronousSocketChannel): Long {
        val sizeBuffer = ByteBuffer.allocate(java.lang.Long.BYTES)
        clientChannel.read(sizeBuffer).get()
        sizeBuffer.flip()
        return sizeBuffer.long
    }

    private fun receiveData(clientChannel: AsynchronousSocketChannel, totalSize: Long, progressIndicator: ProgressIndicator): ByteArray {
        val buffer = ByteBuffer.allocate(BUFFER_SIZE)
        val dataStream = ByteArrayOutputStream()
        var totalBytesRead = 0L

        while (totalBytesRead < totalSize) {
            if (progressIndicator.isCanceled) {
                throw InterruptedException("Transmission cancelled")
            }

            val bytesRead = clientChannel.read(buffer).get()
            if (bytesRead == -1) {
                throw TransmissionIncompleteException(
                    "The connection was closed after receiving $totalBytesRead of $totalSize bytes."
                )
            }

            buffer.flip()
            dataStream.write(buffer.array(), 0, bytesRead)
            buffer.clear()

            totalBytesRead += bytesRead

            progressIndicator.text = "Receiving data..."
            progressIndicator.fraction = totalBytesRead.toDouble() / totalSize
        }

        return dataStream.toByteArray()
    }
}

private class TransmissionIncompleteException(message: String) : IOException(message)
