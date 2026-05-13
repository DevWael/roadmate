package com.roadmate.core.ui.components

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("StatusChip logic")
class StatusChipTest {

    @Nested
    @DisplayName("time ago formatting")
    inner class TimeAgoFormatting {

        @Test
        fun `just now for less than 60 seconds`() {
            val now = System.currentTimeMillis()
            val result = formatTimeAgo(now - 30_000, now)
            assertEquals("just now", result)
        }

        @Test
        fun `minutes ago for less than 60 minutes`() {
            val now = System.currentTimeMillis()
            val result = formatTimeAgo(now - 5 * 60_000, now)
            assertEquals("5m ago", result)
        }

        @Test
        fun `1m ago singular`() {
            val now = System.currentTimeMillis()
            val result = formatTimeAgo(now - 60_000, now)
            assertEquals("1m ago", result)
        }

        @Test
        fun `hours ago for less than 24 hours`() {
            val now = System.currentTimeMillis()
            val result = formatTimeAgo(now - 2 * 3600_000, now)
            assertEquals("2h ago", result)
        }

        @Test
        fun `1h ago singular`() {
            val now = System.currentTimeMillis()
            val result = formatTimeAgo(now - 3600_000, now)
            assertEquals("1h ago", result)
        }

        @Test
        fun `never synced returns never`() {
            val now = System.currentTimeMillis()
            val result = formatTimeAgo(0L, now)
            assertEquals("never", result)
        }

        @Test
        fun `negative elapsed from clock skew returns just now`() {
            val now = System.currentTimeMillis()
            val result = formatTimeAgo(now + 5000, now)
            assertEquals("just now", result)
        }
    }

    @Nested
    @DisplayName("sync status label")
    inner class SyncStatusLabel {

        @Test
        fun `connected idle shows synced time ago`() {
            val now = System.currentTimeMillis()
            val label = syncStatusLabel(
                isConnected = true,
                isConnecting = false,
                isSyncing = false,
                isFailed = false,
                lastSyncTimestamp = now - 2 * 60_000,
                currentTimeMs = now,
            )
            assertEquals("Synced 2m ago", label)
        }

        @Test
        fun `syncing shows syncing message`() {
            val now = System.currentTimeMillis()
            val label = syncStatusLabel(
                isConnected = true,
                isConnecting = false,
                isSyncing = true,
                isFailed = false,
                lastSyncTimestamp = now - 2 * 60_000,
                currentTimeMs = now,
            )
            assertEquals("Syncing...", label)
        }

        @Test
        fun `sync failed shows error message`() {
            val now = System.currentTimeMillis()
            val label = syncStatusLabel(
                isConnected = true,
                isConnecting = false,
                isSyncing = false,
                isFailed = true,
                lastSyncTimestamp = now - 2 * 60_000,
                currentTimeMs = now,
            )
            assertEquals("Sync failed", label)
        }

        @Test
        fun `disconnected shows not connected`() {
            val now = System.currentTimeMillis()
            val label = syncStatusLabel(
                isConnected = false,
                isConnecting = false,
                isSyncing = false,
                isFailed = false,
                lastSyncTimestamp = now - 2 * 60_000,
                currentTimeMs = now,
            )
            assertEquals("Not connected", label)
        }

        @Test
        fun `connecting shows connecting message`() {
            val now = System.currentTimeMillis()
            val label = syncStatusLabel(
                isConnected = false,
                isConnecting = true,
                isSyncing = false,
                isFailed = false,
                lastSyncTimestamp = now - 2 * 60_000,
                currentTimeMs = now,
            )
            assertEquals("Connecting...", label)
        }
    }

    @Nested
    @DisplayName("sync state resolution")
    inner class SyncStateResolution {

        @Test
        fun `sync failed timeout reverts after 10 seconds`() {
            val now = System.currentTimeMillis()
            val failedAt = now - 11_000
            val shouldRevert = shouldRevertFromFailed(failedAt, now, revertDelayMs = 10_000)
            assertTrue(shouldRevert)
        }

        @Test
        fun `sync failed does not revert before 10 seconds`() {
            val now = System.currentTimeMillis()
            val failedAt = now - 5_000
            val shouldRevert = shouldRevertFromFailed(failedAt, now, revertDelayMs = 10_000)
            assertFalse(shouldRevert)
        }

        @Test
        fun `sync failed reverts at exactly 10 seconds`() {
            val now = System.currentTimeMillis()
            val failedAt = now - 10_000
            val shouldRevert = shouldRevertFromFailed(failedAt, now, revertDelayMs = 10_000)
            assertTrue(shouldRevert)
        }

        @Test
        fun `uninitialized failedAtMs does not revert`() {
            val now = System.currentTimeMillis()
            val shouldRevert = shouldRevertFromFailed(0L, now, revertDelayMs = 10_000)
            assertFalse(shouldRevert)
        }

        @Test
        fun `negative failedAtMs does not revert`() {
            val now = System.currentTimeMillis()
            val shouldRevert = shouldRevertFromFailed(-1L, now, revertDelayMs = 10_000)
            assertFalse(shouldRevert)
        }
    }

    @Nested
    @DisplayName("pulse animation control")
    inner class PulseAnimationControl {

        @Test
        fun `syncing state enables pulse`() {
            assertTrue(shouldPulse(isSyncing = true))
        }

        @Test
        fun `non syncing state disables pulse`() {
            assertFalse(shouldPulse(isSyncing = false))
        }
    }
}
