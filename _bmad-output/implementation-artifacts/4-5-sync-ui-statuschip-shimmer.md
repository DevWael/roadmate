# Story 4.5: Sync UI — StatusChip & Shimmer Refresh

Status: done

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

- [x] Task 1: StatusChip component (AC: #1, #2, #3, #4)
  - [x] Create `core/ui/components/StatusChip.kt`
  - [x] Observe `BluetoothStateManager.state`
  - [x] Real-time "X ago" formatting
  - [x] Pulse animation for syncing state

- [x] Task 2: Shimmer skeleton (AC: #5, #6)
  - [x] Create `core/ui/components/ShimmerSkeleton.kt`
  - [x] surfaceVariant→surfaceBright gradient oscillation
  - [x] 200-400ms duration, applied to card shapes
  - [x] Only trigger when new data flag is set

- [x] Task 3: Integration (AC: #7)
  - [x] No interruptions in driving mode
  - [x] StatusChip updates only

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
GLM-5.1

### Debug Log References
No debug issues encountered during implementation.

### Completion Notes List
- Implemented `StatusChip.kt` with reusable `StatusChip(icon, label, color)` composable and `SyncStatusChip` convenience wrapper
- `formatTimeAgo()` handles relative time ("just now", "5m ago", "2h ago") with 60s refresh via `LaunchedEffect`
- `syncStatusLabel()` resolves label from connection state; `shouldRevertFromFailed()` auto-reverts after 10s
- Pulse animation at 800ms loop using `rememberInfiniteTransition` + `animateFloat` (scale 1.0→1.12)
- Disconnected state uses outlined chip style with `onSurfaceVariant` color
- Implemented `ShimmerSkeleton.kt` with `surfaceVariant→surfaceBright` gradient oscillation, duration clamped 200-400ms (default 300ms)
- `shouldShowShimmer()` only triggers when `hasNewData=true`; no-data sync only updates timestamp (AC #6)
- `isDriving()` + `syncFeedbackType()` ensure driving mode suppresses all UI except StatusChip (AC #7)
- 30 unit tests across 3 test files — all pass, no regressions

### File List
- `core/src/main/kotlin/com/roadmate/core/ui/components/StatusChip.kt` (new)
- `core/src/main/kotlin/com/roadmate/core/ui/components/ShimmerSkeleton.kt` (new)
- `core/src/main/kotlin/com/roadmate/core/ui/components/SyncFeedback.kt` (new)
- `core/src/test/kotlin/com/roadmate/core/ui/components/StatusChipTest.kt` (new)
- `core/src/test/kotlin/com/roadmate/core/ui/components/ShimmerSkeletonTest.kt` (new)
- `core/src/test/kotlin/com/roadmate/core/ui/components/SyncIntegrationTest.kt` (new)

### Change Log
- 2026-05-13: Story 4.5 implementation complete — StatusChip component, ShimmerSkeleton, and driving mode integration

### Review Findings

- [x] [Review][Decision] AC #3 — Missing "!" indicator for sync failed state — resolved: BadgedBox with "!" badge overlay on icon
- [x] [Review][Patch] Production logic defined in test file — extracted to `SyncFeedback.kt`; AC #7 driving mode suppression now in production code
- [x] [Review][Patch] `ShimmerContentWrapper` zero-size bug — added `modifier` parameter, forwarded to `ShimmerSkeleton`
- [x] [Review][Patch] `isFailed && isSyncing` simultaneously — color priority now matches label priority (syncing checked before failed)
- [x] [Review][Patch] `System.currentTimeMillis()` hardcoded — added `timeProvider: () -> Long` parameter with default
- [x] [Review][Patch] `BtConnectionState.Connecting` unmapped — added `isConnecting` parameter with "Connecting..." visual state
- [x] [Review][Patch] `formatTimeAgo` unguarded for negative elapsed — added `if (elapsed < 0L) return "just now"` guard
- [x] [Review][Patch] `shouldRevertFromFailed(failedAtMs=0)` immediately reverts — added `if (failedAtMs <= 0L) return false` guard
- [x] [Review][Patch] Unused `CircleShape` import — removed (along with other unused imports)
- [x] [Review][Patch] `contentDescription = null` — now uses `label` as content description
- [x] [Review][Patch] Empty `onClick = {}` — set `enabled = false` with matching disabled colors to prevent click interaction
- [x] [Review][Defer] No `@Preview` composables in either StatusChip.kt or ShimmerSkeleton.kt — deferred, pre-existing pattern across codebase
