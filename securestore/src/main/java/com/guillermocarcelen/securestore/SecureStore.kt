package com.guillermocarcelen.securestore

import android.content.Context
import kotlinx.coroutines.flow.Flow

public interface SecureStore {
    public suspend fun getString(key: String): Result<String?>
    public suspend fun putString(key: String, value: String): Result<Unit>
    public fun observeString(key: String): Flow<String?>
    public suspend fun remove(key : String): Result<Unit>
    public suspend fun clear() : Result<Unit>

    public companion object{
        public fun init(context: Context, config : SecureStoreConfig = SecureStoreConfig() ) : SecureStore =
            DefaultSecureStore(context.applicationContext, config)
    }
}

public class SecureStoreConfig(public val name : String = "secureStore")

/**
 * Errors returned by [SecureStore]. New subtypes may be added in future
 * versions: always include an `else` branch when matching with `when`.
 */
public sealed class SecureStoreException(message: String, cause: Throwable?) : Exception(message, cause)
{
    public class EncryptionFailed(cause: Throwable) : SecureStoreException("Encryption failed", cause)
    public class KeyInvalidated(cause: Throwable) : SecureStoreException("KeyInvalidated", cause)
    public class StorageFailure(cause: Throwable) : SecureStoreException("Storage  failed", cause)


}