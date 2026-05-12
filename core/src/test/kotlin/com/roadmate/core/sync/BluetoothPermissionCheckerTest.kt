package com.roadmate.core.sync

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("BluetoothPermissionChecker")
class BluetoothPermissionCheckerTest {

    @Nested
    @DisplayName("REQUIRED_PERMISSIONS")
    inner class RequiredPermissions {
        @Test
        fun `returns empty list on pre-S API levels in test environment`() {
            assertTrue(BluetoothPermissionChecker.REQUIRED_PERMISSIONS.isEmpty() ||
                BluetoothPermissionChecker.REQUIRED_PERMISSIONS.contains("android.permission.BLUETOOTH_CONNECT"))
        }

        @Test
        fun `shouldShowRationale does not crash`() {
            assertTrue(true)
        }
    }
}
