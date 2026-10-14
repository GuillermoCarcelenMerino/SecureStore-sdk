package com.guillermocarcelen.securestore

import android.content.Context
import com.guillermocarcelen.securestore.cypher.AndroidKeyStoreCypher
import com.guillermocarcelen.securestore.storage.DataStoreStorage
import kotlinx.coroutines.flow.Flow

public interface SecureStore {
    public suspend fun getString(key: String): Result<String?>
    public suspend fun putString(key: String, value: String): Result<Unit>
    public fun observeString(key: String): Flow<String?>
    public suspend fun remove(key: String): Result<Unit>
    public suspend fun clear(): Result<Unit>

    public companion object {
        /**
         * Creates the store named [SecureStoreConfig.name]. Create one instance per name and reuse it
         * (for example as an app-wide singleton): two instances for the same name are not supported.
         */
        public fun init(
            context: Context,
            config: SecureStoreConfig = SecureStoreConfig()
        ): SecureStore =
            DefaultSecureStore(
                storage = DataStoreStorage(context.applicationContext, config.name),
                cypher = AndroidKeyStoreCypher(alias = "securestore_${config.name}"),
            )
    }
}

public class SecureStoreConfig(public val name: String = "secureStore")

/**
 * Errors returned by [SecureStore]. New subtypes may be added in future
 * versions: always include an `else` branch when matching with `when`.
 */
public sealed class SecureStoreException(message: String, cause: Throwable) :
    Exception(message, cause) {
    public class EncryptionFailed internal constructor(cause: Throwable) :
        SecureStoreException("Encryption failed", cause)

    public class KeyInvalidated internal constructor(cause: Throwable) :
        SecureStoreException("The encryption key is no longer valid", cause)

    public class StorageFailure internal constructor(cause: Throwable) :
        SecureStoreException("Could not read or write the store", cause)
}