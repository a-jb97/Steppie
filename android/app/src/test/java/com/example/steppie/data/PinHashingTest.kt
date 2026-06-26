package com.example.steppie.data

import com.example.steppie.data.repository.PinHashing
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHashingTest {
    @Test
    fun hash_verifiesMatchingPinAndRejectsDifferentPin() {
        val hash = PinHashing.hash("1234")

        assertTrue(PinHashing.verify("1234", hash))
        assertFalse(PinHashing.verify("4321", hash))
    }

    @Test
    fun hash_usesDifferentSaltForSamePin() {
        val first = PinHashing.hash("1234")
        val second = PinHashing.hash("1234")

        assertNotEquals(first, second)
        assertTrue(PinHashing.verify("1234", first))
        assertTrue(PinHashing.verify("1234", second))
    }
}
