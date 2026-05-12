package com.roadmate.core.sync.protocol

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

@DisplayName("MessageSerializer")
class MessageSerializerTest {

    private lateinit var serializer: MessageSerializer

    @BeforeEach
    fun setUp() {
        serializer = MessageSerializer()
    }

    @Nested
    @DisplayName("writeMessage")
    inner class WriteMessage {

        @Test
        fun `writes 4-byte big-endian length header followed by UTF-8 JSON`() {
            val json = """{"type":"test"}"""
            val output = ByteArrayOutputStream()
            serializer.writeMessage(output, json)
            val bytes = output.toByteArray()

            val headerLength = ByteBuffer.wrap(bytes, 0, 4).int
            assertEquals(json.toByteArray(Charsets.UTF_8).size, headerLength)
            assertEquals(json, String(bytes, 4, bytes.size - 4, Charsets.UTF_8))
        }

        @Test
        fun `handles empty JSON string`() {
            val output = ByteArrayOutputStream()
            serializer.writeMessage(output, "")
            val bytes = output.toByteArray()
            assertEquals(4, bytes.size)
            assertEquals(0, ByteBuffer.wrap(bytes).int)
        }

        @Test
        fun `handles multi-byte UTF-8 characters`() {
            val json = """{"name":"日本語テスト"}"""
            val output = ByteArrayOutputStream()
            serializer.writeMessage(output, json)
            val bytes = output.toByteArray()
            val length = ByteBuffer.wrap(bytes, 0, 4).int
            assertEquals(json, String(bytes, 4, length, Charsets.UTF_8))
        }

        @Test
        fun `flushes output stream after writing`() {
            val output = ByteArrayOutputStream()
            serializer.writeMessage(output, """{"test":true}""")
            assertTrue(output.toByteArray().isNotEmpty())
        }
    }

    @Nested
    @DisplayName("readMessage")
    inner class ReadMessage {

        @Test
        fun `reads 4-byte header and returns correct JSON string`() {
            val json = """{"type":"sync_status"}"""
            val jsonBytes = json.toByteArray(Charsets.UTF_8)
            val output = ByteArrayOutputStream()
            output.write(ByteBuffer.allocate(4).putInt(jsonBytes.size).array())
            output.write(jsonBytes)

            val result = serializer.readMessage(ByteArrayInputStream(output.toByteArray()))
            assertEquals(json, result)
        }

        @Test
        fun `handles empty payload`() {
            val output = ByteArrayOutputStream()
            output.write(ByteBuffer.allocate(4).putInt(0).array())

            val result = serializer.readMessage(ByteArrayInputStream(output.toByteArray()))
            assertEquals("", result)
        }

        @Test
        fun `handles multi-byte UTF-8 characters`() {
            val json = """{"name":"العربية"}"""
            val jsonBytes = json.toByteArray(Charsets.UTF_8)
            val output = ByteArrayOutputStream()
            output.write(ByteBuffer.allocate(4).putInt(jsonBytes.size).array())
            output.write(jsonBytes)

            val result = serializer.readMessage(ByteArrayInputStream(output.toByteArray()))
            assertEquals(json, result)
        }
    }

    @Nested
    @DisplayName("round-trip")
    inner class RoundTrip {

        @Test
        fun `write then read returns original JSON`() {
            val json = """{"entityType":"trip","data":[{"id":"t-1"}],"messageId":"uuid-123"}"""
            val output = ByteArrayOutputStream()
            serializer.writeMessage(output, json)
            val result = serializer.readMessage(ByteArrayInputStream(output.toByteArray()))
            assertEquals(json, result)
        }

        @Test
        fun `round-trip with large payload`() {
            val largeData = (1..1000).joinToString(",") { """{"id":"item-$it"}""" }
            val json = """{"data":[$largeData]}"""
            val output = ByteArrayOutputStream()
            serializer.writeMessage(output, json)
            val result = serializer.readMessage(ByteArrayInputStream(output.toByteArray()))
            assertEquals(json, result)
        }
    }

    @Nested
    @DisplayName("length guards")
    inner class LengthGuards {

        @Test
        fun `rejects negative length header`() {
            val output = ByteArrayOutputStream()
            output.write(ByteBuffer.allocate(4).putInt(-1).array())
            output.write(ByteArray(0))

            val exception = org.junit.jupiter.api.assertThrows<java.io.IOException> {
                serializer.readMessage(ByteArrayInputStream(output.toByteArray()))
            }
            assertTrue(exception.message!!.contains("Invalid message length"))
        }

        @Test
        fun `rejects length exceeding max message size`() {
            val tooLarge = MessageSerializer.MAX_MESSAGE_SIZE + 1
            val output = ByteArrayOutputStream()
            output.write(ByteBuffer.allocate(4).putInt(tooLarge).array())

            val exception = org.junit.jupiter.api.assertThrows<java.io.IOException> {
                serializer.readMessage(ByteArrayInputStream(output.toByteArray()))
            }
            assertTrue(exception.message!!.contains("Message too large"))
        }
    }
}
