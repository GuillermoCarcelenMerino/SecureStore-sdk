package com.guillermocarcelen.securestore

import android.content.Context
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Uses the public API with the real Keystore cipher and the real DataStore file. */
@RunWith(AndroidJUnit4::class)
class SecureStoreIntegrationTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun a_saved_value_is_read_back_and_the_file_does_not_contain_it() = runTest {
        val name = "integration_${UUID.randomUUID()}"
        val store = SecureStore.init(context, SecureStoreConfig(name))

        store.putString("token", "my-secret-token").getOrThrow()

        assertEquals("my-secret-token", store.getString("token").getOrThrow())
        val file = context.preferencesDataStoreFile(name)
        assertFalse(file.readText(Charsets.ISO_8859_1).contains("my-secret-token"))
    }
}
