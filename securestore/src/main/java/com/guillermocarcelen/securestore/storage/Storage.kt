package com.guillermocarcelen.securestore.storage

import kotlinx.coroutines.flow.Flow

/**
 * Persists already-encrypted values as text. It knows nothing about encryption: it only stores
 * and returns the Strings it is given. Implementations throw IOException when the disk fails.
 */
internal interface Storage {
    suspend fun read(key: String): String?
    suspend fun write(key: String, value: String)
    suspend fun remove(key: String)
    suspend fun clear()
    fun observe(key: String): Flow<String?>
}
