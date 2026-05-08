# Story 1.1: Project Scaffold & Build System

Status: done

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

- [x] Task 1: Root project setup (AC: #1, #6)
  - [x] Create `settings.gradle.kts` with `pluginManagement`, `dependencyResolutionManagement`, and `include` for all 4 modules
  - [x] Create root `build.gradle.kts` with `apply false` for all plugins (AGP, Kotlin, Hilt, KSP, Compose compiler, Room, Serialization)
  - [x] Create `gradle.properties` with `org.gradle.jvmargs=-Xmx2048m`, `android.useAndroidX=true`, `android.nonTransitiveRClass=true`
  - [x] Create `.gitignore` (standard Android template)

- [x] Task 2: Version catalog (AC: #2)
  - [x] Create `gradle/libs.versions.toml` with `[versions]`, `[libraries]`, `[plugins]` sections
  - [x] All dependency versions match the verified versions listed in AC #2

- [x] Task 3: Convention plugins — build-logic (AC: #3)
  - [x] Create `build-logic/settings.gradle.kts` with `dependencyResolutionManagement` pointing to root version catalog
  - [x] Create `build-logic/convention/build.gradle.kts` as `kotlin-dsl` project with `gradlePlugin` block registering all 5 plugins
  - [x] Implement `AndroidApplicationConventionPlugin.kt` — sets `minSdk`, `compileSdk`, `targetSdk`, JVM target 17, enables `BuildConfig`
  - [x] Implement `AndroidLibraryConventionPlugin.kt` — same config but for library modules, enables `consumerProguardFiles`
  - [x] Implement `AndroidComposeConventionPlugin.kt` — enables Compose via Kotlin compiler plugin (`compose = true` in `kotlinOptions`), adds Compose BOM + dependencies
  - [x] Implement `AndroidHiltConventionPlugin.kt` — applies KSP + Hilt plugins, adds Hilt dependencies
  - [x] Implement `AndroidRoomConventionPlugin.kt` — applies KSP, adds Room dependencies, configures schema export directory

- [x] Task 4: Core module (AC: #4)
  - [x] Create `core/build.gradle.kts` applying `AndroidLibraryConventionPlugin`, `AndroidComposeConventionPlugin`, `AndroidHiltConventionPlugin`, `AndroidRoomConventionPlugin`
  - [x] Create package structure: `com.roadmate.core` with empty marker packages (`database/`, `model/`, `repository/`, `sync/`, `location/`, `obd/`, `state/`, `ui/components/`, `ui/theme/`, `util/`)
  - [x] Create placeholder `AndroidManifest.xml` (library manifest — no activities)

- [x] Task 5: Head unit app module (AC: #4, #5)
  - [x] Create `app-headunit/build.gradle.kts` applying `AndroidApplicationConventionPlugin`, `AndroidComposeConventionPlugin`, `AndroidHiltConventionPlugin`; add `implementation(project(":core"))`
  - [x] Set `applicationId = "com.roadmate.headunit"`, `namespace = "com.roadmate.headunit"`
  - [x] Create `AndroidManifest.xml`: `screenOrientation="landscape"`, NO internet permissions, empty `<application>` with `@HiltAndroidApp` placeholder
  - [x] Create `RoadMateApplication.kt` with `@HiltAndroidApp`
  - [x] Create `MainActivity.kt` with `@AndroidEntryPoint` and empty `setContent { }` with a placeholder text ("RoadMate Head Unit")
  - [x] Create package structure: `com.roadmate.headunit` with empty marker packages (`di/`, `service/`, `ui/driving/`, `ui/parked/`, `ui/theme/`, `receiver/`)

- [x] Task 6: Phone app module (AC: #4)
  - [x] Create `app-phone/build.gradle.kts` applying `AndroidApplicationConventionPlugin`, `AndroidComposeConventionPlugin`, `AndroidHiltConventionPlugin`; add `implementation(project(":core"))`
  - [x] Set `applicationId = "com.roadmate.phone"`, `namespace = "com.roadmate.phone"`
  - [x] Create `AndroidManifest.xml` with empty `<application>`
  - [x] Create `RoadMatePhoneApplication.kt` with `@HiltAndroidApp`
  - [x] Create `MainActivity.kt` with `@AndroidEntryPoint` and empty `setContent { }` with placeholder text ("RoadMate Phone")
  - [x] Create package structure: `com.roadmate.phone` with empty marker packages (`di/`, `navigation/`, `ui/hub/`, `ui/maintenance/`, `ui/trips/`, `ui/fuel/`, `ui/documents/`, `ui/settings/`, `ui/components/`, `ui/theme/`, `worker/`)

- [x] Task 7: Build verification (AC: #6)
  - [x] Run `./gradlew assembleDebug` — all 3 modules compile with zero errors
  - [x] Verify Gradle dependency graph: `:core` has no `:app-*` dependencies

### Review Findings

- [x] [Review][Defer] `play-services-location` in `:core` leaks to head unit — deferred, dependency is unused placeholder. Correct split (interfaces in core, Play Services in app-phone, LocationManager in app-headunit) will emerge during Story 2-1 implementation. [core/build.gradle.kts:37]
- [x] [Review][Patch] Dead `kotlin-android` plugin in version catalog — ✅ Fixed. Removed. [gradle/libs.versions.toml]
- [x] [Review][Patch] Dead `androidJunit5` version entry — ✅ Fixed. Removed. [gradle/libs.versions.toml]
- [x] [Review][Patch] No `useJUnitPlatform()` for JUnit 5 — ✅ Fixed. Added to both convention plugins. [AndroidApplicationConventionPlugin.kt, AndroidLibraryConventionPlugin.kt]
- [x] [Review][Patch] `libs` extension should be in shared `ProjectExtensions.kt` — ✅ Fixed. Extracted to ProjectExtensions.kt. [build-logic/convention/src/main/kotlin/ProjectExtensions.kt]
- [x] [Review][Patch] `android:allowBackup="true"` on head unit — ✅ Fixed. Set to `false`. [app-headunit/AndroidManifest.xml]
- [x] [Review][Defer] No `enableEdgeToEdge()` in MainActivities — deferred, pre-existing scaffold placeholder. Address in Story 1-4 (Design System & Theme).
- [x] [Review][Defer] `configChanges` override on head unit — deferred, architectural intent for landscape-locked device. Revisit during UI implementation.
- [x] [Review][Defer] Inconsistent plugin pattern (`alias()` vs `id()`) for serialization — deferred, single-use plugin doesn't warrant convention wrapper.

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

Claude Opus 4.6 (Thinking)

### Debug Log References

- AGP 9.0+ Breaking Change: `org.jetbrains.kotlin.android` plugin is no longer required — AGP 9.0+ has built-in Kotlin support. Convention plugins were updated to NOT apply this plugin.
- AGP 9.0+ Breaking Change: `CommonExtension` no longer accepts generic type parameters. `AndroidComposeConventionPlugin` was refactored to use `ApplicationExtension` and `LibraryExtension` directly with `pluginManager.withPlugin()`.
- AGP 9.0+ Deprecation: `com.android.build.gradle.LibraryExtension` is deprecated in favor of `com.android.build.api.dsl.LibraryExtension`. Convention plugins updated to use the new DSL.
- KSP version 2.3.7 is the compatible version for Kotlin 2.3.21.
- Gradle 9.4.1 is the minimum required version for AGP 9.2.1.
- compileSdk/targetSdk set to 36 (Android 16, latest stable as of May 2026).

### Completion Notes List

- ✅ All 7 tasks and all subtasks completed successfully
- ✅ `./gradlew assembleDebug` passed — BUILD SUCCESSFUL, 118 tasks, zero errors
- ✅ Version catalog contains all specified dependencies with verified versions
- ✅ 5 convention plugins implemented and functional
- ✅ Module dependency graph verified: `:core` has zero project dependencies
- ✅ Head unit manifest: zero network permissions, landscape-only orientation
- ✅ All marker packages created with `.gitkeep` files
- ✅ Both app modules have @HiltAndroidApp Application classes and @AndroidEntryPoint MainActivity with placeholder Compose UI
- ⚠️ Note: `kotlin.android` plugin removed from root build.gradle.kts — AGP 9.0+ handles Kotlin internally

### File List

- settings.gradle.kts (new)
- build.gradle.kts (new)
- gradle.properties (new)
- gradle/libs.versions.toml (new)
- gradle/wrapper/gradle-wrapper.properties (new)
- gradle/wrapper/gradle-wrapper.jar (new)
- gradlew (new)
- gradlew.bat (new)
- build-logic/settings.gradle.kts (new)
- build-logic/convention/build.gradle.kts (new)
- build-logic/convention/src/main/kotlin/AndroidApplicationConventionPlugin.kt (new)
- build-logic/convention/src/main/kotlin/AndroidLibraryConventionPlugin.kt (new)
- build-logic/convention/src/main/kotlin/AndroidComposeConventionPlugin.kt (new)
- build-logic/convention/src/main/kotlin/AndroidHiltConventionPlugin.kt (new)
- build-logic/convention/src/main/kotlin/AndroidRoomConventionPlugin.kt (new)
- core/build.gradle.kts (new)
- core/consumer-rules.pro (new)
- core/src/main/AndroidManifest.xml (new)
- core/src/main/kotlin/com/roadmate/core/{database,model,repository,sync,location,obd,state,ui/components,ui/theme,util}/.gitkeep (new)
- app-headunit/build.gradle.kts (new)
- app-headunit/proguard-rules.pro (new)
- app-headunit/src/main/AndroidManifest.xml (new)
- app-headunit/src/main/res/values/strings.xml (new)
- app-headunit/src/main/res/values/themes.xml (new)
- app-headunit/src/main/kotlin/com/roadmate/headunit/RoadMateApplication.kt (new)
- app-headunit/src/main/kotlin/com/roadmate/headunit/MainActivity.kt (new)
- app-headunit/src/main/kotlin/com/roadmate/headunit/{di,service,ui/driving,ui/parked,ui/theme,receiver}/.gitkeep (new)
- app-phone/build.gradle.kts (new)
- app-phone/proguard-rules.pro (new)
- app-phone/src/main/AndroidManifest.xml (new)
- app-phone/src/main/res/values/strings.xml (new)
- app-phone/src/main/res/values/themes.xml (new)
- app-phone/src/main/kotlin/com/roadmate/phone/RoadMatePhoneApplication.kt (new)
- app-phone/src/main/kotlin/com/roadmate/phone/MainActivity.kt (new)
- app-phone/src/main/kotlin/com/roadmate/phone/{di,navigation,ui/hub,ui/maintenance,ui/trips,ui/fuel,ui/documents,ui/settings,ui/components,ui/theme,worker}/.gitkeep (new)

### Change Log

- 2026-05-09: Story 1.1 implemented — Complete multi-module Gradle project scaffold with convention plugins, version catalog, and three modules (app-headunit, app-phone, core). BUILD SUCCESSFUL with zero errors.
