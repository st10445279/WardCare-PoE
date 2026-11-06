package com.wardcare.app.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InputValidatorTest {

    @Test
    fun validEmails_areAccepted() {
        assertTrue(InputValidator.isValidEmail("nurse@hospital.co.za"))
        assertTrue(InputValidator.isValidEmail("  nurse.one+ward@mail.com  "))
    }

    @Test
    fun invalidEmails_areRejected() {
        assertFalse(InputValidator.isValidEmail(""))
        assertFalse(InputValidator.isValidEmail("nurse"))
        assertFalse(InputValidator.isValidEmail("nurse@"))
        assertFalse(InputValidator.isValidEmail("nurse@hospital"))
    }

    @Test
    fun password_needsEightCharsLetterAndNumber() {
        assertTrue(InputValidator.isValidPassword("Ward1234"))
        assertFalse(InputValidator.isValidPassword("short1"))
        assertFalse(InputValidator.isValidPassword("onlyletters"))
        assertFalse(InputValidator.isValidPassword("12345678"))
    }

    @Test
    fun name_mustBeTwoToSixtyCharacters() {
        assertTrue(InputValidator.isValidName("Li"))
        assertFalse(InputValidator.isValidName("A"))
        assertFalse(InputValidator.isValidName("x".repeat(61)))
    }

    @Test
    fun phone_isOptionalButMustBeValidWhenGiven() {
        assertTrue(InputValidator.isValidPhone(""))
        assertTrue(InputValidator.isValidPhone("+27821234567"))
        assertFalse(InputValidator.isValidPhone("abc"))
        assertFalse(InputValidator.isValidPhone("123"))
    }

    @Test
    fun passwordsMatch_comparesExactly() {
        assertTrue(InputValidator.passwordsMatch("Ward1234", "Ward1234"))
        assertFalse(InputValidator.passwordsMatch("Ward1234", "ward1234"))
    }
}