package com.guillermocarcelen.securestore.cypher


import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import com.guillermocarcelen.securestore.SecureStoreException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal class AndroidKeyStoreCypher(
    private val alias: String
) : Cypher {
    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    override fun encrypt(plain: ByteArray): ByteArray {
        try {
            val cypher = Cipher.getInstance("AES/GCM/NoPadding")
            cypher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val iv = cypher.iv
            val cifrado = cypher.doFinal(plain)
            return iv + cifrado
        } catch (e: KeyPermanentlyInvalidatedException) {
            throw SecureStoreException.KeyInvalidated(e)
        } catch (e: Exception) {
            throw SecureStoreException.EncryptionFailed(e)
        }

    }

    override fun decrypt(encripted: ByteArray): ByteArray {
        try {
            val iv = encripted.copyOfRange(0, IV_SIZE)
            val data = encripted.copyOfRange(IV_SIZE, encripted.size)
            val cypher = Cipher.getInstance("AES/GCM/NoPadding")
            cypher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_BITS, iv))
            return cypher.doFinal(data)
        } catch (e: KeyPermanentlyInvalidatedException) {
            throw SecureStoreException.KeyInvalidated(e)
        } catch (e: Exception) {
            throw SecureStoreException.EncryptionFailed(e)
        }
    }

    private fun getOrCreateKey(): SecretKey =
        (keyStore.getKey(alias, null) as? SecretKey) ?: createSecretKey()

    private fun createSecretKey(): SecretKey {
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        val generator =
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(spec)
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val IV_SIZE = 12;
        const val TAG_BITS = 128
    }
}


