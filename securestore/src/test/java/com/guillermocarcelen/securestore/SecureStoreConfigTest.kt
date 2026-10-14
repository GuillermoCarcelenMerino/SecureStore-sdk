package com.guillermocarcelen.securestore

import org.junit.Assert.assertEquals
import org.junit.Test

class SecureStoreConfigTest {

    // The default name decides the file and the Keystore alias: changing it would hide saved data.
    @Test
    fun `the default store name never changes`() {
        assertEquals("secureStore", SecureStoreConfig().name)
    }

    @Test
    fun `a custom name is kept`() {
        assertEquals("tokens", SecureStoreConfig(name = "tokens").name)
    }
}
