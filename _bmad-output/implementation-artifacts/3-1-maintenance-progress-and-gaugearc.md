# Story 3.1: Maintenance Progress Calculation & GaugeArc Component

Status: ready-for-dev

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

- [ ] Task 1: Progress calculator (AC: #1)
  - [ ] Create `core/util/MaintenanceProgressCalculator.kt`
  - [ ] Calculate km-based progress: `(currentOdo - lastServiceKm) / intervalKm * 100`
  - [ ] Calculate month-based progress: `monthsSinceService / intervalMonths * 100`
  - [ ] Return `max(kmProgress, monthProgress)`

- [ ] Task 2: GaugeArc composable (AC: #2, #3, #4)
  - [ ] Create `core/ui/components/GaugeArc.kt`
  - [ ] `Large` variant: 160dp, arc + center text
  - [ ] `Compact` variant: 48dp, arc only
  - [ ] Color thresholds: 0-74 secondary, 75-94 tertiary, 95-100 error
  - [ ] Critical pulse: `InfiniteTransition` alpha 0.6→1.0, 1.5s

- [ ] Task 3: Reset animation (AC: #3)
  - [ ] `animateFloatAsState` from oldPercent → 0, 600ms LinearOutSlowIn
  - [ ] Triggered when `lastServiceKm` or `lastServiceDate` updates

- [ ] Task 4: Accessibility (AC: #5, #6)
  - [ ] `semantics { contentDescription = "..." }` with item name, percent, remaining km
  - [ ] Disable all animations when `isReduceMotionEnabled`

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
### Debug Log References
### Completion Notes List
### File List
