# Story 6.3: Head Unit Vehicle Switcher & D-pad Navigation

Status: ready-for-dev

## Story

As a driver,
I want to switch vehicles and navigate the dashboard with hardware controls on my head unit,
so that I can manage multiple vehicles and access information even with a rotary controller or D-pad.

## Acceptance Criteria

1. **Vehicle list** — Tap vehicle name in header → large-touch-target list (76dp items) with name + ODO.

2. **Switch action** — Updates DataStore, dashboard refreshes immediately with new vehicle data.

3. **Focus ring** — D-pad/rotary: 1dp primary (#4FC3F7) focus ring. Cycles through panels: left→center→right.

4. **Trip expand** — D-pad Select on trip card → expand inline (distance, duration, avg speed, date). Select again collapses.

5. **GaugeArc expand** — D-pad Select on GaugeArc → show name, percentage, predicted service date.

6. **No driving interaction** — D-pad/rotary has no effect in driving mode.

7. **Focus order** — Modifier.focusOrder: left→center→right panels, top→bottom within each.

## Tasks / Subtasks

- [ ] Task 1: Vehicle switcher (AC: #1, #2)
  - [ ] Create vehicle list overlay with 76dp items
  - [ ] Persist selection to DataStore

- [ ] Task 2: D-pad focus system (AC: #3, #7)
  - [ ] Add `Modifier.focusable()` to interactive elements
  - [ ] Focus ring: 1dp primary color border
  - [ ] Focus order: left→center→right, top→bottom

- [ ] Task 3: Expand/collapse (AC: #4, #5)
  - [ ] AnimatedVisibility for trip card expansion
  - [ ] AnimatedVisibility for GaugeArc detail expansion

- [ ] Task 4: Driving mode lock (AC: #6)
  - [ ] Disable all focus targets when `drivingState != Idle`

## Dev Notes

### Architecture Compliance

**Focus ring pattern:**
```kotlin
Modifier
    .focusable()
    .onFocusChanged { if (it.isFocused) /* show ring */ }
    .border(if (isFocused) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else BorderStroke(0.dp, Color.Transparent))
```

**D-pad = hardware key events.** Android handles focus traversal natively via `FocusRequester` and `focusProperties`.

### References

- [Source: ux-design-specification.md#D-pad Navigation] — Focus system
- [Source: architecture.md#Head Unit Input] — Hardware controls
- [Source: epics.md#Story 6.3] — Acceptance criteria

## Dev Agent Record

### Agent Model Used
### Debug Log References
### Completion Notes List
### File List
