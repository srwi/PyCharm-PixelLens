package com.github.srwi.pixellens.dataTransmitters

import com.github.srwi.pixellens.interop.Python
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

class SocketDataTransmitter : DataTransmitter() {
    companion object {
        const val BUFFER_SIZE: Int = 8192
        const val TIMEOUT_IN_S: Long = 10
    }

    override fun getJsonData(frameAccessor: PyFrameAccessor, progressIndicator: ProgressIndicator, variableName: String): String {
        var serverSocket: AsynchronousServerSocketChannel? = null
        var clientChannel: AsynchronousSocketChannel? = null
        var throwable: Throwable? = null
        var bytes: ByteArray? = null

        try {
            progressIndicator.text = "Setting up connection..."
            progressIndicator.fraction = 0.0

            serverSocket = createServerSocket()
            triggerTransmission(frameAccessor, variableName, getServerSocketPort(serverSocket))

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
            clientChannel?.close()
            serverSocket?.close()

            throwable?.let { throw it }
        }

        return String(bytes!!, Charsets.UTF_8)
    }

    private fun createServerSocket(): AsynchronousServerSocketChannel {
        return AsynchronousServerSocketChannel.open().bind(InetSocketAddress("localhost", 0))
    }

    private fun getServerSocketPort(serverSocket: AsynchronousServerSocketChannel): Int {
        return (serverSocket.localAddress as InetSocketAddress).port
    }

    private fun waitForClientConnection(serverSocket: AsynchronousServerSocketChannel): AsynchronousSocketChannel {
        return serverSocket.accept().get(TIMEOUT_IN_S, TimeUnit.SECONDS)
            ?: throw IOException("Failed to establish a connection with the client")
    }

    private fun triggerTransmission(frameAccessor: PyFrameAccessor, variableName: String, port: Int) {
        val command = """
            import socket as __tmp_socket
            import struct as __tmp_struct
            
            __tmp_bytes = $variableName.encode('utf-8')
            __tmp_a_socket = None
            
            try:
                with __tmp_socket.socket(__tmp_socket.AF_INET, __tmp_socket.SOCK_STREAM) as __tmp_a_socket:
                    __tmp_a_socket.settimeout($TIMEOUT_IN_S)
                    __tmp_a_socket.connect(('localhost', $port))
                    __tmp_a_socket.sendall(__tmp_struct.pack('>Q', len(__tmp_bytes)))
                    __tmp_a_socket.sendall(__tmp_bytes)
            except ConnectionResetError:
                # Can happen if Kotlin side closes connection before Python socket has fully finished its operations.
                pass
                
            del __tmp_socket, __tmp_struct
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
            if (bytesRead == -1) break

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
