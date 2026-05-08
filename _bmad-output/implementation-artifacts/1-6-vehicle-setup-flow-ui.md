# Story 1.6: Vehicle Profile Management UI (Head Unit)

Status: done

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

- [x] Task 1: Vehicle form UI (AC: #1, #3)
  - [x] Create `app-headunit/ui/parked/VehicleSetupScreen.kt` — Screen/Content split
  - [x] Implement form with head-unit-sized inputs (76dp touch targets)
  - [x] Number inputs for ODO, year, engine size; dropdowns for engine type, fuel type
  - [x] Use `RoadMateTheme` head unit typography

- [x] Task 2: Maintenance template step (AC: #2)
  - [x] Add template selector as second step/section in vehicle creation
  - [x] Wire to `MaintenanceTemplates.kt` from Story 1.2
  - [x] Create MaintenanceSchedule records via repository on selection

- [x] Task 3: Vehicle ViewModel (AC: #1, #3, #5)
  - [x] Create `VehicleSetupViewModel.kt` with form state
  - [x] Validate inputs, save via `VehicleRepository`
  - [x] Store active vehicle ID in DataStore

- [x] Task 4: Vehicle switcher (AC: #4)
  - [x] Create vehicle list overlay/dialog for switching
  - [x] Persist selection to DataStore
  - [x] Notify dashboard to refresh via `Flow`

- [x] Task 5: Dashboard shell (AC: #6)
  - [x] Create `app-headunit/ui/parked/DashboardShell.kt` composable
  - [x] Display vehicle name, ODO, StatusChip placeholder
  - [x] Locale-formatted numbers

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
GLM-5.1 (zai/glm-5.1)

### Debug Log References
- Fixed DataStoreModule Hilt annotation import ordering
- Replaced ToggleButton (unavailable in current BOM) with FilterChip for odometer unit toggle
- Replaced deprecated MenuAnchorType with ExposedDropdownMenuAnchorType
- Added Dispatchers.setMain/resetMain for ViewModel coroutine testing

### Completion Notes List
- Implemented complete VehicleSetupScreen with Screen/Content split pattern following Dev Notes
- All form inputs use 76dp minimum touch targets per automotive UX spec
- VehicleFormState includes self-validating extension functions (validate(), isValid(), toVehicle())
- VehicleSetupViewModel manages form state via MutableStateFlow<UiState<VehicleFormState>>
- Template selection integrated into form with radio buttons for Mitsubishi Lancer EX 2015 / Custom
- MaintenanceTemplates.mitsubishiLancerEx2015() creates 9 schedule records when template selected
- VehicleSwitcherDialog shows all vehicle profiles with active indicator, supports add new vehicle
- DashboardShell displays vehicle name, odometer (displayLarge), and StatusChip placeholders
- Odometer displayed with locale number formatting via NumberFormat.getNumberInstance()
- Odometer unit conversion (km/miles) applied in dashboard display
- ActiveVehicleRepository uses DataStore Preferences with key "active_vehicle_id"
- MainViewModel orchestrates screen navigation (setup vs dashboard vs switcher) based on vehicle state
- 72 unit tests pass: VehicleFormStateTest (validation + conversion) and VehicleSetupViewModelTest (field updates, save, templates)
- Full project test suite passes with no regressions

### File List
- `app-headunit/build.gradle.kts` (modified - added hilt-navigation-compose, datastore-preferences test dep)
- `app-headunit/src/main/kotlin/com/roadmate/headunit/MainActivity.kt` (modified - wired RoadMateMainScreen with navigation logic)
- `app-headunit/src/main/kotlin/com/roadmate/headunit/MainViewModel.kt` (new - manages active vehicle + vehicle list state)
- `app-headunit/src/main/kotlin/com/roadmate/headunit/ui/parked/VehicleSetupScreen.kt` (new - vehicle creation form with Screen/Content split)
- `app-headunit/src/main/kotlin/com/roadmate/headunit/ui/parked/VehicleSwitcherDialog.kt` (new - vehicle switching dialog)
- `app-headunit/src/main/kotlin/com/roadmate/headunit/ui/parked/DashboardShell.kt` (new - parked dashboard with ODO and StatusChip)
- `app-headunit/src/main/kotlin/com/roadmate/headunit/viewmodel/VehicleFormState.kt` (new - form state, validation, conversion)
- `app-headunit/src/main/kotlin/com/roadmate/headunit/viewmodel/VehicleSetupViewModel.kt` (new - ViewModel for vehicle setup)
- `app-headunit/src/test/kotlin/com/roadmate/headunit/viewmodel/VehicleFormStateTest.kt` (new - form validation and conversion tests)
- `app-headunit/src/test/kotlin/com/roadmate/headunit/viewmodel/VehicleSetupViewModelTest.kt` (new - ViewModel tests with fake DAOs)
- `core/src/main/kotlin/com/roadmate/core/di/DataStoreModule.kt` (new - Hilt module for DataStore Preferences)
- `core/src/main/kotlin/com/roadmate/core/repository/ActiveVehicleRepository.kt` (new - DataStore wrapper for active vehicle ID)

## Change Log
- 2026-05-09: Story 1.6 implemented — Vehicle Profile Management UI for head unit (all 5 tasks complete, 72 tests passing, no regressions)

### Review Findings

- [x] [Review][Patch] MainViewModel exposes repositories as public val [MainViewModel.kt:17-18]
- [x] [Review][Patch] Composition side-effect: showSetup mutation during recomposition [MainActivity.kt:49]
- [x] [Review][Patch] toVehicle() silent null return after validation passes [VehicleSetupViewModel.kt:64]
- [x] [Review][Patch] setActiveVehicle and saveMaintenanceSchedules not guarded in save [VehicleSetupViewModel.kt:67-73]
- [x] [Review][Patch] RadioButtonWithLabel double-fires onClick [VehicleSetupScreen.kt:461-471]
- [x] [Review][Patch] ActiveVehicleRepository.activeVehicleId missing IOException catch [ActiveVehicleRepository.kt:20-21]
- [x] [Review][Patch] DashboardShell.formatOdometer() truncates via toLong() [DashboardShell.kt:182]
- [x] [Review][Patch] Enum display uses raw uppercase names [VehicleSetupScreen.kt:219,241]
- [x] [Review][Patch] DashboardShell shows "0 L/100km" for vehicles without consumption data [DashboardShell.kt:104]
- [x] [Review][Patch] FQN reference instead of import in MainActivity [MainActivity.kt:72]
- [x] [Review][Defer] VehicleSwitcherDialog is not modal (no Dialog wrapper) [VehicleSwitcherDialog.kt:36] — deferred, pre-existing design choice for Story 6-3
- [x] [Review][Defer] No @Preview composables in any UI file — deferred, general practice gap
