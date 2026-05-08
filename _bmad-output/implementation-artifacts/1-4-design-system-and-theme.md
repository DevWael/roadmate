# Story 1.4: Shared Design System & Theme

Status: ready-for-dev

## Story

As a developer,
I want a shared RoadMate design system with dark-only theme, typography, spacing, and icon conventions,
so that all UI components across both apps have a consistent automotive look and feel.

## Acceptance Criteria

1. **Dark-only ColorScheme** — `RoadMateTheme` in `core/ui/theme/` provides M3 dark `ColorScheme`: surface #0A0A0A, surfaceVariant #1A1A1A, surfaceContainer #121212, primary #4FC3F7, secondary #80CBC4, tertiary #FFB74D, error #EF5350, onSurface #E8E8E8, onSurfaceVariant #9E9E9E, outline #4A4A4A. No dynamic color/Material You.

2. **Head unit typography** — displayLarge 48sp Bold, headlineMedium 28sp SemiBold, titleLarge 22sp Medium, bodyLarge 18sp Regular, labelLarge 16sp Medium. Inter font family.

3. **Phone typography** — displayLarge 36sp Bold, headlineMedium 24sp SemiBold, titleMedium 16sp Medium, bodyLarge 16sp Regular, bodyMedium 14sp Regular, labelSmall 11sp Medium. Inter font family.

4. **Spacing tokens** — `RoadMateSpacing` object: xs=4dp, sm=8dp, md=12dp, lg=16dp, xl=20dp, 2xl=24dp, 3xl=32dp.

5. **Shape theme** — Card corners 4dp radius (angular automotive style).

6. **UiState sealed interface** — `Loading`, `Success<T>(data: T)`, `Error(message: String)` in `core/model/`.

7. **State sealed interfaces** — `DrivingState` (Idle, Driving, Stopping, GapCheck), `BtConnectionState` (Connected, Disconnected, Connecting, SyncInProgress, SyncFailed), `GpsState` (Acquired, Acquiring, Unavailable) in `core/model/`.

## Tasks / Subtasks

- [ ] Task 1: Color system (AC: #1)
  - [ ] Create `core/ui/theme/Color.kt` with all color tokens as `Color` constants
  - [ ] Create `roadMateDarkColorScheme()` function returning `darkColorScheme(...)`

- [ ] Task 2: Typography (AC: #2, #3)
  - [ ] Add Inter font to `core/src/main/res/font/` (Google Fonts)
  - [ ] Create `core/ui/theme/Type.kt` with `HeadUnitTypography` and `PhoneTypography`

- [ ] Task 3: Spacing & shapes (AC: #4, #5)
  - [ ] Create `core/ui/theme/Spacing.kt` — `RoadMateSpacing` object with dp values
  - [ ] Create `core/ui/theme/Shape.kt` — `RoadMateShapes` with 4dp card corners

- [ ] Task 4: RoadMateTheme composable (AC: #1)
  - [ ] Create `core/ui/theme/RoadMateTheme.kt` — accepts `isHeadUnit: Boolean` parameter for typography selection
  - [ ] No `dynamicColor`, no light theme

- [ ] Task 5: State models (AC: #6, #7)
  - [ ] Create `core/model/UiState.kt`
  - [ ] Create `core/model/DrivingState.kt` — sealed interface with data in Driving/Stopping/GapCheck subtypes
  - [ ] Create `core/model/BtConnectionState.kt`
  - [ ] Create `core/model/GpsState.kt`

## Dev Notes

### Architecture Compliance

**Theme must be dark-only.** No light theme, no dynamic color. The entire automotive UI is designed for dark backgrounds.

**UiState pattern (MUST use everywhere):**
```kotlin
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
```

**DrivingState carries data in subtypes:**
```kotlin
sealed interface DrivingState {
    data object Idle : DrivingState
    data class Driving(val tripId: String, val distanceKm: Double, val durationMs: Long) : DrivingState
    data class Stopping(val timeSinceStopMs: Long) : DrivingState
    data class GapCheck(val gapDurationMs: Long) : DrivingState
}
```

**Inter font:** Download from Google Fonts. Place TTF/OTF files in `core/src/main/res/font/`. Reference via `FontFamily(Font(R.font.inter_regular), ...)`.

### References

- [Source: architecture.md#State Management] — DrivingState, BtConnectionState, GpsState definitions
- [Source: architecture.md#Error Handling] — UiState sealed interface
- [Source: ux-design-specification.md#Color System] — Exact color tokens
- [Source: ux-design-specification.md#Typography] — Head unit and phone scales
- [Source: epics.md#Story 1.4] — Acceptance criteria

## Dev Agent Record

### Agent Model Used
### Debug Log References
### Completion Notes List
### File List
