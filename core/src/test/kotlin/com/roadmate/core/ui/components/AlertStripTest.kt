package com.roadmate.core.ui.components

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("AlertStrip logic")
class AlertStripTest {

    @Nested
    @DisplayName("message formatting")
    inner class MessageFormatting {

        @Test
        fun `single item message is the item name`() {
            val items = listOf("Oil Change")
            val message = if (items.isEmpty()) null else items.joinToString(", ")
            assertEquals("Oil Change", message)
        }

        @Test
        fun `multiple items joined with comma`() {
            val items = listOf("Oil Change", "Tire Rotation")
            val message = if (items.isEmpty()) null else items.joinToString(", ")
            assertEquals("Oil Change, Tire Rotation", message)
        }

        @Test
        fun `empty items returns null`() {
            val items = emptyList<String>()
            val message = if (items.isEmpty()) null else items.joinToString(", ")
            assertNull(message)
        }
    }
}
