package com.guillermocarcelen.securestore.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * [Storage] backed by a Preferences DataStore file in the app's private folder, one file per [name].
 * DataStore allows a single instance per file: creating two for the same name throws.
 */
internal class DataStoreStorage(
    context: Context,
    name: String,
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
) : Storage {

    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { context.preferencesDataStoreFile(name) },
    )

    override suspend fun read(key: String): String? =
        dataStore.data.first()[stringPreferencesKey(key)]

    override suspend fun write(key: String, value: String) {
        dataStore.edit { it[stringPreferencesKey(key)] = value }
    }

    override suspend fun remove(key: String) {
        dataStore.edit { it.remove(stringPreferencesKey(key)) }
    }

    override suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    override fun observe(key: String): Flow<String?> =
        dataStore.data.map { it[stringPreferencesKey(key)] }.distinctUntilChanged()
}
