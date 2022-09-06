package dev.brighten.log.socket

import dev.brighten.log.utils.EncryptionUtils.Companion.encrypt
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.ObjectOutputStream
import java.net.Socket
import java.security.PublicKey
import java.util.*

class OutRequest(header: String, ip: String?, key: PublicKey?) {
    private val socket: Socket
    private var encrypted = false
    private val streamToSend: DataOutputStream
    private val bytesStream: ByteArrayOutputStream
    val objects: ObjectOutputStream
    private var key: PublicKey? = null

    init {
        socket = Socket(ip, 80)
        streamToSend = DataOutputStream(socket.getOutputStream())
        streamToSend.writeUTF(header)
        if (key != null) {
            encrypted = true
            streamToSend.writeBoolean(true)
            this.key = key
        } else {
            encrypted = false
            streamToSend.writeBoolean(false)
        }
        bytesStream = ByteArrayOutputStream()
        objects = ObjectOutputStream(bytesStream)
    }

    @Throws(IOException::class)
    fun write() {
        objects.close()
        val bytesToWrite = if (encrypted) encrypt(bytesStream.toByteArray(), key!!) else bytesStream.toByteArray();
        try {
            val base = Base64.getEncoder().encodeToString(bytesToWrite)
            streamToSend.writeUTF(base)
            streamToSend.close()
            socket.close()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}