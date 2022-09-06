package dev.brighten.log.socket

import dev.brighten.ac.Anticheat
import java.net.ServerSocket
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class LogSocket(port: Int) {
    private val serverSocket: ServerSocket
    private var thread: Thread
    private var enabled: Boolean = true
    private val socketExecutor: ExecutorService;

    init {
        socketExecutor = Executors.newCachedThreadPool();
        serverSocket = ServerSocket(port)
        thread = Thread {
            while (enabled) {
                try {
                    val socket = serverSocket.accept()
                    socket.soTimeout = 10000

                    Anticheat.INSTANCE.logger.info("Received input on port " + port)

                    if(!socket.isClosed) {
                        try {
                            System.out.println("executing!")
                            Anticheat.INSTANCE.socketManager.processInputReceived(socket)
                        } catch (throwable: Throwable) {
                            throwable.printStackTrace()
                        }
                    }
                } catch(e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun startSocket() {
        thread.start();
    }

    fun shutdownSocket() {
        enabled = false
        thread.interrupt()
    }
}