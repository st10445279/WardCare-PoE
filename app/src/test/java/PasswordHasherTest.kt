package com.wardcare.app.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {

    private val password = "Nurse@1234"

    @Test
    fun hashPassword_doesNotStorePlainText() {
        val hash = PasswordHasher.hashPassword(password)

        assertNotEquals(password, hash)
        assertTrue(hash.startsWith("\$2"))
        assertTrue(hash.contains("\$10\$"))
    }

    @Test
    fun hashPassword_samePasswordGivesDifferentHashes() {
        val first = PasswordHasher.hashPassword(password)
        val second = PasswordHasher.hashPassword(password)

        assertNotEquals(first, second)
    }

    @Test
    fun verifyPassword_correctPassword_returnsTrue() {
        val hash = PasswordHasher.hashPassword(password)

        assertTrue(PasswordHasher.verifyPassword(password, hash))
    }

    @Test
    fun verifyPassword_wrongPassword_returnsFalse() {
        val hash = PasswordHasher.hashPassword(password)

        assertFalse(PasswordHasher.verifyPassword("Nurse@12345", hash))
    }

    @Test
    fun verifyPassword_isCaseSensitive() {
        val hash = PasswordHasher.hashPassword(password)

        assertFalse(PasswordHasher.verifyPassword(password.lowercase(), hash))
    }

    @Test
    fun verifyPassword_emptyHash_returnsFalse() {
        // Google sign-in accounts are stored with an empty hash
        assertFalse(PasswordHasher.verifyPassword(password, ""))
    }

    @Test
    fun verifyPassword_emptyPasswordAgainstRealHash_returnsFalse() {
        val hash = PasswordHasher.hashPassword(password)

        assertFalse(PasswordHasher.verifyPassword("", hash))
    }
}