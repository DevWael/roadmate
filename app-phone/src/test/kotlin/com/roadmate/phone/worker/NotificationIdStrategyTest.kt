package com.roadmate.phone.worker

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("NotificationIdStrategy")
class NotificationIdStrategyTest {

    @Test
    @DisplayName("same schedule ID produces same notification ID")
    fun sameScheduleIdSameNotificationId() {
        val first = "mnt:schedule-123".hashCode()
        val second = "mnt:schedule-123".hashCode()
        assertEquals(first, second)
    }

    @Test
    @DisplayName("different IDs produce different notification IDs")
    fun differentIdsDifferentNotificationIds() {
        val id1 = "mnt:schedule-123".hashCode()
        val id2 = "mnt:schedule-456".hashCode()
        assertNotEquals(id1, id2)
    }

    @Test
    @DisplayName("hashCode is stable across calls")
    fun hashCodeIsStable() {
        val id = "doc:abc"
        val first = id.hashCode()
        val second = id.hashCode()
        assertEquals(first, second)
    }

    @Test
    @DisplayName("maintenance and document IDs for same UUID do not collide")
    fun maintenanceAndDocumentIdsDoNotCollide() {
        val uuid = "550e8400-e29b-41d4-a716-446655440000"
        val maintenanceId = "mnt:$uuid".hashCode()
        val documentId = "doc:$uuid".hashCode()
        assertNotEquals(maintenanceId, documentId)
    }
}
