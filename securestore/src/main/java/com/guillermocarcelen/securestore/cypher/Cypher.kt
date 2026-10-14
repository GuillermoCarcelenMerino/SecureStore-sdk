package com.guillermocarcelen.securestore.cypher

internal interface Cypher {
    fun encrypt(plain: ByteArray): ByteArray
    fun decrypt(encripted: ByteArray): ByteArray

    /** Deletes the key, so the next [encrypt] creates a new one. Data encrypted before can no longer be read. */
    fun deleteKey()
}