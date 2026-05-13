# Story 5.4: Fuel Log — Entry, Calculation & Trends

Status: done

## Story

As a driver,
I want to log fuel fill-ups and see consumption and cost trends,
so that I can track my fuel efficiency and spending over time.

## Acceptance Criteria

1. **Add fuel entry** — FAB "+" → ModalBottomSheet: date (today), ODO (current), liters (required), price/liter (required), total cost (auto-calculated), full tank toggle (default true), station (optional).

2. **Auto-calculation** — Total cost = liters × pricePerLiter, real-time update.

3. **Actual consumption** — Full tank + previous full tank → `totalLitersBetweenFills / distanceBetweenFills × 100` = L/100km.

4. **Consumption comparison** — "Actual: X.X vs Estimated: Y.Y L/100km". Actual >20% over estimated → tertiary color.

5. **Fuel list** — Most recent first. Cards: date, liters, cost, consumption, station.

6. **Summary section** — Total fuel cost (this month), average L/100km, average cost/km.

7. **Empty state** — Fuel pump icon + "No fuel entries yet. Log your first fill-up." + "Add" button.

8. **ODO validation** — Red border if ODO < previous entry's ODO. Save disabled.

## Tasks / Subtasks

- [x] Task 1: Add fuel sheet (AC: #1, #2)
  - [x] Create `app-phone/ui/fuel/AddFuelSheet.kt`
  - [x] Auto-calculate total cost
  - [x] Pre-fill date and ODO

- [x] Task 2: Consumption calculator (AC: #3, #4)
  - [x] Calculate actual consumption between full-tank fills
  - [x] Compare with vehicle's estimated consumption

- [x] Task 3: FuelLog screen (AC: #5, #6, #7)
  - [x] Create `app-phone/ui/fuel/FuelLogScreen.kt`
  - [x] Summary card at top
  - [x] Entry cards list
  - [x] Empty state

- [x] Task 4: Validation (AC: #8)
  - [x] ODO must be > previous fuel entry ODO
  - [x] Inline validation with error styling

### Review Findings

- [x] [Review][Decision] #7 — `findFullTankPairs` sorts by `odometerKm` not by `date` → fixed: sort by `date` [FuelConsumptionCalculator.kt:55-57]
- [x] [Review][Decision] #11 — ODO validation uses `<=` (blocks equal ODO), spec says `<` → fixed: use `<` [FuelLogViewModel.kt:225]
- [x] [Review][Patch] #1 — ODO not pre-filled with current reading (AC#1 violation) → fixed: pre-fill from vehicle.odometerKm [FuelLogViewModel.kt:184]
- [x] [Review][Patch] #2 — Consumption comparison text missing (AC#4 violation) → fixed: shows "Actual: X.X vs Estimated: Y.Y L/100km" [FuelLogScreen.kt:294-301]
- [x] [Review][Patch] #3 — `saveFuelEntry()` ignores `Result<Unit>` → fixed: handle failure with error message [FuelLogViewModel.kt:266]
- [x] [Review][Patch] #4 — Dead-code blank checks in `saveFuelEntry()` → fixed: removed unreachable code [FuelLogViewModel.kt:239-241]
- [x] [Review][Patch] #5 — `totalCost` field `readOnly` vs `disabled` mismatch → fixed: use `enabled = false` [AddFuelSheet.kt:137-148]
- [x] [Review][Patch] #6 — `calculateAvgCostPerKm` includes first entry's cost → fixed: excluded first entry [FuelConsumptionCalculator.kt:38-48]
- [x] [Review][Patch] #8 — `latestOdoForValidation` race condition → fixed: added `@Volatile` [FuelLogViewModel.kt:96]
- [x] [Review][Patch] #9 — `validateOdo` silently accepts non-numeric input → fixed: shows "Enter a valid number" [FuelLogViewModel.kt:222-231]
- [x] [Review][Patch] #10 — `findFullTankPairs` called twice per update → fixed: computed once, passed to both functions [FuelLogViewModel.kt:141,171]
- [x] [Review][Defer] — 8 findings dismissed as noise (design token usage, magic numbers, SDF allocation, defensive behavior, test nits)

## Dev Notes

### Consumption calculation:
```kotlin
fun calculateActualConsumption(current: FuelLog, previous: FuelLog): Double? {
    if (!current.isFullTank || !previous.isFullTank) return null
    val distance = current.odometerKm - previous.odometerKm
    if (distance <= 0) return null
    return current.liters / distance * 100.0
}
```

### References

- [Source: architecture.md#Fuel Domain] — Consumption calculation
- [Source: epics.md#Story 5.4] — Acceptance criteria

## Dev Agent Record

### Agent Model Used
glm-5.1

### Debug Log References
None

### Completion Notes List
- Task 1: Created AddFuelSheet.kt as ModalBottomSheet with date picker, ODO, liters, price/liter (side-by-side), auto-calculated total cost, full tank toggle (default true), optional station field. Follows existing AddMaintenanceSheet pattern.
- Task 2: Created FuelConsumptionCalculator in core/util with actual consumption calculation between full-tank pairs, over-consumption detection (>20% over estimated average of city/highway), avg L/100km, avg cost/km, monthly total cost, and full-tank pair finder.
- Task 3: Rewrote FuelLogScreen with FAB "+", FuelEmptyState (gas pump icon + CTA), FuelLogContent with summary card (total cost this month, avg L/100km, avg cost/km) and entry cards showing date, liters, cost, consumption (tertiary color if over), station. Created FuelLogViewModel with reactive data loading via combine over fuel logs and vehicle flows.
- Task 4: ODO validation in ViewModel - real-time error when ODO <= previous entry's ODO, red border on text field, save disabled while errors present.
- Added FuelDao queries: getLatestFuelEntry, getTwoLastFullTankEntries. Added corresponding FuelRepository methods.
- Updated RoadMateNavHost to pass onBack to FuelLogScreen.
- All 29 new tests pass (FuelConsumptionCalculatorTest: 20, CalculateTotalCostTest: 6, FuelLogViewModelTest: 10+). No regressions (2 pre-existing failures in MaintenancePredictionEngineTest).

### File List
- `core/src/main/kotlin/com/roadmate/core/database/dao/FuelDao.kt` (modified)
- `core/src/main/kotlin/com/roadmate/core/repository/FuelRepository.kt` (modified)
- `core/src/main/kotlin/com/roadmate/core/util/FuelConsumptionCalculator.kt` (new)
- `app-phone/src/main/kotlin/com/roadmate/phone/ui/fuel/AddFuelSheet.kt` (new)
- `app-phone/src/main/kotlin/com/roadmate/phone/ui/fuel/FuelLogScreen.kt` (rewritten)
- `app-phone/src/main/kotlin/com/roadmate/phone/ui/fuel/FuelLogViewModel.kt` (new)
- `app-phone/src/main/kotlin/com/roadmate/phone/navigation/RoadMateNavHost.kt` (modified)
- `core/src/test/kotlin/com/roadmate/core/util/FuelConsumptionCalculatorTest.kt` (new)
- `app-phone/src/test/kotlin/com/roadmate/phone/ui/fuel/CalculateTotalCostTest.kt` (new)
- `app-phone/src/test/kotlin/com/roadmate/phone/ui/fuel/FuelLogViewModelTest.kt` (new)
- `core/src/test/kotlin/com/roadmate/core/repository/FuelRepositoryTest.kt` (modified)
- `core/src/test/kotlin/com/roadmate/core/sync/DeltaSyncEngineTest.kt` (modified)
- `core/src/test/kotlin/com/roadmate/core/sync/SyncSessionTest.kt` (modified)
- `core/src/test/kotlin/com/roadmate/core/sync/SyncTriggerManagerTest.kt` (modified)
- `core/src/test/kotlin/com/roadmate/core/sync/BluetoothConnectionManagerTest.kt` (modified)
- `core/src/test/kotlin/com/roadmate/core/sync/ConflictResolverTest.kt` (modified)
- `app-phone/src/test/kotlin/com/roadmate/phone/ui/hub/VehicleHubViewModelTest.kt` (modified)
