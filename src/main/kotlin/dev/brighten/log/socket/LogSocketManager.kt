package dev.brighten.log.socket

import dev.brighten.ac.Anticheat
import dev.brighten.ac.utils.StreamUtils
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.IOException
import java.net.Socket
import java.util.function.Consumer

class LogSocketManager {
    private val inputListeners: MutableList<Consumer<SocketEvent>> = ArrayList()
    private val sockets: MutableList<LogSocket> = ArrayList()
    fun startSockets() {
        if (sockets.size > 0) {
            Anticheat.INSTANCE.logger.warning("Had to shutdown previous sockets when trying to start them!")
            sockets.forEach(Consumer { obj: LogSocket -> obj.shutdownSocket() })
            sockets.clear()
        }
        for (port in allowedPorts) {
            val socket = LogSocket(port)
            socket.startSocket()
            sockets.add(socket)
        }
    }

    fun shutdownSockets() {
        if (sockets.size == 0) {
            Anticheat.INSTANCE.logger.warning("No sockets to shutdown!")
            return
        }
        sockets.forEach(Consumer { obj: LogSocket -> obj.shutdownSocket() })
        sockets.clear()
    }

    fun onInputReceived(inputReceived: Consumer<SocketEvent>) {
        inputListeners.add(inputReceived)
    }

    @Throws(IOException::class)
    fun processInputReceived(socket: Socket) {
        val stream = socket.getInputStream()
        val baos = ByteArrayOutputStream()
        StreamUtils.streamCopy(stream, baos);
        for (listener in inputListeners) {
            val ois = DataInputStream(ByteArrayInputStream(baos.toByteArray()))
            val address = socket.inetAddress

            try {
                listener.accept(SocketEvent(socket, address, InRequest(address, ois)))
            } catch(e: Throwable) {
                e.printStackTrace()
            }
        }
        stream.close()
        socket.close()
    }

    companion object {
        private val allowedPorts = intArrayOf(81)
    }
}