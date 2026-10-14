package com.guillermocarcelen.securestore.storage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory [Storage] for unit tests. [values] exposes what was stored; [failWith] makes calls throw. */
internal class FakeStorage : Storage {

    val values = MutableStateFlow<Map<String, String>>(emptyMap())
    var failWith: Exception? = null

    override suspend fun read(key: String): String? {
        failWith?.let { throw it }
        return values.value[key]
    }

    override suspend fun write(key: String, value: String) {
        failWith?.let { throw it }
        values.update { it + (key to value) }
    }

    override suspend fun remove(key: String) {
        failWith?.let { throw it }
        values.update { it - key }
    }

    override suspend fun clear() {
        failWith?.let { throw it }
        values.value = emptyMap()
    }

    override fun observe(key: String): Flow<String?> =
        values.map { it[key] }.distinctUntilChanged()
}
