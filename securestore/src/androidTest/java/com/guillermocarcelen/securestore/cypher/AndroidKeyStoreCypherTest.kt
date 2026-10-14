package com.guillermocarcelen.securestore.cypher

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guillermocarcelen.securestore.SecureStoreException
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore

/**
 * Runs on a device or emulator: the Android Keystore does not exist on the JVM.
 * Method names use underscores because spaces are not allowed in DEX method names below API 30.
 */
@RunWith(AndroidJUnit4::class)
class AndroidKeyStoreCypherTest {

    private val cypher: Cypher = AndroidKeyStoreCypher(ALIAS)
    private val plain = "value on test".toByteArray(Charsets.UTF_8)

    // Each test starts without keys, so the result of one test never depends on another.
    @After
    fun deleteKeys() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        keyStore.deleteEntry(ALIAS)
        keyStore.deleteEntry(OTHER_ALIAS)
    }

    @Test
    fun decrypting_an_encrypted_value_returns_the_original() {
        assertArrayEquals(plain, cypher.decrypt(cypher.encrypt(plain)))
    }

    @Test
    fun encrypting_the_same_value_twice_gives_different_results() {
        // ByteArrays must be compared by content: assertNotEquals would compare references.
        assertFalse(cypher.encrypt(plain).contentEquals(cypher.encrypt(plain)))
    }

    @Test
    fun the_encrypted_value_does_not_contain_the_original() {
        val encrypted = cypher.encrypt(plain)

        assertFalse(encrypted.toList().windowed(plain.size).any { it == plain.toList() })
    }

    @Test
    fun the_encrypted_value_is_iv_plus_data_plus_tag() {
        val encrypted = cypher.encrypt(plain)

        assertEquals(IV_BYTES + plain.size + TAG_BYTES, encrypted.size)
    }

    @Test
    fun accents_and_emojis_survive_the_round_trip() {
        val text = "contraseña🔐"

        val decrypted = cypher.decrypt(cypher.encrypt(text.toByteArray(Charsets.UTF_8)))

        assertEquals(text, String(decrypted, Charsets.UTF_8))
    }

    @Test
    fun an_empty_value_can_be_encrypted_and_decrypted() {
        assertArrayEquals(ByteArray(0), cypher.decrypt(cypher.encrypt(ByteArray(0))))
    }

    @Test
    fun a_new_instance_with_the_same_alias_decrypts_previous_data() {
        // Simulates closing and reopening the app: the key stays in the Keystore.
        val encrypted = cypher.encrypt(plain)

        val reopened = AndroidKeyStoreCypher(ALIAS)

        assertArrayEquals(plain, reopened.decrypt(encrypted))
    }

    @Test
    fun a_different_alias_cannot_decrypt_the_data() {
        val encrypted = cypher.encrypt(plain)

        assertThrows(SecureStoreException.EncryptionFailed::class.java) {
            AndroidKeyStoreCypher(OTHER_ALIAS).decrypt(encrypted)
        }
    }

    @Test
    fun tampered_data_fails_to_decrypt() {
        val encrypted = cypher.encrypt(plain)
        encrypted[encrypted.size - 1] = (encrypted[encrypted.size - 1] + 1).toByte()

        assertThrows(SecureStoreException.EncryptionFailed::class.java) {
            cypher.decrypt(encrypted)
        }
    }

    @Test
    fun data_shorter_than_the_iv_fails_to_decrypt() {
        assertThrows(SecureStoreException.EncryptionFailed::class.java) {
            cypher.decrypt(ByteArray(5))
        }
    }

    @Test
    fun after_deleting_the_key_old_data_is_unreadable_and_a_new_key_works() {
        val oldData = cypher.encrypt(plain)

        cypher.deleteKey()

        assertThrows(SecureStoreException.EncryptionFailed::class.java) { cypher.decrypt(oldData) }
        assertArrayEquals(plain, cypher.decrypt(cypher.encrypt(plain)))
    }

    private companion object {
        const val ALIAS = "securestore_test"
        const val OTHER_ALIAS = "securestore_test_other"
        const val IV_BYTES = 12
        const val TAG_BYTES = 16
    }
}
