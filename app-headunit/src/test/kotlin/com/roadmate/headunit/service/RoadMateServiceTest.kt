package com.roadmate.headunit.service

import com.roadmate.core.model.BtConnectionState
import com.roadmate.core.model.DrivingState
import com.roadmate.core.model.GpsState
import com.roadmate.core.state.BluetoothStateManager
import com.roadmate.core.state.DrivingStateManager
import com.roadmate.core.state.LocationStateManager
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("RoadMateService")
class RoadMateServiceTest {

    private lateinit var drivingStateManager: DrivingStateManager
    private lateinit var bluetoothStateManager: BluetoothStateManager
    private lateinit var locationStateManager: LocationStateManager

    @BeforeEach
    fun setUp() {
        drivingStateManager = DrivingStateManager()
        bluetoothStateManager = BluetoothStateManager()
        locationStateManager = LocationStateManager()
    }

    @Nested
    @DisplayName("state manager integration")
    inner class StateManagerIntegration {
        @Test
        fun `state managers are injectable singletons`() {
            assertNotNull(drivingStateManager)
            assertNotNull(bluetoothStateManager)
            assertNotNull(locationStateManager)
        }

        @Test
        fun `driving state flows correctly`() = runTest {
            drivingStateManager.updateState(DrivingState.Driving("trip-1", 5.0, 3000L))
            assertEquals(DrivingState.Driving("trip-1", 5.0, 3000L), drivingStateManager.drivingState.value)
        }

        @Test
        fun `bluetooth state flows correctly`() = runTest {
            bluetoothStateManager.updateState(BtConnectionState.Connected)
            assertEquals(BtConnectionState.Connected, bluetoothStateManager.btConnectionState.value)
        }

        @Test
        fun `location state flows correctly`() = runTest {
            locationStateManager.updateState(GpsState.Acquired)
            assertEquals(GpsState.Acquired, locationStateManager.gpsState.value)
        }
    }

    @Nested
    @DisplayName("companion constants")
    inner class CompanionConstants {
        @Test
        fun `notification ID is stable`() {
            assertEquals(1, RoadMateService.NOTIFICATION_ID)
        }

        @Test
        fun `channel ID matches naming convention`() {
            assertEquals("roadmate_tracking", RoadMateService.CHANNEL_ID)
        }

        @Test
        fun `notification text describes tracking state`() {
            assertEquals("RoadMate tracking active", RoadMateService.NOTIFICATION_TEXT)
        }
    }
}
