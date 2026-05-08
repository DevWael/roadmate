package com.roadmate.headunit.viewmodel

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("WelcomeFormState")
class WelcomeFormStateTest {

    private lateinit var validForm: WelcomeFormState

    @BeforeEach
    fun setUp() {
        validForm = WelcomeFormState(
            name = "My Car",
            odometer = "50000",
        )
    }

    @Nested
    @DisplayName("validation")
    inner class Validation {

        @Test
        fun `valid form has no errors`() {
            val result = validForm.validate()
            assertTrue(result.errors.isEmpty())
        }

        @Test
        fun `isValid returns true for valid form`() {
            assertTrue(validForm.isValid())
        }

        @Test
        fun `blank name produces error`() {
            val form = validForm.copy(name = "")
            val result = form.validate()
            assertTrue(result.errors.containsKey("name"))
            assertEquals("Required", result.errors["name"])
        }

        @Test
        fun `whitespace-only name produces error`() {
            val form = validForm.copy(name = "   ")
            assertTrue(form.validate().errors.containsKey("name"))
        }

        @Test
        fun `blank odometer produces error`() {
            val form = validForm.copy(odometer = "")
            assertTrue(form.validate().errors.containsKey("odometer"))
            assertEquals("Required", form.validate().errors["odometer"])
        }

        @Test
        fun `non-numeric odometer produces error`() {
            val form = validForm.copy(odometer = "abc")
            assertEquals("Invalid", form.validate().errors["odometer"])
        }

        @Test
        fun `negative odometer produces error`() {
            val form = validForm.copy(odometer = "-100")
            assertEquals("Invalid", form.validate().errors["odometer"])
        }

        @Test
        fun `zero odometer is valid`() {
            val form = validForm.copy(odometer = "0")
            assertFalse(form.validate().errors.containsKey("odometer"))
        }

        @Test
        fun `decimal odometer is valid`() {
            val form = validForm.copy(odometer = "50000.5")
            assertFalse(form.validate().errors.containsKey("odometer"))
        }

        @Test
        fun `multiple errors reported simultaneously`() {
            val form = WelcomeFormState()
            val result = form.validate()
            assertEquals(2, result.errors.size)
            assertTrue(result.errors.containsKey("name"))
            assertTrue(result.errors.containsKey("odometer"))
        }
    }

    @Nested
    @DisplayName("defaults")
    inner class Defaults {

        @Test
        fun `default form has empty name`() {
            assertEquals("", WelcomeFormState().name)
        }

        @Test
        fun `default form has empty odometer`() {
            assertEquals("", WelcomeFormState().odometer)
        }

        @Test
        fun `default form has no errors`() {
            assertTrue(WelcomeFormState().errors.isEmpty())
        }
    }
}
