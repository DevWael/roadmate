package com.roadmate.core.state

import app.cash.turbine.test
import com.roadmate.core.model.DrivingState
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("DrivingStateManager")
class DrivingStateManagerTest {

    private lateinit var manager: DrivingStateManager

    @BeforeEach
    fun setUp() {
        manager = DrivingStateManager()
    }

    @Nested
    @DisplayName("initial state")
    inner class InitialState {
        @Test
        fun `is Idle`() = runTest {
            manager.drivingState.test {
                assertEquals(DrivingState.Idle, awaitItem())
            }
        }
    }

    @Nested
    @DisplayName("updateState")
    inner class UpdateState {
        @Test
        fun `emits new Driving state`() = runTest {
            manager.drivingState.test {
                assertEquals(DrivingState.Idle, awaitItem())
                val driving = DrivingState.Driving("trip-1", 10.0, 60000L)
                manager.updateState(driving)
                assertEquals(driving, awaitItem())
            }
        }

        @Test
        fun `emits Stopping state`() = runTest {
            manager.drivingState.test {
                assertEquals(DrivingState.Idle, awaitItem())
                val stopping = DrivingState.Stopping(3000L)
                manager.updateState(stopping)
                assertEquals(stopping, awaitItem())
            }
        }

        @Test
        fun `emits GapCheck state`() = runTest {
            manager.drivingState.test {
                assertEquals(DrivingState.Idle, awaitItem())
                val gap = DrivingState.GapCheck(5000L)
                manager.updateState(gap)
                assertEquals(gap, awaitItem())
            }
        }

        @Test
        fun `emits back to Idle`() = runTest {
            manager.drivingState.test {
                assertEquals(DrivingState.Idle, awaitItem())
                manager.updateState(DrivingState.Driving("t", 1.0, 100L))
                assertEquals(DrivingState.Driving("t", 1.0, 100L), awaitItem())
                manager.updateState(DrivingState.Idle)
                assertEquals(DrivingState.Idle, awaitItem())
            }
        }
    }
}
