package dev.brighten.log.utils

import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class EncryptionUtils {
    companion object {
        val rsaGenerator: KeyPairGenerator = KeyPairGenerator.getInstance("RSA");
        val aesGenerator: KeyGenerator = KeyGenerator.getInstance("AES");
        lateinit var skey: SecretKey;
        val rsaKeyFactory = KeyFactory.getInstance("RSA");

        fun geneateRsa(): KeyPair? {
            val keyPair = rsaGenerator.genKeyPair();
            return keyPair;
        }

        fun encrypt(data: ByteArray, publicKey: PublicKey): ByteArray? {
            val cipher: Cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");

            cipher.init(Cipher.ENCRYPT_MODE, publicKey);

            return cipher.doFinal(data);
        }

        fun decrypt(data: ByteArray, privateKey: PrivateKey): ByteArray? {
            val cipher: Cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");

            cipher.init(Cipher.DECRYPT_MODE, privateKey);

            return cipher.doFinal(data);
        }

        fun publicKeyFromString(publicKeyString: String): PublicKey? {
            val keySpec = X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyString.toByteArray()));
            return rsaKeyFactory.generatePublic(keySpec);
        }

        fun privateKeyFromString(privateKeyString: String): PrivateKey? {
            val keySpec = PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyString.toByteArray()));

            return rsaKeyFactory.generatePrivate(keySpec);
        }
    }

    init {
        rsaGenerator.initialize(2048);

        aesGenerator.init(128);
        skey = aesGenerator.generateKey();
    }
}