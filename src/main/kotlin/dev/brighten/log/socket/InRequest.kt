package dev.brighten.log.socket

import dev.brighten.log.utils.EncryptionUtils.Companion.decrypt
import lombok.Getter
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.net.InetAddress
import java.security.PrivateKey
import java.util.*

@Getter
class InRequest(address: InetAddress, val stream: DataInputStream) {
    val header: String
    val encrypted: Boolean
    var objectString: String? = null

    init {
        header = stream.readUTF()
        encrypted = stream.readBoolean()
        objectString = stream.readUTF();
    }

    val objects: ObjectInputStream
        get() {
            if (encrypted) throw RuntimeException("Encrypted request. Must decrypt request to be processed.")
            return try {
                val bytes = Base64.getDecoder().decode(objectString)
                ObjectInputStream(ByteArrayInputStream(bytes))
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }

    fun getObjects(key: PrivateKey?): ObjectInputStream {
        return try {
            val bytes = Base64.getDecoder().decode(objectString)
            if (!encrypted) {
                ObjectInputStream(ByteArrayInputStream(bytes))
            } else {
                ObjectInputStream(
                    ByteArrayInputStream(
                        Objects
                            .requireNonNull(decrypt(bytes, key!!))
                    )
                )
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}