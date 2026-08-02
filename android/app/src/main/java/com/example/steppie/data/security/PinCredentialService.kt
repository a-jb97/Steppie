package com.example.steppie.data.security

import java.security.SecureRandom
import java.security.spec.KeySpec
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

interface PinCredentialService {
    fun isValidPin(value: String): Boolean
    fun isValidRecoveryCode(value: String): Boolean
    fun hash(value: String): String
    fun verify(value: String, storedHash: String): Boolean
    fun generateRecoveryCode(): String
}

class Pbkdf2PinCredentialService(
    private val secureRandom: SecureRandom = SecureRandom(),
) : PinCredentialService {
    override fun isValidPin(value: String): Boolean = value.matches(PinPattern)

    override fun isValidRecoveryCode(value: String): Boolean = value.matches(RecoveryCodePattern)

    override fun hash(value: String): String {
        require(isValidPin(value) || isValidRecoveryCode(value)) {
            "PIN values must be 4 digits and recovery codes must be 6 digits."
        }
        val salt = ByteArray(SaltLengthBytes).also(secureRandom::nextBytes)
        val encoded = derive(value, salt, Iterations)
        return listOf(
            StorageAlgorithm,
            Iterations.toString(),
            Base64.getEncoder().encodeToString(salt),
            Base64.getEncoder().encodeToString(encoded),
        ).joinToString("$")
    }

    override fun verify(value: String, storedHash: String): Boolean {
        val parts = storedHash.split('$')
        if (parts.size != 4 || parts[0] != StorageAlgorithm) return false
        val iterations = parts[1].toIntOrNull() ?: return false
        val salt = runCatching { Base64.getDecoder().decode(parts[2]) }.getOrNull() ?: return false
        val expected = runCatching { Base64.getDecoder().decode(parts[3]) }.getOrNull() ?: return false
        val actual = derive(value, salt, iterations)
        return constantTimeEquals(expected, actual)
    }

    override fun generateRecoveryCode(): String =
        (secureRandom.nextInt(RecoveryCodeRange) + RecoveryCodeMinimum).toString()

    private fun derive(value: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec: KeySpec = PBEKeySpec(value.toCharArray(), salt, iterations, KeyLengthBits)
        return SecretKeyFactory.getInstance(CryptoAlgorithm).generateSecret(spec).encoded
    }

    private fun constantTimeEquals(left: ByteArray, right: ByteArray): Boolean {
        if (left.size != right.size) return false
        var result = 0
        left.indices.forEach { index -> result = result or (left[index].toInt() xor right[index].toInt()) }
        return result == 0
    }

    private companion object {
        const val CryptoAlgorithm = "PBKDF2WithHmacSHA256"
        const val StorageAlgorithm = "pbkdf2-sha256"
        const val Iterations = 120_000
        const val KeyLengthBits = 256
        const val SaltLengthBytes = 16
        const val RecoveryCodeMinimum = 100_000
        const val RecoveryCodeRange = 900_000
        val PinPattern = Regex("^\\d{4}$")
        val RecoveryCodePattern = Regex("^\\d{6}$")
    }
}
