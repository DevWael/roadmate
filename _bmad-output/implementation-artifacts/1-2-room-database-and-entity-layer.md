# Story 1.2: Core Data Layer — Vehicle & Maintenance Entities

Status: ready-for-dev

## Story

As a developer,
I want Room database entities, DAOs, and repositories for Vehicle and Maintenance domain,
so that vehicle profiles and maintenance schedules can be persisted and queried.

## Acceptance Criteria

1. **RoadMateDatabase configured** — Uses `JournalMode.WRITE_AHEAD_LOGGING`, `exportSchema = true`, and includes all entity classes. Lives in `core/database/RoadMateDatabase.kt`.

2. **Vehicle entity complete** — Fields: `id` (String UUID PK, default `UUID.randomUUID().toString()`), `name`, `make`, `model`, `year`, `engineType`, `engineSize`, `fuelType`, `plateNumber`, `vin` (nullable), `odometerKm` (Double), `odometerUnit` (km/miles enum), `cityConsumption` (Double), `highwayConsumption` (Double), `lastModified` (Long epoch millis).

3. **MaintenanceSchedule entity complete** — Fields: `id` (String UUID PK), `vehicleId` (FK), `name`, `intervalKm` (Int nullable), `intervalMonths` (Int nullable), `lastServiceKm` (Double), `lastServiceDate` (Long), `isCustom` (Boolean), `lastModified` (Long). Index on `vehicleId`.

4. **MaintenanceRecord entity complete** — Fields: `id` (String UUID PK), `scheduleId` (FK), `vehicleId` (FK), `datePerformed` (Long), `odometerKm` (Double), `cost` (Double nullable), `location` (String nullable), `notes` (String nullable), `lastModified` (Long).

5. **DAOs follow pattern** — `VehicleDao` and `MaintenanceDao` scope all queries by `vehicleId`, return `Flow<T>` for reactive queries, use `@Upsert` for writes.

6. **Repositories follow pattern** — `VehicleRepository` and `MaintenanceRepository` return `suspend Result<Unit>` for writes via `runCatching {}`, `Flow<T>` for reactive queries delegating to DAOs.

7. **Maintenance template** — Pre-built template for Mitsubishi Lancer EX 2015 with 9 items: Oil Change (10K/6mo), Oil Filter (10K/6mo), Air Filter (20K/12mo), Brake Pads (40K/24mo), Tire Rotation (10K/6mo), Coolant (40K/24mo), Spark Plugs (30K), Transmission Fluid (60K), Brake Fluid (40K/24mo).

## Tasks / Subtasks

- [ ] Task 1: Room database setup (AC: #1)
  - [ ] Create `core/src/main/kotlin/com/roadmate/core/database/RoadMateDatabase.kt`
  - [ ] Configure WAL journal mode, export schema, list all entities
  - [ ] Create `core/src/main/kotlin/com/roadmate/core/database/converter/Converters.kt` for Room TypeConverters (enums)

- [ ] Task 2: Vehicle entity & DAO (AC: #2, #5)
  - [ ] Create `core/src/main/kotlin/com/roadmate/core/database/entity/Vehicle.kt` with all fields
  - [ ] Create `OdometerUnit` enum (KM, MILES) and `EngineType`/`FuelType` enums
  - [ ] Create `core/src/main/kotlin/com/roadmate/core/database/dao/VehicleDao.kt` — queries scoped by id, `Flow<Vehicle?>` for active, `@Upsert`

- [ ] Task 3: Maintenance entities & DAO (AC: #3, #4, #5)
  - [ ] Create `MaintenanceSchedule.kt` entity with `vehicleId` index
  - [ ] Create `MaintenanceRecord.kt` entity
  - [ ] Create `MaintenanceDao.kt` — queries scoped by `vehicleId`, `Flow<List<MaintenanceSchedule>>`, `@Upsert`

- [ ] Task 4: Repositories (AC: #6)
  - [ ] Create `core/src/main/kotlin/com/roadmate/core/repository/VehicleRepository.kt`
  - [ ] Create `core/src/main/kotlin/com/roadmate/core/repository/MaintenanceRepository.kt`
  - [ ] All writes return `suspend Result<Unit>`, reads return `Flow<T>`

- [ ] Task 5: Hilt DI module (AC: #1)
  - [ ] Create `DatabaseModule.kt` in `core/di/` providing `RoadMateDatabase`, `VehicleDao`, `MaintenanceDao`
  - [ ] Create `RepositoryModule.kt` binding repositories

- [ ] Task 6: Maintenance template (AC: #7)
  - [ ] Create `core/src/main/kotlin/com/roadmate/core/database/template/MaintenanceTemplates.kt`
  - [ ] Implement Mitsubishi Lancer EX 2015 template as a list of `MaintenanceSchedule` objects

## Dev Notes

### Architecture Compliance

**Database Naming (MUST follow):**
- Table names: `snake_case`, plural → `vehicles`, `maintenance_schedules`, `maintenance_records`
- Column names: `snake_case` → `vehicle_id`, `last_modified`, `odometer_km`
- Entity class names: PascalCase, singular → `Vehicle`, `MaintenanceSchedule`
- Use `@ColumnInfo(name = "snake_case")` annotations

**Room entity = domain model** — No separate domain class. Entity is used throughout. Sync uses separate DTOs (Story 1.3).

**UUID strategy:** `@PrimaryKey val id: String = UUID.randomUUID().toString()` — conflict-free IDs for sync.

**Timestamp convention:** All `lastModified` fields are `Long` epoch millis, updated on every write.

**Hilt module pattern:**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RoadMateDatabase =
        Room.databaseBuilder(context, RoadMateDatabase::class.java, "roadmate.db")
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .build()
    @Provides
    fun provideVehicleDao(db: RoadMateDatabase): VehicleDao = db.vehicleDao()
}
```

**Repository pattern:**
```kotlin
class VehicleRepository @Inject constructor(private val vehicleDao: VehicleDao) {
    fun getVehicle(vehicleId: String): Flow<Vehicle?> = vehicleDao.getVehicle(vehicleId)
    suspend fun saveVehicle(vehicle: Vehicle): Result<Unit> = runCatching { vehicleDao.upsert(vehicle) }
}
```

### Anti-Patterns to Avoid

| ❌ Do NOT | ✅ Do THIS |
|---|---|
| Create unscoped `getAllVehicles()` | Every query takes a filter parameter |
| Use `@Insert(onConflict = REPLACE)` | Use `@Upsert` (Room 2.8+) |
| Return `LiveData` from DAOs | Return `Flow<T>` |
| Throw exceptions from repository | Return `Result<T>` via `runCatching` |
| Use auto-increment Int PKs | Use String UUID PKs |
| Store dates as formatted strings | Store as `Long` epoch millis |

### Dependencies on Story 1.1

This story requires the project scaffold from 1.1:
- Convention plugins with `AndroidRoomConventionPlugin` applied to `:core`
- Version catalog with Room 2.8.4, Hilt 2.59.2 entries
- KSP configured for Room annotation processing

### References

- [Source: architecture.md#Data Architecture] — Entity design, UUID strategy, timestamp convention
- [Source: architecture.md#Implementation Patterns] — Repository pattern, DAO pattern, Hilt module pattern
- [Source: architecture.md#Naming Patterns] — Database and code naming conventions
- [Source: epics.md#Story 1.2] — Acceptance criteria and maintenance template

## Dev Agent Record

### Agent Model Used
### Debug Log References
### Completion Notes List
### File List
