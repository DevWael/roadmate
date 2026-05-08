# Story 1.3: Core Data Layer — Trip, Fuel & Document Entities

Status: ready-for-dev

## Story

As a developer,
I want Room entities, DAOs, and repositories for Trip, Fuel, and Document domains plus the OBDProvider interface,
so that all data models are ready for trip tracking, fuel logging, document management, and future OBD-II integration.

## Acceptance Criteria

1. **Trip entity** — Fields: `id` (String UUID PK), `vehicleId` (FK), `startTime` (Long), `endTime` (Long?), `distanceKm` (Double), `durationMs` (Long), `maxSpeedKmh` (Double), `avgSpeedKmh` (Double), `estimatedFuelL` (Double), `startOdometerKm` (Double), `endOdometerKm` (Double), `status` (enum: ACTIVE, COMPLETED, INTERRUPTED), `lastModified` (Long).

2. **TripPoint entity** — Fields: `id` (String UUID PK), `tripId` (FK), `latitude` (Double), `longitude` (Double), `speedKmh` (Double), `altitude` (Double), `accuracy` (Float), `timestamp` (Long), `lastModified` (Long). Index on `tripId`.

3. **FuelLog entity** — Fields: `id` (String UUID PK), `vehicleId` (FK), `date` (Long), `odometerKm` (Double), `liters` (Double), `pricePerLiter` (Double), `totalCost` (Double), `isFullTank` (Boolean), `station` (String?), `lastModified` (Long).

4. **Document entity** — Fields: `id` (String UUID PK), `vehicleId` (FK), `type` (enum: INSURANCE, LICENSE, REGISTRATION, OTHER), `name` (String), `expiryDate` (Long), `reminderDaysBefore` (Int, default 30), `notes` (String?), `lastModified` (Long).

5. **DAOs** — `TripDao`, `FuelDao`, `DocumentDao` — all queries scoped by `vehicleId` or parent FK, `Flow<T>` reactive, `@Upsert` writes.

6. **Repositories** — `TripRepository`, `FuelRepository`, `DocumentRepository` — same `Result<T>` / `Flow<T>` pattern.

7. **OBDProvider interface** — Methods for odometer, RPM, fuel rate, coolant temp, battery voltage, DTC codes — all returning nullable types. `MockOBDProvider` returns null for all.

8. **Sync DTOs** — `TripSyncDto`, `TripPointSyncDto`, `MaintenanceScheduleSyncDto`, `MaintenanceRecordSyncDto`, `FuelLogSyncDto`, `DocumentSyncDto`, `VehicleSyncDto` — all `@Serializable` camelCase. Extension functions `toSyncDto()` / `toEntity()` for each pair.

## Tasks / Subtasks

- [ ] Task 1: Trip entities (AC: #1, #2)
  - [ ] Create `Trip.kt` entity with `TripStatus` enum
  - [ ] Create `TripPoint.kt` entity with `tripId` index
  - [ ] Add both to `RoadMateDatabase` entities list

- [ ] Task 2: FuelLog & Document entities (AC: #3, #4)
  - [ ] Create `FuelLog.kt` entity
  - [ ] Create `Document.kt` entity with `DocumentType` enum
  - [ ] Add both to `RoadMateDatabase`

- [ ] Task 3: DAOs (AC: #5)
  - [ ] Create `TripDao.kt` — `getTripsForVehicle(vehicleId)`, `getTripPointsForTrip(tripId)`, `getActiveTrip(vehicleId)`
  - [ ] Create `FuelDao.kt` — `getFuelLogsForVehicle(vehicleId)`, `getLastFullTankEntry(vehicleId)`
  - [ ] Create `DocumentDao.kt` — `getDocumentsForVehicle(vehicleId)`, `getExpiringDocuments(vehicleId, threshold)`

- [ ] Task 4: Repositories (AC: #6)
  - [ ] Create `TripRepository.kt`, `FuelRepository.kt`, `DocumentRepository.kt`
  - [ ] Update `DatabaseModule.kt` and `RepositoryModule.kt` with new DAOs/repos

- [ ] Task 5: OBDProvider (AC: #7)
  - [ ] Create `core/obd/OBDProvider.kt` interface
  - [ ] Create `core/obd/MockOBDProvider.kt` — all methods return null

- [ ] Task 6: Sync DTOs & mappers (AC: #8)
  - [ ] Create `core/model/sync/*.kt` — 7 DTO classes, all `@Serializable`
  - [ ] Create extension functions `toSyncDto()` / `toEntity()` for each entity-DTO pair
  - [ ] Create `SyncMessage.kt` sealed interface: `SyncStatus`, `SyncPull`, `SyncPush`, `SyncAck`

## Dev Notes

### Architecture Compliance

**TripPoint volume concern:** At 3s GPS interval, a 1-hour trip produces 1200 TripPoints. Ensure TripPoint table has index on `tripId` and queries are efficient. NFR26 constrains DB to <50MB/year.

**Sync DTO pattern:**
```kotlin
@Serializable
data class TripSyncDto(
    val id: String, val vehicleId: String, val startTime: Long,
    val endTime: Long?, val distanceKm: Double, val lastModified: Long
)
fun Trip.toSyncDto() = TripSyncDto(id, vehicleId, startTime, endTime, distanceKm, lastModified)
fun TripSyncDto.toEntity() = Trip(id, vehicleId, startTime, endTime, distanceKm, ...)
```

**OBDProvider is V2-ready stub** — Interface designed now, real implementation comes in V2 with ELM327 Bluetooth adapter. `MockOBDProvider` allows GPS-based fallback for V1.

### Dependencies on Story 1.2

Requires `RoadMateDatabase`, `DatabaseModule`, `RepositoryModule`, Vehicle entity, and Maintenance entities from Story 1.2.

### References

- [Source: architecture.md#Data Architecture] — Entity design, sync DTO pattern
- [Source: architecture.md#Sync Protocol Architecture] — DTO format, message types
- [Source: architecture.md#Structure Patterns] — File locations for OBD, sync, model packages
- [Source: epics.md#Story 1.3] — Acceptance criteria

## Dev Agent Record

### Agent Model Used
### Debug Log References
### Completion Notes List
### File List
