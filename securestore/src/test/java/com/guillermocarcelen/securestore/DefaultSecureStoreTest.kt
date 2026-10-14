package com.guillermocarcelen.securestore

import android.content.Context
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DefaultSecureStoreTest {


    private val context: Context = mockk(relaxed = true)
    private val store = DefaultSecureStore(context, SecureStoreConfig())

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
}
