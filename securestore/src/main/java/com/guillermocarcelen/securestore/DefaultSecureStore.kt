package com.guillermocarcelen.securestore

import com.guillermocarcelen.securestore.cypher.Cypher
import com.guillermocarcelen.securestore.storage.Storage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlin.io.encoding.Base64

/**
 * Encrypts each value with [cypher], encodes it as Base64 text and keeps it in [storage].
 * Every failure reaches the caller as a [SecureStoreException].
 */
internal class DefaultSecureStore(
    private val storage: Storage,
    private val cypher: Cypher,
) : SecureStore {

    override suspend fun getString(key: String): Result<String?> =
        storageOperation { storage.read(key)?.let { decryptText(it) } }

    override suspend fun putString(key: String, value: String): Result<Unit> =
        storageOperation { storage.write(key, encryptToText(value)) }

    override fun observeString(key: String): Flow<String?> =
        storage.observe(key)
            .map { encoded -> encoded?.let { decryptText(it) } }
            .catch { throw it.toSecureStoreException() }

    override suspend fun remove(key: String): Result<Unit> =
        storageOperation { storage.remove(key) }

    override suspend fun clear(): Result<Unit> =
        storageOperation { storage.clear() }

    private inline fun <T> storageOperation(block: () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e                                           // a cancelled coroutine must stay cancelled
        } catch (e: Exception) {
            Result.failure(e.toSecureStoreException())
        }

    private fun Throwable.toSecureStoreException(): Throwable = when (this) {
        is CancellationException, is SecureStoreException -> this
        else -> SecureStoreException.StorageFailure(this)
    }

    private fun encryptToText(plain: String): String =
        Base64.encode(cypher.encrypt(plain.encodeToByteArray()))

    private fun decryptText(encoded: String): String =
        cypher.decrypt(Base64.decode(encoded)).decodeToString()
}
