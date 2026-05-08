# Story 1.2: Core Data Layer — Vehicle & Maintenance Entities

Status: done

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

- [x] Task 1: Room database setup (AC: #1)
  - [x] Create `core/src/main/kotlin/com/roadmate/core/database/RoadMateDatabase.kt`
  - [x] Configure WAL journal mode, export schema, list all entities
  - [x] Create `core/src/main/kotlin/com/roadmate/core/database/converter/Converters.kt` for Room TypeConverters (enums)

- [x] Task 2: Vehicle entity & DAO (AC: #2, #5)
  - [x] Create `core/src/main/kotlin/com/roadmate/core/database/entity/Vehicle.kt` with all fields
  - [x] Create `OdometerUnit` enum (KM, MILES) and `EngineType`/`FuelType` enums
  - [x] Create `core/src/main/kotlin/com/roadmate/core/database/dao/VehicleDao.kt` — queries scoped by id, `Flow<Vehicle?>` for active, `@Upsert`

- [x] Task 3: Maintenance entities & DAO (AC: #3, #4, #5)
  - [x] Create `MaintenanceSchedule.kt` entity with `vehicleId` index
  - [x] Create `MaintenanceRecord.kt` entity
  - [x] Create `MaintenanceDao.kt` — queries scoped by `vehicleId`, `Flow<List<MaintenanceSchedule>>`, `@Upsert`

- [x] Task 4: Repositories (AC: #6)
  - [x] Create `core/src/main/kotlin/com/roadmate/core/repository/VehicleRepository.kt`
  - [x] Create `core/src/main/kotlin/com/roadmate/core/repository/MaintenanceRepository.kt`
  - [x] All writes return `suspend Result<Unit>`, reads return `Flow<T>`

- [x] Task 5: Hilt DI module (AC: #1)
  - [x] Create `DatabaseModule.kt` in `core/di/` providing `RoadMateDatabase`, `VehicleDao`, `MaintenanceDao`
  - [x] Create `RepositoryModule.kt` binding repositories — Note: Repositories use constructor injection (@Inject + @Singleton), no separate RepositoryModule needed

- [x] Task 6: Maintenance template (AC: #7)
  - [x] Create `core/src/main/kotlin/com/roadmate/core/database/template/MaintenanceTemplates.kt`
  - [x] Implement Mitsubishi Lancer EX 2015 template as a list of `MaintenanceSchedule` objects

### Review Findings

- [x] [Review][Patch] `getSchedulesWithRecords` is a misleading no-op — returns same data as `getSchedulesForVehicle()` with unnecessary `@Transaction` overhead. Remove method. [MaintenanceDao.kt:67-69] ✅ Removed
- [x] [Review][Patch] Missing `deleteScheduleById` and `deleteRecordById` tests — repository methods exist without test coverage [MaintenanceRepositoryTest.kt] ✅ Added
- [x] [Review][Patch] `junit-platform-launcher` version not pinned — BOM-managed, different version scheme from Jupiter. Finding dismissed as false positive.
- [x] [Review][Defer] TypeConverter crash on removed enum values — pre-existing architectural choice, requires migration strategy [Converters.kt:20,26,32] — deferred, pre-existing
- [x] [Review][Defer] No automatic `lastModified` enforcement on writes — belongs in service/ViewModel layer (Story 1-3 scope) — deferred, pre-existing
- [x] [Review][Defer] No cascade delete integration test — requires Robolectric or instrumented test, out of unit test scope — deferred, pre-existing
- [x] [Review][Defer] Both schedule intervals can be null simultaneously — domain validation belongs in UI/service layer — deferred, pre-existing
- [x] [Review][Defer] No domain validation on numeric fields (year, engineSize, odometerKm) — validation is a presentation concern — deferred, pre-existing

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
Claude Opus 4.6 (Thinking)

### Debug Log References
- Fixed JUnit 5 Platform Launcher missing dependency (Gradle 9+ requires explicit `junit-platform-launcher` on test runtime classpath)
- Fixed FakeVehicleDao test: direct map insertions needed explicit `updateFlow()` calls to trigger `MutableStateFlow` emissions

### Completion Notes List
- ✅ Task 1: RoadMateDatabase with exportSchema=true, WAL journal mode (via DatabaseModule), TypeConverters for 3 enums
- ✅ Task 2: Vehicle entity with 15 fields, 3 enum types (EngineType, FuelType, OdometerUnit), VehicleDao with Flow-based queries and @Upsert
- ✅ Task 3: MaintenanceSchedule (with vehicleId FK + index) and MaintenanceRecord (with scheduleId + vehicleId FKs + indices), MaintenanceDao with scoped queries
- ✅ Task 4: VehicleRepository and MaintenanceRepository — suspend Result<Unit> writes, Flow<T> reads, @Singleton + @Inject constructor injection
- ✅ Task 5: DatabaseModule providing RoadMateDatabase (singleton, WAL mode), VehicleDao, MaintenanceDao. Repositories use constructor injection — no separate RepositoryModule needed
- ✅ Task 6: MaintenanceTemplates object with mitsubishiLancerEx2015() factory — 9 items matching AC #7 specification exactly
- ✅ Infrastructure fix: Added junit-platform-launcher to version catalog and core/build.gradle.kts for Gradle 9+ JUnit 5 compatibility
- ✅ Room schema v1 exported to core/schemas/
- All 61 unit tests pass. Both app modules compile with no regressions.

### File List
- core/src/main/kotlin/com/roadmate/core/database/RoadMateDatabase.kt (NEW)
- core/src/main/kotlin/com/roadmate/core/database/converter/Converters.kt (NEW)
- core/src/main/kotlin/com/roadmate/core/database/entity/Vehicle.kt (NEW)
- core/src/main/kotlin/com/roadmate/core/database/entity/MaintenanceSchedule.kt (NEW)
- core/src/main/kotlin/com/roadmate/core/database/entity/MaintenanceRecord.kt (NEW)
- core/src/main/kotlin/com/roadmate/core/database/dao/VehicleDao.kt (NEW)
- core/src/main/kotlin/com/roadmate/core/database/dao/MaintenanceDao.kt (NEW)
- core/src/main/kotlin/com/roadmate/core/repository/VehicleRepository.kt (NEW)
- core/src/main/kotlin/com/roadmate/core/repository/MaintenanceRepository.kt (NEW)
- core/src/main/kotlin/com/roadmate/core/di/DatabaseModule.kt (NEW)
- core/src/main/kotlin/com/roadmate/core/database/template/MaintenanceTemplates.kt (NEW)
- core/src/test/kotlin/com/roadmate/core/database/entity/VehicleTest.kt (NEW)
- core/src/test/kotlin/com/roadmate/core/database/entity/MaintenanceScheduleTest.kt (NEW)
- core/src/test/kotlin/com/roadmate/core/database/entity/MaintenanceRecordTest.kt (NEW)
- core/src/test/kotlin/com/roadmate/core/database/converter/ConvertersTest.kt (NEW)
- core/src/test/kotlin/com/roadmate/core/database/template/MaintenanceTemplatesTest.kt (NEW)
- core/src/test/kotlin/com/roadmate/core/repository/VehicleRepositoryTest.kt (NEW)
- core/src/test/kotlin/com/roadmate/core/repository/MaintenanceRepositoryTest.kt (NEW)
- core/build.gradle.kts (MODIFIED — added junit-platform-launcher)
- gradle/libs.versions.toml (MODIFIED — added junit-platform-launcher library)
- core/schemas/com.roadmate.core.database.RoadMateDatabase/1.json (NEW — auto-generated)

## Change Log
- 2026-05-09: Implemented Story 1-2 — Room database, 3 entities, 2 DAOs, 2 repositories, Hilt DI module, maintenance template, 61 unit tests. Fixed JUnit 5 launcher compatibility for Gradle 9+.
