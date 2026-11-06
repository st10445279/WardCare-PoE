package com.wardcare.app.util

/** Input checks shared by the Login/Register screens. Pure Kotlin so it is unit tested. */
object InputValidator {
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val PASSWORD_REGEX = Regex("^(?=.*[A-Za-z])(?=.*[0-9]).{8,}$")
    private val PHONE_REGEX = Regex("^[+]?[0-9]{7,15}$")

    fun isValidEmail(email: String) = EMAIL_REGEX.matches(email.trim())

    /** At least 8 characters with 1 letter and 1 number. */
    fun isValidPassword(password: String) = PASSWORD_REGEX.matches(password)

    fun isValidName(name: String) = name.trim().length in 2..60

    /** Phone is optional; if given it must look like +27821234567. */
    fun isValidPhone(phone: String) = phone.isBlank() || PHONE_REGEX.matches(phone.trim())

    fun passwordsMatch(password: String, confirm: String) = password == confirm
}