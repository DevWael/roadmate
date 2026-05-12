package com.roadmate.core.sync.protocol

import com.roadmate.core.model.sync.SyncMessage
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("SyncMessage Protocol")
class SyncMessageProtocolTest {

    private val json = Json { ignoreUnknownKeys = true; classDiscriminator = "type" }

    @Nested
    @DisplayName("SyncStatus")
    inner class SyncStatusTests {

        @Test
        fun `serializes with sync_status discriminator`() {
            val msg = SyncMessage.SyncStatus(
                deviceId = "dev-1",
                timestamp = 1000L,
                lastSyncTimestamp = 500L,
            )
            val serialized = json.encodeToString(SyncMessage.serializer(), msg)
            assertTrue(serialized.contains("\"type\":\"sync_status\""))
        }

        @Test
        fun `deserializes SyncStatus`() {
            val jsonStr = """{"type":"sync_status","deviceId":"dev-1","timestamp":1000,"lastSyncTimestamp":500}"""
            val msg = json.decodeFromString(SyncMessage.serializer(), jsonStr)
            assertTrue(msg is SyncMessage.SyncStatus)
            val status = msg as SyncMessage.SyncStatus
            assertEquals("dev-1", status.deviceId)
            assertEquals(1000L, status.timestamp)
            assertEquals(500L, status.lastSyncTimestamp)
        }

        @Test
        fun `round-trips correctly`() {
            val msg = SyncMessage.SyncStatus("dev-1", 1000L, 500L)
            val serialized = json.encodeToString(SyncMessage.serializer(), msg)
            val deserialized = json.decodeFromString(SyncMessage.serializer(), serialized)
            assertEquals(msg, deserialized)
        }
    }

    @Nested
    @DisplayName("SyncPush")
    inner class SyncPushTests {

        @Test
        fun `contains entityType, data, and messageId`() {
            val msg = SyncMessage.SyncPush(
                entityType = "trip",
                data = """[{"id":"t-1"}]""",
                messageId = "uuid-abc-123",
                timestamp = 1000L,
            )
            assertEquals("trip", msg.entityType)
            assertEquals("uuid-abc-123", msg.messageId)
            assertNotNull(msg.data)
        }

        @Test
        fun `serializes with sync_push discriminator`() {
            val msg = SyncMessage.SyncPush(
                entityType = "trip_point",
                data = "[]",
                messageId = "uuid-1",
                timestamp = 1000L,
            )
            val serialized = json.encodeToString(SyncMessage.serializer(), msg)
            assertTrue(serialized.contains("\"type\":\"sync_push\""))
            assertTrue(serialized.contains("\"entityType\":\"trip_point\""))
            assertTrue(serialized.contains("\"messageId\":\"uuid-1\""))
        }

        @Test
        fun `round-trips correctly`() {
            val msg = SyncMessage.SyncPush(
                entityType = "vehicle",
                data = """[{"id":"v-1"}]""",
                messageId = "uuid-xyz",
                timestamp = 2000L,
            )
            val serialized = json.encodeToString(SyncMessage.serializer(), msg)
            val deserialized = json.decodeFromString(SyncMessage.serializer(), serialized)
            assertEquals(msg, deserialized)
        }
    }

    @Nested
    @DisplayName("SyncAck")
    inner class SyncAckTests {

        @Test
        fun `contains messageId field`() {
            val msg = SyncMessage.SyncAck(
                success = true,
                messageId = "uuid-abc-123",
                timestamp = 1000L,
                message = null,
            )
            assertEquals("uuid-abc-123", msg.messageId)
            assertTrue(msg.success)
        }

        @Test
        fun `serializes with sync_ack discriminator and messageId`() {
            val msg = SyncMessage.SyncAck(
                success = true,
                messageId = "uuid-1",
                timestamp = 1000L,
                message = null,
            )
            val serialized = json.encodeToString(SyncMessage.serializer(), msg)
            assertTrue(serialized.contains("\"type\":\"sync_ack\""))
            assertTrue(serialized.contains("\"messageId\":\"uuid-1\""))
        }

        @Test
        fun `round-trips with error message`() {
            val msg = SyncMessage.SyncAck(
                success = false,
                messageId = "uuid-err",
                timestamp = 1000L,
                message = "Upsert failed",
            )
            val serialized = json.encodeToString(SyncMessage.serializer(), msg)
            val deserialized = json.decodeFromString(SyncMessage.serializer(), serialized)
            assertEquals(msg, deserialized)
        }
    }
}
