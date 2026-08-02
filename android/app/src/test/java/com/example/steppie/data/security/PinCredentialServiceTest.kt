package com.example.steppie.data.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinCredentialServiceTest {
    private val credentials = Pbkdf2PinCredentialService()

    @Test
    fun `hash verifies matching pin and rejects a different pin`() {
        val hash = credentials.hash("1234")

        assertTrue(credentials.verify("1234", hash))
        assertFalse(credentials.verify("4321", hash))
    }

    @Test
    fun `hash uses a different salt for the same pin`() {
        val first = credentials.hash("1234")
        val second = credentials.hash("1234")

        assertNotEquals(first, second)
        assertTrue(credentials.verify("1234", first))
        assertTrue(credentials.verify("1234", second))
    }

    @Test
    fun `hash verifies recovery code and rejects a different code`() {
        val hash = credentials.hash("654321")

        assertTrue(credentials.verify("654321", hash))
        assertFalse(credentials.verify("123456", hash))
    }

    @Test
    fun `verifier accepts the existing pbkdf2 storage format`() {
        val existingHash =
            "pbkdf2-sha256\$120000\$AAECAwQFBgcICQoLDA0ODw==\$UHAyHKNNva/pslNx0cD4O8r+Bz+64fInTwCm1saOmJk="

        assertTrue(credentials.verify("1234", existingHash))
        assertFalse(credentials.verify("4321", existingHash))
    }

    @Test
    fun `credential formats distinguish pin and recovery code`() {
        assertTrue(credentials.isValidPin("1234"))
        assertFalse(credentials.isValidPin("12345"))
        assertTrue(credentials.isValidRecoveryCode("654321"))
        assertFalse(credentials.isValidRecoveryCode("65432"))
        assertTrue(credentials.generateRecoveryCode().matches(Regex("^\\d{6}$")))
    }
}
