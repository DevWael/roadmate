# Story 4.5: Sync UI — StatusChip & Shimmer Refresh

Status: ready-for-dev

## Story

As a driver,
I want to see the sync status at a glance and have the UI refresh smoothly after sync,
so that I know my data is current and trust the system is working.

## Acceptance Criteria

1. **Connected idle** — BT icon + "Synced [X] ago" in primary. Time updates real-time.

2. **Syncing** — BT icon + "Syncing..." with pulse (800ms loop). State reflected within 16ms.

3. **Sync failed** — BT icon + "!" + "Sync failed" in error. Reverts to last sync time after 10s.

4. **Disconnected** — BT icon (outlined) + "Not connected" in onSurfaceVariant.

5. **Shimmer refresh** — New data received → cards show shimmer skeleton (surfaceVariant→surfaceBright, 200-400ms) → populate.

6. **No-data sync** — No shimmer. Only StatusChip timestamp updates.

7. **Driving mode** — Sync completes silently. No snackbar/toast/modal.

## Tasks / Subtasks

- [ ] Task 1: StatusChip component (AC: #1, #2, #3, #4)
  - [ ] Create `core/ui/components/StatusChip.kt`
  - [ ] Observe `BluetoothStateManager.state`
  - [ ] Real-time "X ago" formatting
  - [ ] Pulse animation for syncing state

- [ ] Task 2: Shimmer skeleton (AC: #5, #6)
  - [ ] Create `core/ui/components/ShimmerSkeleton.kt`
  - [ ] surfaceVariant→surfaceBright gradient oscillation
  - [ ] 200-400ms duration, applied to card shapes
  - [ ] Only trigger when new data flag is set

- [ ] Task 3: Integration (AC: #7)
  - [ ] No interruptions in driving mode
  - [ ] StatusChip updates only

## Dev Notes

### Architecture Compliance

**StatusChip is reusable** for sync status and tracking status. Parameterize: `StatusChip(icon, label, color)`.

**Shimmer implementation:**
```kotlin
val shimmerTransition = rememberInfiniteTransition()
val translateAnim by shimmerTransition.animateFloat(
    initialValue = 0f, targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(300, easing = LinearEasing))
)
```

**Time ago formatting:** Use relative time: "just now", "2m ago", "1h ago". Update via `LaunchedEffect` + `delay(60_000)`.

### References

- [Source: ux-design-specification.md#StatusChip] — Component specs
- [Source: ux-design-specification.md#Shimmer] — Skeleton animation
- [Source: epics.md#Story 4.5] — Acceptance criteria

## Dev Agent Record

### Agent Model Used
### Debug Log References
### Completion Notes List
### File List
