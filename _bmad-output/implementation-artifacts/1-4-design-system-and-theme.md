# Story 1.4: Shared Design System & Theme

Status: done

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

- [x] Task 1: Color system (AC: #1)
  - [x] Create `core/ui/theme/Color.kt` with all color tokens as `Color` constants
  - [x] Create `roadMateDarkColorScheme()` function returning `darkColorScheme(...)`

- [x] Task 2: Typography (AC: #2, #3)
  - [x] Add Inter font to `core/src/main/res/font/` (Google Fonts)
  - [x] Create `core/ui/theme/Type.kt` with `HeadUnitTypography` and `PhoneTypography`

- [x] Task 3: Spacing & shapes (AC: #4, #5)
  - [x] Create `core/ui/theme/Spacing.kt` — `RoadMateSpacing` object with dp values
  - [x] Create `core/ui/theme/Shape.kt` — `RoadMateShapes` with 4dp card corners

- [x] Task 4: RoadMateTheme composable (AC: #1)
  - [x] Create `core/ui/theme/RoadMateTheme.kt` — accepts `isHeadUnit: Boolean` parameter for typography selection
  - [x] No `dynamicColor`, no light theme

- [x] Task 5: State models (AC: #6, #7)
  - [x] Create `core/model/UiState.kt`
  - [x] Create `core/model/DrivingState.kt` — sealed interface with data in Driving/Stopping/GapCheck subtypes
  - [x] Create `core/model/BtConnectionState.kt`
  - [x] Create `core/model/GpsState.kt`

### Review Findings

- [x] [Review][Patch] `roadMateDarkColorScheme()` allocates new `ColorScheme` on every call — cache as private val [Color.kt:37-54]
- [x] [Review][Patch] Head unit Typography leaves 10/15 slots at M3 defaults — undocumented fallback sizes may be too small for automotive [Type.kt:21-93]
- [x] [Review][Patch] Phone Typography leaves 9/15 slots at M3 defaults — same issue for phone scale [Type.kt:55-93]
- [x] [Review][Patch] `Shapes` missing `extraSmall`/`extraLarge` slot definitions — bottom sheets/dialogs use non-4dp M3 defaults [Shape.kt:20-24]
- [x] [Review][Patch] `UiState.Error` carries only `String`, no `Throwable` or typed error — lost stack traces [UiState.kt:10]
- [x] [Review][Patch] `BtConnectionState.SyncFailed` carries no error context — cannot display failure reason or decide retry strategy [BtConnectionState.kt:11]
- [x] [Review][Defer] `DrivingState.Driving.tripId` is untyped `String` — value class wrapper deferred to trip tracking epic

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
Claude Opus 4.6 (Thinking)

### Debug Log References
No issues encountered during implementation.

### Completion Notes List
- **Task 1 (Color):** Created Color.kt with 10 named color constants and `roadMateDarkColorScheme()` function. All color values verified via unit tests against hex spec. darkColorScheme() sets primary, secondary, tertiary, error, surface, surfaceVariant, surfaceContainer, onSurface, onSurfaceVariant, outline, background, and their inverse content colors.
- **Task 2 (Typography):** Downloaded Inter font (Regular/Medium/SemiBold/Bold static TTFs) from Google Fonts to `core/src/main/res/font/`. Created Type.kt with `HeadUnitTypographyTokens` (5 styles) and `PhoneTypographyTokens` (6 styles) as `Typography` objects. `roadMateTypography(isHeadUnit)` applies InterFontFamily to all styles. Unit tests verify every fontSize/fontWeight combination.
- **Task 3 (Spacing & Shape):** Created Spacing.kt with `RoadMateSpacing` object (7 tokens: xs through 3xl). Created Shape.kt with `RoadMateCorners.card = 4.dp` and `RoadMateShapes` M3 Shapes instance. All values tested.
- **Task 4 (Theme):** Created RoadMateTheme.kt composable that wires colorScheme, typography (head-unit/phone variant), and shapes into MaterialTheme. No dynamicColor, no light theme.
- **Task 5 (State models):** Created UiState<T> (Loading/Success/Error), DrivingState (Idle/Driving/Stopping/GapCheck with data), BtConnectionState (5 states), GpsState (3 states) as sealed interfaces in core/model/. All subtypes verified with unit tests including exhaustive when-expression compilation checks.
- **Regression:** Full suite: 199 tests, 0 failures.

### File List
- core/src/main/kotlin/com/roadmate/core/ui/theme/Color.kt [NEW]
- core/src/main/kotlin/com/roadmate/core/ui/theme/Type.kt [NEW]
- core/src/main/kotlin/com/roadmate/core/ui/theme/Spacing.kt [NEW]
- core/src/main/kotlin/com/roadmate/core/ui/theme/Shape.kt [NEW]
- core/src/main/kotlin/com/roadmate/core/ui/theme/RoadMateTheme.kt [NEW]
- core/src/main/kotlin/com/roadmate/core/model/UiState.kt [NEW]
- core/src/main/kotlin/com/roadmate/core/model/DrivingState.kt [NEW]
- core/src/main/kotlin/com/roadmate/core/model/BtConnectionState.kt [NEW]
- core/src/main/kotlin/com/roadmate/core/model/GpsState.kt [NEW]
- core/src/main/res/font/inter_regular.ttf [NEW]
- core/src/main/res/font/inter_medium.ttf [NEW]
- core/src/main/res/font/inter_semibold.ttf [NEW]
- core/src/main/res/font/inter_bold.ttf [NEW]
- core/src/test/kotlin/com/roadmate/core/ui/theme/ColorTest.kt [NEW]
- core/src/test/kotlin/com/roadmate/core/ui/theme/TypeTest.kt [NEW]
- core/src/test/kotlin/com/roadmate/core/ui/theme/SpacingShapeTest.kt [NEW]
- core/src/test/kotlin/com/roadmate/core/model/UiStateTest.kt [NEW]
- core/src/test/kotlin/com/roadmate/core/model/DrivingStateTest.kt [NEW]
- core/src/test/kotlin/com/roadmate/core/model/BtConnectionStateTest.kt [NEW]
- core/src/test/kotlin/com/roadmate/core/model/GpsStateTest.kt [NEW]

## Change Log

- 2026-05-09: Story 1.4 implemented — dark-only color scheme, head-unit/phone typography with Inter font, spacing tokens, 4dp angular shapes, RoadMateTheme composable, UiState/DrivingState/BtConnectionState/GpsState sealed interfaces. 20 new files, 199 total tests passing.
