package com.roadmate.core.di

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationServices
import com.roadmate.core.location.FusedLocationProvider
import com.roadmate.core.location.LocationProvider
import com.roadmate.core.location.PlatformLocationProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocationModule {

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(
        @ApplicationContext context: Context,
    ) = LocationServices.getFusedLocationProviderClient(context)

    @Provides
    @Singleton
    fun provideLocationProvider(
        @ApplicationContext context: Context,
        fusedProvider: Provider<FusedLocationProvider>,
        platformProvider: Provider<PlatformLocationProvider>,
    ): LocationProvider {
        val playServicesAvailable =
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) ==
                ConnectionResult.SUCCESS

        return if (playServicesAvailable) {
            fusedProvider.get()
        } else {
            platformProvider.get()
        }
    }
}
