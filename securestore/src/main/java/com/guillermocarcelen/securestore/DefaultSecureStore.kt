package com.guillermocarcelen.securestore

import android.content.Context
import com.guillermocarcelen.securestore.cypher.Cypher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.io.encoding.Base64

/**
 * Temporary in-memory implementation: values live only while the process is alive and are not
 * encrypted yet. Phase B replaces the map with the Keystore cipher and DataStore.
 */
internal class DefaultSecureStore(
    private val context: Context,
    private val config: SecureStoreConfig,
    private val cypher: Cypher
) : SecureStore {

    private val data = MutableStateFlow<Map<String, String>>(emptyMap())

    override suspend fun getString(key: String): Result<String?> =
        storageOperation { data.value[key]?.let { decryptText(it) } }

    override suspend fun putString(key: String, value: String): Result<Unit> =
        storageOperation {
            val encrypted = encryptToText(value)
            data.update { it + (key to encrypted) }
        }

    override fun observeString(key: String): Flow<String?> =
        data.map { it[key] }.distinctUntilChanged().map {
            it?.let { text ->
                decryptText(text)
            }
        }

    override suspend fun remove(key: String): Result<Unit> =
        storageOperation { data.update { it - key } }

    override suspend fun clear(): Result<Unit> =
        storageOperation { data.value = emptyMap() }

    private inline fun <T> storageOperation(block: () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: SecureStoreException) {
            Result.failure(e)                                    // ya es de las nuestras
        } catch (e: Exception) {
            Result.failure(SecureStoreException.StorageFailure(e))
        }

    private fun encryptToText(data: String): String =
        Base64.encode(cypher.encrypt(data.encodeToByteArray()))

    private fun decryptText(encoded: String): String =
        cypher.decrypt(Base64.decode(encoded)).decodeToString()
}
