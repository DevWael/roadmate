# Story 5.4: Fuel Log — Entry, Calculation & Trends

Status: ready-for-dev

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

- [ ] Task 1: Add fuel sheet (AC: #1, #2)
  - [ ] Create `app-phone/ui/fuel/AddFuelSheet.kt`
  - [ ] Auto-calculate total cost
  - [ ] Pre-fill date and ODO

- [ ] Task 2: Consumption calculator (AC: #3, #4)
  - [ ] Calculate actual consumption between full-tank fills
  - [ ] Compare with vehicle's estimated consumption

- [ ] Task 3: FuelLog screen (AC: #5, #6, #7)
  - [ ] Create `app-phone/ui/fuel/FuelLogScreen.kt`
  - [ ] Summary card at top
  - [ ] Entry cards list
  - [ ] Empty state

- [ ] Task 4: Validation (AC: #8)
  - [ ] ODO must be > previous fuel entry ODO
  - [ ] Inline validation with error styling

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
### Debug Log References
### Completion Notes List
### File List
