# Story 7.1: Driving Summaries & Statistics

Status: review

## Story

As a driver,
I want to see summaries of my driving, fuel, and maintenance costs over time,
so that I can understand my vehicle usage patterns and total cost of ownership.

## Acceptance Criteria

1. **Period selector** — Day, Week, Month (default), Year.

2. **Statistics** — Total distance, total trips, avg trip distance, total driving time, total fuel cost, total maintenance cost, cost/km.

3. **Month view** — Values from trips, fuel, maintenance within selected calendar month. Query <200ms.

4. **Year view** — Month-by-month breakdown list (distance, fuel cost, maintenance cost) + running total.

5. **Empty period** — All values 0/"—" + "No data for this period".

6. **Week comparison** — Current vs previous week: distance change (↑12%/↓5%), fuel cost change.

## Tasks / Subtasks

- [x] Task 1: Statistics screen (AC: #1, #5)
  - [x] Create `app-phone/ui/statistics/StatisticsScreen.kt`
  - [x] Period selector tabs (Day/Week/Month/Year)
  - [x] Empty state for no-data periods

- [x] Task 2: Statistics calculator (AC: #2, #3)
  - [x] Create `core/util/StatisticsCalculator.kt`
  - [x] Aggregate trips by date range
  - [x] Sum fuel costs, maintenance costs
  - [x] Calculate cost/km

- [x] Task 3: Year breakdown (AC: #4)
  - [x] Month-by-month list with totals
  - [x] Running total at top

- [x] Task 4: Week comparison (AC: #6)
  - [x] Compare current week aggregates vs previous
  - [x] Calculate percentage change

## Dev Notes

### Cost per km calculation:
```kotlin
val costPerKm = if (totalDistanceKm > 0) (totalFuelCost + totalMaintenanceCost) / totalDistanceKm else 0.0
```

### References

- [Source: epics.md#Story 7.1] — Acceptance criteria

## Dev Agent Record

### Agent Model Used
glm-5.1 (via Kilo)

### Debug Log References
- Pre-existing test failures (unrelated): MaintenancePredictionEngineTest (2 failures), FuelLogViewModelTest (1 failure)

### Completion Notes List
- Created StatisticsCalculator as a pure utility object in core/util/ following existing patterns (FuelConsumptionCalculator, HaversineCalculator)
- StatisticsCalculator handles all aggregation logic: date range filtering, trip/fuel/maintenance sums, cost/km, year breakdown, week comparison
- Created StatisticsViewModel following existing ViewModel patterns (HiltViewModel, flatMapLatest on activeVehicleId, combine for multi-source data)
- StatisticsScreen uses ScrollableTabRow for period selector (Day/Week/Month/Year), with empty state handling
- Year view shows running total card + 12 month-by-month breakdown rows
- Week view shows current vs previous week comparison with percentage change (↑/↓ arrows)
- Added Statistics route and wired into NavHost
- Added statistics icon button in VehicleHubScreen top bar
- All new tests pass: StatisticsCalculatorTest (30+ test cases), StatisticsViewModelTest (5 test cases)
- No regressions introduced (3 pre-existing failures unrelated to this story)

### File List
- `core/src/main/kotlin/com/roadmate/core/util/StatisticsCalculator.kt` (NEW)
- `core/src/test/kotlin/com/roadmate/core/util/StatisticsCalculatorTest.kt` (NEW)
- `app-phone/src/main/kotlin/com/roadmate/phone/ui/statistics/StatisticsViewModel.kt` (NEW)
- `app-phone/src/main/kotlin/com/roadmate/phone/ui/statistics/StatisticsScreen.kt` (NEW)
- `app-phone/src/test/kotlin/com/roadmate/phone/ui/statistics/StatisticsViewModelTest.kt` (NEW)
- `app-phone/src/main/kotlin/com/roadmate/phone/navigation/Routes.kt` (MODIFIED)
- `app-phone/src/main/kotlin/com/roadmate/phone/navigation/RoadMateNavHost.kt` (MODIFIED)
- `app-phone/src/main/kotlin/com/roadmate/phone/ui/hub/VehicleHubScreen.kt` (MODIFIED)
- `_bmad-output/implementation-artifacts/sprint-status.yaml` (MODIFIED)

### Review Findings
- [x] [Review][Patch] `formatDistance`/`formatCost` missing `Locale.US` — uses default locale which produces inconsistent number formatting in European locales [StatisticsScreen.kt:L370-380]
- [x] [Review][Patch] Week comparison color semantics inverted for costs — positive cost change colored as primary (good) when it should be error (bad) [StatisticsScreen.kt:L398-403]

## Change Log
- 2026-05-14: Implemented driving summaries & statistics feature (Story 7.1)
- 2026-05-14: Code review patches applied (locale formatting, cost color semantics)
