package com.guillermocarcelen.securestore.cypher

import com.guillermocarcelen.securestore.SecureStoreException

/**
 * Reversible stand-in for [AndroidKeyStoreCypher], which needs a device. It flips every byte, so the
 * stored value never equals the plain one, and it records what it was asked to encrypt.
 * Set [failWith] to make the next calls throw that error. Like a real invalidated key, a
 * [SecureStoreException.KeyInvalidated] error keeps failing until [deleteKey] is called.
 */
internal class FakeCypher : Cypher {

    val encryptedInputs = mutableListOf<String>()
    var failWith: Exception? = null
    var deletedKeys = 0
        private set

    override fun deleteKey() {
        deletedKeys++
        if (failWith is SecureStoreException.KeyInvalidated) failWith = null
    }

    override fun encrypt(plain: ByteArray): ByteArray {
        failWith?.let { throw it }
        encryptedInputs += plain.decodeToString()
        return plain.flip()
    }

    override fun decrypt(encripted: ByteArray): ByteArray {
        failWith?.let { throw it }
        return encripted.flip()
    }

    private fun ByteArray.flip(): ByteArray = ByteArray(size) { (this[it].toInt() xor 0xFF).toByte() }
}
