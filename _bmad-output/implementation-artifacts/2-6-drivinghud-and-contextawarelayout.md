# Story 2.6: DrivingHUD & ContextAwareLayout

Status: ready-for-dev

## Story

As a driver,
I want a minimal heads-up display while driving and an automatic switch between driving and parked views,
so that I can glance at essential information without distraction.

## Acceptance Criteria

1. **ContextAwareLayout switching** — Displays `DrivingHUD` when `Driving`, parked dashboard when `Idle`.

2. **DrivingHUD layout** — ODO in displayLarge left-center, trip distance in titleLarge teal below ODO, `TripLiveIndicator` (12dp pulsing green circle, 1.0→1.3→1.0 scale, 1500ms) top-center, current time titleLarge top-right. ZERO interactive elements.

3. **Transition animation** — `FastOutSlowIn` 400ms. Instant if `AccessibilityManager.isReduceMotionEnabled`.

4. **GPS acquiring indicator** — `TripLiveIndicator` shows gray static circle when `GpsState.Acquiring`.

5. **Maintenance alert strip** — At bottom edge: red (#EF5350) background, full-width, non-interactive, single-line. Shows when maintenance ≥95%. Hidden if no alerts.

6. **Return to parked** — Driving→Idle triggers same 400ms transition back to parked dashboard.

## Tasks / Subtasks

- [ ] Task 1: ContextAwareLayout (AC: #1, #3)
  - [ ] Create `app-headunit/ui/ContextAwareLayout.kt`
  - [ ] Observe `DrivingStateManager.drivingState`
  - [ ] AnimatedContent with FastOutSlowIn 400ms
  - [ ] Accessibility: instant switch if reduce motion enabled

- [ ] Task 2: DrivingHUD (AC: #2)
  - [ ] Create `app-headunit/ui/driving/DrivingHUD.kt`
  - [ ] ODO displayLarge left-center
  - [ ] Trip distance titleLarge teal (#80CBC4)
  - [ ] Time display top-right
  - [ ] Zero clickable elements — all `Modifier.clickable` forbidden

- [ ] Task 3: TripLiveIndicator (AC: #2, #4)
  - [ ] Create `core/ui/components/TripLiveIndicator.kt`
  - [ ] 12dp circle, green pulsing (scale 1.0→1.3→1.0, 1500ms `InfiniteTransition`)
  - [ ] Gray static when `GpsState.Acquiring`

- [ ] Task 4: Alert strip (AC: #5)
  - [ ] Create `core/ui/components/AlertStrip.kt`
  - [ ] Query maintenance items ≥95% progress
  - [ ] Red background, white text, full-width, non-interactive

## Dev Notes

### Architecture Compliance

**TripLiveIndicator animation:**
```kotlin
val scale by rememberInfiniteTransition().animateFloat(
    initialValue = 1f, targetValue = 1.3f,
    animationSpec = infiniteRepeatable(
        animation = tween(750, easing = LinearEasing),
        repeatMode = RepeatMode.Reverse
    )
)
```

**Zero interactivity in driving mode** is a HARD safety constraint. No buttons, no taps, no swipe gestures.

### References

- [Source: architecture.md#Driving Mode] — HUD layout, safety constraints
- [Source: ux-design-specification.md#DrivingHUD] — Component specs
- [Source: ux-design-specification.md#Animations] — Transition timing
- [Source: epics.md#Story 2.6] — Acceptance criteria

## Dev Agent Record

### Agent Model Used
### Debug Log References
### Completion Notes List
### File List
