# Story 1.1: Project Scaffold & Build System

Status: ready-for-dev

## Story

As a developer,
I want a multi-module Kotlin Gradle project with convention plugins and version catalog,
so that all modules share consistent build configuration and I can begin implementing features across `:app-headunit`, `:app-phone`, and `:core`.

## Acceptance Criteria

1. **Gradle sync succeeds** — Opening the project in Android Studio and triggering Gradle sync completes successfully. `settings.gradle.kts` includes `:app-headunit`, `:app-phone`, `:core`, and `:build-logic`.

2. **Version catalog is complete** — `gradle/libs.versions.toml` contains all verified dependency versions:
   - Kotlin 2.3.21
   - AGP 9.2.1
   - Compose BOM 2026.04.01
   - Room 2.8.4
   - Hilt 2.59.2
   - WorkManager 2.11.2
   - DataStore Preferences 1.2.1
   - Kotlin Serialization 1.11.0
   - Play Services Location 21.3.0
   - JUnit 5, Turbine, Espresso, Compose UI Testing
   - Timber

3. **Convention plugins work** — `build-logic/convention/` contains plugins that apply shared config (minSdk 29, compileSdk latest, JVM 17, Compose compiler, KSP):
   - `AndroidApplicationConventionPlugin`
   - `AndroidLibraryConventionPlugin`
   - `AndroidComposeConventionPlugin`
   - `AndroidHiltConventionPlugin`
   - `AndroidRoomConventionPlugin`

4. **Module dependency graph is correct** — `:app-headunit` depends on `:core`, `:app-phone` depends on `:core`, `:core` has no project dependencies. App modules do NOT depend on each other.

5. **Head unit manifest is correct** — `app-headunit/src/main/AndroidManifest.xml` declares zero network permissions (no `INTERNET`, no `ACCESS_NETWORK_STATE`) and sets `android:screenOrientation="landscape"`.

6. **Build succeeds** — `./gradlew assembleDebug` completes with zero errors for all three modules.

## Tasks / Subtasks

- [ ] Task 1: Root project setup (AC: #1, #6)
  - [ ] Create `settings.gradle.kts` with `pluginManagement`, `dependencyResolutionManagement`, and `include` for all 4 modules
  - [ ] Create root `build.gradle.kts` with `apply false` for all plugins (AGP, Kotlin, Hilt, KSP, Compose compiler, Room, Serialization)
  - [ ] Create `gradle.properties` with `org.gradle.jvmargs=-Xmx2048m`, `android.useAndroidX=true`, `android.nonTransitiveRClass=true`
  - [ ] Create `.gitignore` (standard Android template)

- [ ] Task 2: Version catalog (AC: #2)
  - [ ] Create `gradle/libs.versions.toml` with `[versions]`, `[libraries]`, `[plugins]` sections
  - [ ] All dependency versions match the verified versions listed in AC #2

- [ ] Task 3: Convention plugins — build-logic (AC: #3)
  - [ ] Create `build-logic/settings.gradle.kts` with `dependencyResolutionManagement` pointing to root version catalog
  - [ ] Create `build-logic/convention/build.gradle.kts` as `kotlin-dsl` project with `gradlePlugin` block registering all 5 plugins
  - [ ] Implement `AndroidApplicationConventionPlugin.kt` — sets `minSdk`, `compileSdk`, `targetSdk`, JVM target 17, enables `BuildConfig`
  - [ ] Implement `AndroidLibraryConventionPlugin.kt` — same config but for library modules, enables `consumerProguardFiles`
  - [ ] Implement `AndroidComposeConventionPlugin.kt` — enables Compose via Kotlin compiler plugin (`compose = true` in `kotlinOptions`), adds Compose BOM + dependencies
  - [ ] Implement `AndroidHiltConventionPlugin.kt` — applies KSP + Hilt plugins, adds Hilt dependencies
  - [ ] Implement `AndroidRoomConventionPlugin.kt` — applies KSP, adds Room dependencies, configures schema export directory

- [ ] Task 4: Core module (AC: #4)
  - [ ] Create `core/build.gradle.kts` applying `AndroidLibraryConventionPlugin`, `AndroidComposeConventionPlugin`, `AndroidHiltConventionPlugin`, `AndroidRoomConventionPlugin`
  - [ ] Create package structure: `com.roadmate.core` with empty marker packages (`database/`, `model/`, `repository/`, `sync/`, `location/`, `obd/`, `state/`, `ui/components/`, `ui/theme/`, `util/`)
  - [ ] Create placeholder `AndroidManifest.xml` (library manifest — no activities)

- [ ] Task 5: Head unit app module (AC: #4, #5)
  - [ ] Create `app-headunit/build.gradle.kts` applying `AndroidApplicationConventionPlugin`, `AndroidComposeConventionPlugin`, `AndroidHiltConventionPlugin`; add `implementation(project(":core"))`
  - [ ] Set `applicationId = "com.roadmate.headunit"`, `namespace = "com.roadmate.headunit"`
  - [ ] Create `AndroidManifest.xml`: `screenOrientation="landscape"`, NO internet permissions, empty `<application>` with `@HiltAndroidApp` placeholder
  - [ ] Create `RoadMateApplication.kt` with `@HiltAndroidApp`
  - [ ] Create `MainActivity.kt` with `@AndroidEntryPoint` and empty `setContent { }` with a placeholder text ("RoadMate Head Unit")
  - [ ] Create package structure: `com.roadmate.headunit` with empty marker packages (`di/`, `service/`, `ui/driving/`, `ui/parked/`, `ui/theme/`, `receiver/`)

- [ ] Task 6: Phone app module (AC: #4)
  - [ ] Create `app-phone/build.gradle.kts` applying `AndroidApplicationConventionPlugin`, `AndroidComposeConventionPlugin`, `AndroidHiltConventionPlugin`; add `implementation(project(":core"))`
  - [ ] Set `applicationId = "com.roadmate.phone"`, `namespace = "com.roadmate.phone"`
  - [ ] Create `AndroidManifest.xml` with empty `<application>`
  - [ ] Create `RoadMatePhoneApplication.kt` with `@HiltAndroidApp`
  - [ ] Create `MainActivity.kt` with `@AndroidEntryPoint` and empty `setContent { }` with placeholder text ("RoadMate Phone")
  - [ ] Create package structure: `com.roadmate.phone` with empty marker packages (`di/`, `navigation/`, `ui/hub/`, `ui/maintenance/`, `ui/trips/`, `ui/fuel/`, `ui/documents/`, `ui/settings/`, `ui/components/`, `ui/theme/`, `worker/`)

- [ ] Task 7: Build verification (AC: #6)
  - [ ] Run `./gradlew assembleDebug` — all 3 modules compile with zero errors
  - [ ] Verify Gradle dependency graph: `:core` has no `:app-*` dependencies

## Dev Notes

### Architecture Compliance

**Build System Pattern (Now In Android convention plugin pattern):**
- Convention plugins live in `build-logic/convention/` as an **included build** (not a regular module)
- `settings.gradle.kts` at root includes `build-logic` via `includeBuild("build-logic")`
- Each convention plugin extends `Plugin<Project>` and configures via `extensions.configure<*Extension>`
- Plugin IDs are registered in `build-logic/convention/build.gradle.kts` using `gradlePlugin { plugins { } }` block

**Critical Version Compatibility:**
- Kotlin 2.3.21 + Compose compiler: Since Kotlin 2.0+, Compose compiler is integrated into the Kotlin Gradle plugin. Do NOT add a separate `org.jetbrains.kotlin:kotlin-compose-compiler-plugin` dependency. Enable via `composeOptions` or `compose = true` in `kotlinOptions`.
- KSP version must match Kotlin version: use `com.google.devtools.ksp` plugin version compatible with Kotlin 2.3.21
- AGP 9.2.1 requires Gradle 8.13+ (verify wrapper version)

**Convention Plugin Registration:**
```kotlin
// build-logic/convention/build.gradle.kts
gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "roadmate.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        // repeat for all 5 plugins
    }
}
```

**Module build.gradle.kts pattern:**
```kotlin
// app-headunit/build.gradle.kts
plugins {
    id("roadmate.android.application")
    id("roadmate.android.compose")
    id("roadmate.android.hilt")
}
dependencies {
    implementation(project(":core"))
}
```

### CRITICAL Anti-Patterns to Avoid

| ❌ Do NOT | ✅ Do THIS |
|---|---|
| Use `kapt` for annotation processing | Use `ksp` — required for Room 2.8+ and Hilt 2.59+ |
| Add `kotlin-compose-compiler-plugin` manually | Enable via Kotlin plugin `compose = true` (Kotlin 2.0+ built-in) |
| Copy build config across module `build.gradle.kts` files | Use convention plugins for all shared config |
| Add `INTERNET` permission anywhere | Zero network permissions — this is a hard constraint |
| Make `:core` depend on app modules | Dependency flows one way: app → core |
| Use `LiveData` imports | Zero LiveData — `StateFlow` only |
| Set `compileSdk` or `minSdk` in individual modules | Set once in convention plugins |

### Project Structure Notes

**Exact directory tree this story must create:**
```
roadmate/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── .gitignore
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/gradle-wrapper.properties
├── build-logic/
│   ├── settings.gradle.kts
│   └── convention/
│       ├── build.gradle.kts
│       └── src/main/kotlin/
│           ├── AndroidApplicationConventionPlugin.kt
│           ├── AndroidLibraryConventionPlugin.kt
│           ├── AndroidComposeConventionPlugin.kt
│           ├── AndroidHiltConventionPlugin.kt
│           └── AndroidRoomConventionPlugin.kt
├── app-headunit/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── kotlin/com/roadmate/headunit/
│           ├── RoadMateApplication.kt
│           ├── MainActivity.kt
│           ├── di/
│           ├── service/
│           ├── ui/driving/
│           ├── ui/parked/
│           ├── ui/theme/
│           └── receiver/
├── app-phone/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── kotlin/com/roadmate/phone/
│           ├── RoadMatePhoneApplication.kt
│           ├── MainActivity.kt
│           ├── di/
│           ├── navigation/
│           ├── ui/hub/
│           ├── ui/maintenance/
│           ├── ui/trips/
│           ├── ui/fuel/
│           ├── ui/documents/
│           ├── ui/settings/
│           ├── ui/components/
│           ├── ui/theme/
│           └── worker/
└── core/
    ├── build.gradle.kts
    └── src/main/
        ├── AndroidManifest.xml
        └── kotlin/com/roadmate/core/
            ├── database/
            ├── model/
            ├── repository/
            ├── sync/
            ├── location/
            ├── obd/
            ├── state/
            ├── ui/components/
            ├── ui/theme/
            └── util/
```

Empty marker packages: Create a `.gitkeep` file in each empty directory to ensure they are tracked by Git. This preserves the architectural intent for future story implementations.

### References

- [Source: architecture.md#Build System Architecture] — Convention plugin structure and rationale
- [Source: architecture.md#Technology Stack — Verified Current Versions] — Exact dependency versions
- [Source: architecture.md#Project Structure & Boundaries] — Complete directory tree
- [Source: architecture.md#Module Dependency Rules] — Dependency graph constraints
- [Source: architecture.md#Architectural Boundaries] — Layer boundaries within core
- [Source: architecture.md#Implementation Patterns & Consistency Rules] — Naming conventions and enforcement rules
- [Source: epics.md#Story 1.1] — Acceptance criteria source

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
