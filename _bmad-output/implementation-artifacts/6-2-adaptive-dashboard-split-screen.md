# Story 6.2: AdaptiveDashboard & Split-Screen Support

Status: ready-for-dev

## Story

As a driver,
I want the head unit dashboard to adapt when running side-by-side with navigation apps,
so that I can see my vehicle info alongside Google Maps without either app breaking.

## Acceptance Criteria

1. **Full-screen (≥960dp)** — Full three-panel parked / full DrivingHUD.

2. **Half-screen (480-959dp)** — Parked: two-panel (ODO+status, maintenance). Trips hidden. Driving: compact HUD (ODO+trip stacked, dot, no time). Alert strip stays.

3. **Narrow (≤479dp)** — Parked: single column (ODO card, maintenance scroll). Driving: ODO + dot only. Alert = icon only.

4. **Touch targets** — 76dp minimum always. Fewer items, not smaller items.

5. **ODO always visible** — Non-negotiable across all widths.

6. **Recording indicator always visible** — In all driving breakpoints.

7. **Alert strip degradation** — Full text (≥960) → icon+abbreviated (480-959) → dot only (≤479).

8. **Live mode switch** — Adapts immediately when entering/exiting multi-window.

## Tasks / Subtasks

- [ ] Task 1: AdaptiveDashboard (AC: #1, #2, #3, #8)
  - [ ] Create `app-headunit/ui/AdaptiveDashboard.kt`
  - [ ] Use `BoxWithConstraints` to measure available width
  - [ ] 3 layout breakpoints: ≥960, 480-959, ≤479

- [ ] Task 2: Compact driving HUD (AC: #2, #3, #6)
  - [ ] ODO + trip stacked vertically for half-screen
  - [ ] ODO only for narrow
  - [ ] Recording indicator always present

- [ ] Task 3: Alert strip variants (AC: #7)
  - [ ] Full text, icon+text, dot-only variants
  - [ ] Select based on current breakpoint

## Dev Notes

### Architecture Compliance

**BoxWithConstraints pattern:**
```kotlin
@Composable
fun AdaptiveDashboard(drivingState: DrivingState) {
    BoxWithConstraints {
        val width = maxWidth
        when {
            width >= 960.dp -> FullDashboard(drivingState)
            width >= 480.dp -> CompactDashboard(drivingState)
            else -> MinimalDashboard(drivingState)
        }
    }
}
```

**`isInMultiWindowMode`** — Check via `LocalActivity.current.isInMultiWindowMode`. Recomposition handles layout changes automatically.

### References

- [Source: ux-design-specification.md#Adaptive Layout] — Breakpoints
- [Source: architecture.md#Split-Screen] — Width thresholds, degradation
- [Source: epics.md#Story 6.2] — Acceptance criteria

## Dev Agent Record

### Agent Model Used
### Debug Log References
### Completion Notes List
### File List
