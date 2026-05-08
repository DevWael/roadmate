package com.roadmate.core.location

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("LocationProvider interface contract")
class LocationProviderTest {

    @Test
    @DisplayName("anonymous implementation satisfies LocationProvider interface")
    fun anonymousImplementationSatisfiesInterface() {
        val provider: LocationProvider = object : LocationProvider {
            override val locationUpdates: Flow<android.location.Location> = emptyFlow()
            override fun requestLocationUpdates() {}
            override fun stopLocationUpdates() {}
        }
        assertNotNull(provider)
    }
}
