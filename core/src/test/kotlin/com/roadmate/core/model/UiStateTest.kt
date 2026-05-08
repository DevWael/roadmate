package com.roadmate.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("UiState sealed interface")
class UiStateTest {

    @Test
    fun `Loading is a UiState`() {
        val state: UiState<String> = UiState.Loading
        assertInstanceOf(UiState::class.java, state)
    }

    @Test
    fun `Success carries data`() {
        val state: UiState<String> = UiState.Success("hello")
        assertInstanceOf(UiState.Success::class.java, state)
        assertEquals("hello", (state as UiState.Success).data)
    }

    @Test
    fun `Error carries message`() {
        val state: UiState<String> = UiState.Error("fail")
        assertInstanceOf(UiState.Error::class.java, state)
        assertEquals("fail", (state as UiState.Error).message)
    }

    @Test
    fun `Error cause defaults to null`() {
        val state = UiState.Error("fail")
        assertNull(state.cause)
    }

    @Test
    fun `Error preserves throwable cause`() {
        val ex = RuntimeException("boom")
        val state = UiState.Error("fail", cause = ex)
        assertSame(ex, state.cause)
    }

    @Test
    fun `when exhaustive over all subtypes compiles`() {
        val state: UiState<Int> = UiState.Success(42)
        val result = when (state) {
            is UiState.Loading -> "loading"
            is UiState.Success -> "success: ${state.data}"
            is UiState.Error -> "error: ${state.message}"
        }
        assertTrue(result.startsWith("success"))
    }
}
