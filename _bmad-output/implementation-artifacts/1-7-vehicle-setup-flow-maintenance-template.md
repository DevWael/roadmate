# Story 1.7: First-Time Setup Flow (Head Unit)

Status: ready-for-dev

## Story

As a first-time driver,
I want a minimal setup experience that gets tracking running in under 2 minutes,
so that I start getting value immediately without a lengthy configuration process.

## Acceptance Criteria

1. **Welcome screen** — Single-screen: vehicle name input, odometer input (number keyboard), "Start Tracking" primary button. No onboarding tour, no feature walkthrough, no account creation.

2. **Start tracking flow** — Creates Vehicle entity, starts foreground service, begins GPS acquisition, shows dashboard within 15 seconds. Notification reads "RoadMate tracking active".

3. **Skip on subsequent launches** — If ≥1 vehicle exists, welcome flow skipped; dashboard shown with last active vehicle.

4. **Minimal inputs** — Only 2 fields (name, ODO) + 1 tap. Completable in <60 seconds.

5. **No template required** — Vehicle created with zero maintenance schedules if no template selected.

## Tasks / Subtasks

- [ ] Task 1: Welcome screen (AC: #1, #4)
  - [ ] Create `app-headunit/ui/WelcomeScreen.kt` — single screen, 2 inputs
  - [ ] Vehicle name TextField, ODO NumberField (number keyboard)
  - [ ] "Start Tracking" primary button (76dp+ touch target)
  - [ ] Dark theme, head unit typography

- [ ] Task 2: Setup flow logic (AC: #2)
  - [ ] Create `WelcomeViewModel.kt`
  - [ ] On "Start Tracking": create Vehicle via repository, set active in DataStore, trigger service start
  - [ ] Navigate to dashboard on completion

- [ ] Task 3: Navigation gate (AC: #3)
  - [ ] In `MainActivity.kt`, check DataStore for active vehicle on launch
  - [ ] If exists → DashboardShell; if not → WelcomeScreen
  - [ ] No splash screen, no loading — direct decision

- [ ] Task 4: Integration test (AC: #2, #3)
  - [ ] Verify fresh install → WelcomeScreen
  - [ ] Verify post-setup → Dashboard on relaunch

## Dev Notes

### Architecture Compliance

**Navigation gate logic:**
```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setContent {
            RoadMateTheme(isHeadUnit = true) {
                val hasVehicle by viewModel.hasVehicle.collectAsStateWithLifecycle()
                if (hasVehicle == true) DashboardShell() else WelcomeScreen()
            }
        }
    }
}
```

**Under 60 seconds UX constraint.** Only 2 fields. No pagination, no steps, no progress indicator.

### References

- [Source: architecture.md#First-Time Experience] — Minimal setup pattern
- [Source: ux-design-specification.md#Welcome Flow] — Single screen design
- [Source: epics.md#Story 1.7] — Acceptance criteria

## Dev Agent Record

### Agent Model Used
### Debug Log References
### Completion Notes List
### File List
