package com.roadmate.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.roadmate.core.database.RoadMateDatabase
import com.roadmate.core.database.dao.DocumentDao
import com.roadmate.core.database.dao.FuelDao
import com.roadmate.core.database.dao.MaintenanceDao
import com.roadmate.core.database.dao.TripDao
import com.roadmate.core.database.dao.VehicleDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing the Room database and DAO instances.
 *
 * The database is configured with WAL journal mode for improved
 * concurrent read/write performance on the head unit.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RoadMateDatabase =
        Room.databaseBuilder(context, RoadMateDatabase::class.java, "roadmate.db")
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .addMigrations(RoadMateDatabase.MIGRATION_1_2)
            .build()

    @Provides
    fun provideVehicleDao(db: RoadMateDatabase): VehicleDao =
        db.vehicleDao()

    @Provides
    fun provideMaintenanceDao(db: RoadMateDatabase): MaintenanceDao =
        db.maintenanceDao()

    @Provides
    fun provideTripDao(db: RoadMateDatabase): TripDao =
        db.tripDao()

    @Provides
    fun provideFuelDao(db: RoadMateDatabase): FuelDao =
        db.fuelDao()

    @Provides
    fun provideDocumentDao(db: RoadMateDatabase): DocumentDao =
        db.documentDao()
}
