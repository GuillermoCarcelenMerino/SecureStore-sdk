package com.guillermocarcelen.securestore

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Temporary in-memory implementation: values live only while the process is alive and are not
 * encrypted yet. Phase B replaces the map with the Keystore cipher and DataStore.
 */
internal class DefaultSecureStore(
    private val context: Context,
    private val config: SecureStoreConfig
) : SecureStore {

    private val data = MutableStateFlow<Map<String, String>>(emptyMap())

    override suspend fun getString(key: String): Result<String?> =
        Result.success(data.value[key])

    override suspend fun putString(key: String, value: String): Result<Unit> =
        storageOperation { data.update { it + (key to value) } }

    override fun observeString(key: String): Flow<String?> =
        data.map { it[key] }.distinctUntilChanged()

    override suspend fun remove(key: String): Result<Unit> =
        storageOperation { data.update { it - key } }

    override suspend fun clear(): Result<Unit> =
        storageOperation { data.value = emptyMap() }

    private inline fun storageOperation(block: () -> Unit): Result<Unit> =
        try {
            block()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(SecureStoreException.StorageFailure(e))
        }
}
