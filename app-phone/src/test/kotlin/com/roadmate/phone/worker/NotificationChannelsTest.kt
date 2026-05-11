package com.roadmate.phone.worker

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("NotificationChannels")
class NotificationChannelsTest {

    @Test
    @DisplayName("channel IDs are stable constants")
    fun channelIdsAreStable() {
        assertEquals("maintenance_alerts", NotificationChannels.MAINTENANCE_ALERTS)
        assertEquals("document_reminders", NotificationChannels.DOCUMENT_REMINDERS)
    }
}
