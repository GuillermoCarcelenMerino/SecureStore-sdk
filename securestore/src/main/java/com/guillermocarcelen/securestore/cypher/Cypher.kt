package com.guillermocarcelen.securestore.cypher

internal interface Cypher {
    fun encrypt(plain: ByteArray): ByteArray
    fun decrypt(encripted: ByteArray): ByteArray
}