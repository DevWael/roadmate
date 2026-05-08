package com.roadmate.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.roadmate.core.database.converter.Converters
import com.roadmate.core.database.dao.MaintenanceDao
import com.roadmate.core.database.dao.VehicleDao
import com.roadmate.core.database.entity.MaintenanceRecord
import com.roadmate.core.database.entity.MaintenanceSchedule
import com.roadmate.core.database.entity.Vehicle

/**
 * Main Room database for the RoadMate application.
 *
 * Uses WAL journal mode for improved concurrent read/write performance.
 * Schema export is enabled for migration validation.
 */
@Database(
    entities = [
        Vehicle::class,
        MaintenanceSchedule::class,
        MaintenanceRecord::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class RoadMateDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun maintenanceDao(): MaintenanceDao
}
