package com.guillermocarcelen.securestore

import android.content.Context
import com.guillermocarcelen.securestore.cypher.FakeCypher
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultSecureStoreTest {

    private val context: Context = mockk(relaxed = true)
    private val cypher = FakeCypher()
    private val store = DefaultSecureStore(context, SecureStoreConfig(), cypher)

    @Test
    fun `when user add a value it is saved`() = runTest {
        store.putString("token", "abc")

        assertEquals("abc", store.getString("token").getOrThrow())
    }

    @Test
    fun `a missing key returns null`() = runTest {
        assertNull(store.getString("token").getOrThrow())
    }

    @Test
    fun `saving the same key twice keeps the last value`() = runTest {
        store.putString("token", "abc")
        store.putString("token", "xyz")

        assertEquals("xyz", store.getString("token").getOrThrow())
    }

    @Test
    fun `remove deletes only that key`() = runTest {
        store.putString("token", "abc")
        store.putString("user", "guillermo")

        store.remove("token")

        assertNull(store.getString("token").getOrThrow())
        assertEquals("guillermo", store.getString("user").getOrThrow())
    }

    @Test
    fun `clear deletes every key`() = runTest {
        store.putString("token", "abc")
        store.putString("user", "guillermo")

        store.clear()

        assertNull(store.getString("token").getOrThrow())
        assertNull(store.getString("user").getOrThrow())
    }

    @Test
    fun `observe emits the current value of the key`() = runTest {
        assertNull(store.observeString("token").first())

        store.putString("token", "abc")

        assertEquals("abc", store.observeString("token").first())
    }

    @Test
    fun `saving a value encrypts it`() = runTest {
        store.putString("token", "abc")

        assertEquals(listOf("abc"), cypher.encryptedInputs)
    }

    @Test
    fun `accents and emojis survive encryption`() = runTest {
        store.putString("password", "contraseña🔐")

        assertEquals("contraseña🔐", store.getString("password").getOrThrow())
    }

    @Test
    fun `an encryption error when saving returns EncryptionFailed and stores nothing`() = runTest {
        cypher.failWith = SecureStoreException.EncryptionFailed(IllegalStateException())

        val result = store.putString("token", "abc")

        assertTrue(result.exceptionOrNull() is SecureStoreException.EncryptionFailed)
        cypher.failWith = null
        assertNull(store.getString("token").getOrThrow())
    }

    @Test
    fun `an invalidated key when reading returns KeyInvalidated`() = runTest {
        store.putString("token", "abc")
        cypher.failWith = SecureStoreException.KeyInvalidated(IllegalStateException())

        val result = store.getString("token")

        assertTrue(result.exceptionOrNull() is SecureStoreException.KeyInvalidated)
    }

    @Test
    fun `an unexpected error is returned as StorageFailure`() = runTest {
        cypher.failWith = IllegalStateException("disk full")

        val result = store.putString("token", "abc")

        assertTrue(result.exceptionOrNull() is SecureStoreException.StorageFailure)
    }
}
