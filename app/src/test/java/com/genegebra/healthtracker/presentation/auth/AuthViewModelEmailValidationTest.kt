package com.genegebra.healthtracker.presentation.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthViewModelEmailValidationTest {

    @Test
    fun `valid email passes`() {
        assertTrue(AuthViewModel.isValidEmail("user@example.com"))
    }

    @Test
    fun `missing domain extension fails`() {
        assertFalse(AuthViewModel.isValidEmail("user@example"))
    }

    @Test
    fun `format-valid address with uncommon tld passes`() {
        // A regex cannot detect gmail.coma vs gmail.com — both are syntactically valid.
        // The real guard against this typo is showing the email on the verification screen.
        assertTrue(AuthViewModel.isValidEmail("user@gmail.coma"))
    }

    @Test
    fun `missing at-sign fails`() {
        assertFalse(AuthViewModel.isValidEmail("userexample.com"))
    }

    @Test
    fun `empty string fails`() {
        assertFalse(AuthViewModel.isValidEmail(""))
    }

    @Test
    fun `leading and trailing whitespace is trimmed`() {
        assertTrue(AuthViewModel.isValidEmail("  user@example.com  "))
    }

    @Test
    fun `subdomain address passes`() {
        assertTrue(AuthViewModel.isValidEmail("user@mail.example.co.uk"))
    }
}
