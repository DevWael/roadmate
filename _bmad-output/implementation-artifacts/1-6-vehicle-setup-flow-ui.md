# Story 1.6: Vehicle Profile Management UI (Head Unit)

Status: ready-for-dev

## Story

As a driver,
I want to create and manage vehicle profiles on my head unit,
so that I can set up my car's details and maintenance schedule before I start driving.

## Acceptance Criteria

1. **Vehicle creation form** — Number-only and selection inputs: vehicle name (text), make (text), model (text), year (number), engine type (dropdown), engine size (number), fuel type (dropdown), plate number (text), odometer (number), odometer unit (km/miles toggle). All touch targets ≥76dp.

2. **Maintenance template selection** — "Mitsubishi Lancer EX 2015" or "Custom (no template)". Template pre-populates MaintenanceSchedule records via repository.

3. **Odometer input** — Value stored as `odometerKm`, displayed with locale number formatting on dashboard.

4. **Vehicle switcher** — List of all profiles. Selecting updates active vehicle in DataStore. Dashboard reflects immediately.

5. **Fuel consumption estimates** — City/highway L/100km persisted to Vehicle entity for trip fuel estimation.

6. **Dashboard shell** — Vehicle name, current ODO (displayLarge), StatusChip visible. Renders within 2 seconds.

## Tasks / Subtasks

- [ ] Task 1: Vehicle form UI (AC: #1, #3)
  - [ ] Create `app-headunit/ui/parked/VehicleSetupScreen.kt` — Screen/Content split
  - [ ] Implement form with head-unit-sized inputs (76dp touch targets)
  - [ ] Number inputs for ODO, year, engine size; dropdowns for engine type, fuel type
  - [ ] Use `RoadMateTheme` head unit typography

- [ ] Task 2: Maintenance template step (AC: #2)
  - [ ] Add template selector as second step/section in vehicle creation
  - [ ] Wire to `MaintenanceTemplates.kt` from Story 1.2
  - [ ] Create MaintenanceSchedule records via repository on selection

- [ ] Task 3: Vehicle ViewModel (AC: #1, #3, #5)
  - [ ] Create `VehicleSetupViewModel.kt` with form state
  - [ ] Validate inputs, save via `VehicleRepository`
  - [ ] Store active vehicle ID in DataStore

- [ ] Task 4: Vehicle switcher (AC: #4)
  - [ ] Create vehicle list overlay/dialog for switching
  - [ ] Persist selection to DataStore
  - [ ] Notify dashboard to refresh via `Flow`

- [ ] Task 5: Dashboard shell (AC: #6)
  - [ ] Create `app-headunit/ui/parked/DashboardShell.kt` composable
  - [ ] Display vehicle name, ODO, StatusChip placeholder
  - [ ] Locale-formatted numbers

## Dev Notes

### Architecture Compliance

**Screen/Content split pattern (MUST follow):**
```kotlin
@Composable
fun VehicleSetupScreen(viewModel: VehicleSetupViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    VehicleSetupContent(uiState = uiState, onSave = viewModel::save)
}
@Composable
fun VehicleSetupContent(uiState: UiState<VehicleFormState>, onSave: () -> Unit) { ... }
```

**Automotive touch targets:** All interactive elements ≥76dp. This is a hard UX constraint for arm's-length operation.

**DataStore for active vehicle:** `PreferencesDataStore` key: `active_vehicle_id`. Read as `Flow<String?>`.

### References

- [Source: architecture.md#Implementation Patterns] — Screen/Content split, ViewModel
- [Source: ux-design-specification.md#Touch Targets] — 76dp minimum
- [Source: ux-design-specification.md#Dashboard Layout] — Parked mode shell
- [Source: epics.md#Story 1.6] — Acceptance criteria

## Dev Agent Record

### Agent Model Used
### Debug Log References
### Completion Notes List
### File List
