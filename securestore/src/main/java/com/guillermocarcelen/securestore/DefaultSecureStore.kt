package com.guillermocarcelen.securestore

import android.content.Context
import kotlinx.coroutines.flow.Flow

internal class DefaultSecureStore(private val context: Context, private val config: SecureStoreConfig) : SecureStore {
    override suspend fun getString(key: String): Result<String> {
        TODO("Not yet implemented")
    }

    override suspend fun putString(
        key: String,
        value: String
    ): Result<Unit> {
        TODO("Not yet implemented")
    }

    override fun observeString(key: String): Flow<String?> {
        TODO("Not yet implemented")
    }

    override suspend fun remove(key: String): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun clear(): Result<Unit> {
        TODO("Not yet implemented")
    }
}