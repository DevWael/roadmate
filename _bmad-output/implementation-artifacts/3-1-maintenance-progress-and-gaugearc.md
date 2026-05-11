# Story 3.1: Maintenance Progress Calculation & GaugeArc Component

Status: done

## Story

As a driver,
I want to see a visual gauge showing how close each maintenance item is to being due,
so that I can tell at a glance which services need attention.

## Acceptance Criteria

1. **Progress calculation** — `(kmDriven - lastServiceKm) / intervalKm * 100`. If both km and month intervals set, higher percentage wins.

2. **GaugeArc Large (160dp)** — 0-74% secondary (#80CBC4), 75-94% tertiary (#FFB74D), 95-100% error (#EF5350) with pulse (1.5s). Center text = percentage.

3. **GaugeArc reset animation** — On completion: old% → 0% over 600ms LinearOutSlowIn.

4. **GaugeArc Compact (48dp)** — Same color logic, no center text.

5. **Accessibility** — TalkBack: "[Item name]: [X]% complete, [Y km] remaining until due".

6. **Reduce motion** — All animations disabled when `isReduceMotionEnabled`.

## Tasks / Subtasks

- [x] Task 1: Progress calculator (AC: #1)
  - [x] Create `core/util/MaintenanceProgressCalculator.kt`
  - [x] Calculate km-based progress: `(currentOdo - lastServiceKm) / intervalKm * 100`
  - [x] Calculate month-based progress: `monthsSinceService / intervalMonths * 100`
  - [x] Return `max(kmProgress, monthProgress)`

- [x] Task 2: GaugeArc composable (AC: #2, #3, #4)
  - [x] Create `core/ui/components/GaugeArc.kt`
  - [x] `Large` variant: 160dp, arc + center text
  - [x] `Compact` variant: 48dp, arc only
  - [x] Color thresholds: 0-74 secondary, 75-94 tertiary, 95-100 error
  - [x] Critical pulse: `InfiniteTransition` alpha 0.6→1.0, 1.5s

- [x] Task 3: Reset animation (AC: #3)
  - [x] `animateFloatAsState` from oldPercent → 0, 600ms LinearOutSlowIn
  - [x] Triggered when `lastServiceKm` or `lastServiceDate` updates

- [x] Task 4: Accessibility (AC: #5, #6)
  - [x] `semantics { contentDescription = "..." }` with item name, percent, remaining km
  - [x] Disable all animations when `isReduceMotionEnabled`

## Dev Notes

### Architecture Compliance

**GaugeArc uses Canvas drawing:**
```kotlin
@Composable
fun GaugeArc(percentage: Float, variant: GaugeArcVariant, modifier: Modifier) {
    val sweepAngle = percentage / 100f * 270f // 270° arc
    Canvas(modifier.size(if (variant == Large) 160.dp else 48.dp)) {
        drawArc(color = arcColor, startAngle = 135f, sweepAngle = sweepAngle, ...)
    }
}
```

### References

- [Source: ux-design-specification.md#GaugeArc] — Component specs
- [Source: architecture.md#Maintenance Logic] — Progress calculation
- [Source: epics.md#Story 3.1] — Acceptance criteria

## Dev Agent Record

### Agent Model Used
GLM-5.1

### Debug Log References
No issues encountered during implementation.

### Completion Notes List
- Task 1: Created `MaintenanceProgressCalculator` object with `calculate()` for km/month progress (max of both) and `remainingKm()` helper. All edge cases handled: null intervals, values beyond 100% clamped, negative progress clamped to 0.
- Task 2: Created `GaugeArc` composable with `GaugeArcVariant` enum (Large=160dp, Compact=48dp). Canvas-based arc drawing with 270° sweep from 135° start. Color thresholds via `gaugeArcColor()`. Background arc uses RoadMateOutline.
- Task 3: Reset animation uses `animateFloatAsState` with 600ms `LinearOutSlowInEasing`, gated by `shouldAnimateReset()` helper.
- Task 4: Accessibility via `semantics { contentDescription }` using `gaugeContentDescription()` format. Reduce motion detection from `Settings.Global.TRANSITION_ANIMATION_SCALE` disables all animations (reset + pulse).
- Critical pulse: `InfiniteTransition` alpha 0.6→1.0 over 1.5s, only active at ≥95% with reduce motion off.

### File List
- core/src/main/kotlin/com/roadmate/core/util/MaintenanceProgressCalculator.kt (new)
- core/src/main/kotlin/com/roadmate/core/ui/components/GaugeArc.kt (new)
- core/src/test/kotlin/com/roadmate/core/util/MaintenanceProgressCalculatorTest.kt (new)
- core/src/test/kotlin/com/roadmate/core/ui/components/GaugeArcTest.kt (new)

### Review Findings

- [x] [Review][Decision] Accessibility wording: "complete" vs "used" — UX spec L1139 says "[X]% used", story spec says "[X]% complete". Implementation follows story spec. Which wording is canonical? → Resolved: use UX spec wording.
- [x] [Review][Decision] Stroke width deviation — UX spec L793 says 8dp stroke for arc track. Code uses 12dp (Large) / 4dp (Compact). Intentional divergence or spec drift? → Resolved: 8dp Large, 4dp Compact.
- [x] [Review][Decision] Success color not used for reset state — UX spec L805 defines Completed state as success (#66BB6A). Code uses gaugeArcColor(0f) → secondary. Should reset flash green before settling? → Resolved: add success color during reset animation.
- [x] [Review][Patch] Month calculation truncates fractional months [MaintenanceProgressCalculator.kt:21] — integer division on Long drops sub-30-day precision. Use floating-point division.
- [x] [Review][Patch] Settings.Global read on every recomposition [GaugeArc.kt:76-84] — cache reduce-motion check or switch to AccessibilityManager API per spec.
- [x] [Review][Patch] Center text uses raw TextStyle instead of theme headlineMedium [GaugeArc.kt:183-184] — UX spec L795 requires headlineMedium.
- [x] [Review][Patch] displayPercentage.toInt() truncates instead of rounding [GaugeArc.kt:182] — 99.5% shows "99%". Use roundToInt().
- [x] [Review][Patch] GaugeArcTest uses assert() instead of assertEquals() [GaugeArcTest.kt:248,254,261] — assert() may be disabled, silently passing.
- [x] [Review][Patch] Month-based tests use System.currentTimeMillis() [MaintenanceProgressCalculatorTest.kt:93,109,125,141,160,178] — use deterministic timestamps for CI stability.
- [x] [Review][Defer] No @Stable annotation on GaugeArcVariant enum [GaugeArc.kt:33] — deferred, pre-existing pattern across project enums

## Change Log
- 2026-05-11: Implemented story 3-1 — Maintenance progress calculator and GaugeArc composable with all ACs satisfied.
- 2026-05-11: Code review complete — 9 patches applied (3 decisions resolved, 6 patches fixed), 1 deferred, 4 dismissed.
