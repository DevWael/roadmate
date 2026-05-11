package com.roadmate.core.ui.components

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import com.roadmate.core.database.entity.DocumentType
import java.time.LocalDate
import java.time.ZoneId

@DisplayName("Document list logic")
class DocumentListTest {

    @Nested
    @DisplayName("days until expiry")
    inner class DaysUntilExpiry {

        @Test
        @DisplayName("future date returns positive days")
        fun futureDatePositive() {
            val futureDate = LocalDate.now().plusDays(10)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            val days = daysUntilExpiry(futureDate)
            assertEquals(10L, days)
        }

        @Test
        @DisplayName("past date returns negative days")
        fun pastDateNegative() {
            val pastDate = LocalDate.now().minusDays(5)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            val days = daysUntilExpiry(pastDate)
            assertEquals(-5L, days)
        }

        @Test
        @DisplayName("today returns zero")
        fun todayReturnsZero() {
            val today = LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            val days = daysUntilExpiry(today)
            assertEquals(0L, days)
        }
    }

    @Nested
    @DisplayName("expiry state classification")
    inner class ExpiryState {

        @Test
        @DisplayName("negative days is expired")
        fun negativeDaysExpired() {
            assertEquals(DocumentExpiryState.EXPIRED, documentExpiryState(-1, 30))
        }

        @Test
        @DisplayName("zero days is warning")
        fun zeroDaysWarning() {
            assertEquals(DocumentExpiryState.WARNING, documentExpiryState(0, 30))
        }

        @Test
        @DisplayName("within reminder window is warning")
        fun withinReminderWarning() {
            assertEquals(DocumentExpiryState.WARNING, documentExpiryState(15, 30))
        }

        @Test
        @DisplayName("at reminder boundary is warning")
        fun atBoundaryWarning() {
            assertEquals(DocumentExpiryState.WARNING, documentExpiryState(30, 30))
        }

        @Test
        @DisplayName("beyond reminder window is normal")
        fun beyondReminderNormal() {
            assertEquals(DocumentExpiryState.NORMAL, documentExpiryState(31, 30))
        }

        @Test
        @DisplayName("custom reminder window of 14 days")
        fun customReminderWindow() {
            assertEquals(DocumentExpiryState.NORMAL, documentExpiryState(15, 14))
            assertEquals(DocumentExpiryState.WARNING, documentExpiryState(14, 14))
            assertEquals(DocumentExpiryState.WARNING, documentExpiryState(5, 14))
        }

        @Test
        @DisplayName("zero reminder days means no warning")
        fun zeroReminderNoWarning() {
            assertEquals(DocumentExpiryState.NORMAL, documentExpiryState(0, 0))
            assertEquals(DocumentExpiryState.NORMAL, documentExpiryState(5, 0))
            assertEquals(DocumentExpiryState.EXPIRED, documentExpiryState(-1, 0))
        }
    }

    @Nested
    @DisplayName("expiry text formatting")
    inner class ExpiryText {

        @Test
        @DisplayName("expired 1 day ago")
        fun expiredOneDay() {
            assertEquals("Expired 1 day ago", formatExpiryText(-1))
        }

        @Test
        @DisplayName("expired 5 days ago")
        fun expiredFiveDays() {
            assertEquals("Expired 5 days ago", formatExpiryText(-5))
        }

        @Test
        @DisplayName("expires today")
        fun expiresToday() {
            assertEquals("Expires today", formatExpiryText(0))
        }

        @Test
        @DisplayName("1 day remaining")
        fun oneDayRemaining() {
            assertEquals("1 day remaining", formatExpiryText(1))
        }

        @Test
        @DisplayName("30 days remaining")
        fun thirtyDaysRemaining() {
            assertEquals("30 days remaining", formatExpiryText(30))
        }
    }

    @Nested
    @DisplayName("document type icon mapping")
    inner class DocumentTypeIcon {

        @Test
        @DisplayName("all document types return non-null icons")
        fun allTypesReturnIcons() {
            DocumentType.entries.forEach { type ->
                assertNotNull(documentTypeIcon(type), "Icon should not be null for $type")
            }
        }
    }
}
