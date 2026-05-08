package com.roadmate.core.di

import com.roadmate.core.obd.MockOBDProvider
import com.roadmate.core.obd.OBDProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that binds the [OBDProvider] interface to its V1 implementation.
 *
 * V1 uses [MockOBDProvider] (all methods return null) for GPS-based fallback.
 * V2 will swap this binding for a real ELM327 Bluetooth implementation.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class OBDModule {

    @Binds
    @Singleton
    abstract fun bindOBDProvider(impl: MockOBDProvider): OBDProvider
}
