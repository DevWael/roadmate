package com.roadmate.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.roadmate.core.database.converter.Converters
import com.roadmate.core.database.dao.DocumentDao
import com.roadmate.core.database.dao.FuelDao
import com.roadmate.core.database.dao.MaintenanceDao
import com.roadmate.core.database.dao.TripDao
import com.roadmate.core.database.dao.VehicleDao
import com.roadmate.core.database.entity.Document
import com.roadmate.core.database.entity.FuelLog
import com.roadmate.core.database.entity.MaintenanceRecord
import com.roadmate.core.database.entity.MaintenanceSchedule
import com.roadmate.core.database.entity.Trip
import com.roadmate.core.database.entity.TripPoint
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
        Trip::class,
        TripPoint::class,
        FuelLog::class,
        Document::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class RoadMateDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun maintenanceDao(): MaintenanceDao
    abstract fun tripDao(): TripDao
    abstract fun fuelDao(): FuelDao
    abstract fun documentDao(): DocumentDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trip_points ADD COLUMN is_gap_boundary INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
