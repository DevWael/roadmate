package com.roadmate.core.state

import app.cash.turbine.test
import com.roadmate.core.model.GpsState
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("LocationStateManager")
class LocationStateManagerTest {

    private lateinit var manager: LocationStateManager

    @BeforeEach
    fun setUp() {
        manager = LocationStateManager()
    }

    @Nested
    @DisplayName("initial state")
    inner class InitialState {
        @Test
        fun `is Acquiring`() = runTest {
            manager.gpsState.test {
                assertEquals(GpsState.Acquiring, awaitItem())
            }
        }
    }

    @Nested
    @DisplayName("updateState")
    inner class UpdateState {
        @Test
        fun `emits Acquired`() = runTest {
            manager.gpsState.test {
                assertEquals(GpsState.Acquiring, awaitItem())
                manager.updateState(GpsState.Acquired)
                assertEquals(GpsState.Acquired, awaitItem())
            }
        }

        @Test
        fun `emits Unavailable`() = runTest {
            manager.gpsState.test {
                assertEquals(GpsState.Acquiring, awaitItem())
                manager.updateState(GpsState.Unavailable)
                assertEquals(GpsState.Unavailable, awaitItem())
            }
        }

        @Test
        fun `transitions from Acquired to Unavailable`() = runTest {
            manager.gpsState.test {
                assertEquals(GpsState.Acquiring, awaitItem())
                manager.updateState(GpsState.Acquired)
                assertEquals(GpsState.Acquired, awaitItem())
                manager.updateState(GpsState.Unavailable)
                assertEquals(GpsState.Unavailable, awaitItem())
            }
        }

        @Test
        fun `transitions back to Acquiring`() = runTest {
            manager.gpsState.test {
                assertEquals(GpsState.Acquiring, awaitItem())
                manager.updateState(GpsState.Acquired)
                assertEquals(GpsState.Acquired, awaitItem())
                manager.updateState(GpsState.Acquiring)
                assertEquals(GpsState.Acquiring, awaitItem())
            }
        }
    }
}
