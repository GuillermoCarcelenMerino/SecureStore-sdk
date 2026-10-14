package com.guillermocarcelen.securestore.storage

import android.content.Context
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Runs on a device: DataStore writes real files. Each test uses fresh file names because DataStore
 * allows only one instance per file, and the files are deleted afterwards.
 */
@RunWith(AndroidJUnit4::class)
class DataStoreStorageTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val names = mutableListOf<String>()

    private fun newStorage(): DataStoreStorage {
        val name = "test_${UUID.randomUUID()}"
        names += name
        return DataStoreStorage(context, name, scope)
    }

    @After
    fun deleteFiles() {
        scope.cancel()
        names.forEach { context.preferencesDataStoreFile(it).delete() }
    }

    @Test
    fun a_written_value_can_be_read() = runTest {
        val storage = newStorage()

        storage.write("token", "abc")

        assertEquals("abc", storage.read("token"))
    }

    @Test
    fun a_missing_key_returns_null() = runTest {
        assertNull(newStorage().read("token"))
    }

    @Test
    fun remove_deletes_only_that_key() = runTest {
        val storage = newStorage()
        storage.write("token", "abc")
        storage.write("user", "guillermo")

        storage.remove("token")

        assertNull(storage.read("token"))
        assertEquals("guillermo", storage.read("user"))
    }

    @Test
    fun clear_deletes_every_key() = runTest {
        val storage = newStorage()
        storage.write("token", "abc")
        storage.write("user", "guillermo")

        storage.clear()

        assertNull(storage.read("token"))
        assertNull(storage.read("user"))
    }

    @Test
    fun observe_emits_the_written_value() = runTest {
        val storage = newStorage()

        storage.write("token", "abc")

        assertEquals("abc", storage.observe("token").first())
    }

    @Test
    fun the_value_is_written_to_the_file() = runTest {
        val storage = newStorage()

        storage.write("token", "abc")

        assertEquals(true, context.preferencesDataStoreFile(names.last()).exists())
    }

    @Test
    fun stores_with_different_names_do_not_mix() = runTest {
        val first = newStorage()
        val second = newStorage()

        first.write("token", "abc")

        assertNull(second.read("token"))
    }
}
