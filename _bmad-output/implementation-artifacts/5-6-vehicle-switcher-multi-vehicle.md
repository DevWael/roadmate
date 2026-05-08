# Story 5.6: Vehicle Switcher & Multi-Vehicle Support

Status: ready-for-dev

## Story

As a driver with multiple vehicles,
I want to switch between vehicles on my phone,
so that I can view and manage data for each car separately.

## Acceptance Criteria

1. **Vehicle selector** — Tap vehicle name in TopAppBar → dropdown/ModalBottomSheet with all vehicle names + ODO.

2. **Switch action** — Persist active vehicle ID to DataStore. VehicleHub refreshes immediately. All child screens scoped to new vehicle.

3. **Transition speed** — Data swap within 200ms (local data).

4. **Single vehicle** — List still shows with one entry. Can navigate to vehicle management.

5. **Launch persistence** — DataStore active vehicle ID loaded on launch without prompt.

6. **Deleted vehicle fallback** — If active vehicle deleted via sync → fall back to first available. If none → empty state.

## Tasks / Subtasks

- [ ] Task 1: Vehicle selector UI (AC: #1, #4)
  - [ ] Create `app-phone/ui/components/VehicleSelectorSheet.kt`
  - [ ] List all vehicles with name + ODO
  - [ ] Trigger from TopAppBar tap

- [ ] Task 2: Switch logic (AC: #2, #3, #5)
  - [ ] Persist to DataStore
  - [ ] Notify all screens via shared Flow
  - [ ] VehicleHub ViewModel re-collects all data for new vehicle

- [ ] Task 3: Edge cases (AC: #6)
  - [ ] Handle deleted vehicle gracefully
  - [ ] Fallback to first available or empty state

## Dev Notes

### Architecture Compliance

**Active vehicle is a DataStore preference** read by all ViewModels. When it changes, all Flows re-emit with the new vehicleId.

```kotlin
val activeVehicleId: Flow<String?> = dataStore.data.map { it[ACTIVE_VEHICLE_ID] }
```

### References

- [Source: architecture.md#Multi-Vehicle] — Vehicle switching, DataStore
- [Source: epics.md#Story 5.6] — Acceptance criteria

## Dev Agent Record

### Agent Model Used
### Debug Log References
### Completion Notes List
### File List
