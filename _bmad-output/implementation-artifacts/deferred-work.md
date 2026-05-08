# Deferred Work

## Deferred from: code review of story-1-1 (2026-05-08)

- **No `enableEdgeToEdge()` in MainActivities** — With `targetSdk = 36`, Android enforces edge-to-edge. Placeholder activities don't call `enableEdgeToEdge()`. Address in Story 1-4 (Design System & Theme).
- **`configChanges` override on head unit MainActivity** — `android:configChanges="orientation|screenSize|screenLayout|keyboardHidden"` prevents activity recreation. Unnecessary for landscape-locked device and can mask lifecycle bugs. Revisit during UI implementation.
- **Inconsistent plugin application pattern for serialization** — `kotlin.serialization` applied via `alias()` while all other plugins use convention `id()`. Minor inconsistency; a convention plugin could be created later but isn't warranted for a single-use plugin.
- **`play-services-location` in `:core` leaks to head unit** — Dependency is unused scaffold placeholder. When Story 2-1 implements location, split into: interfaces in `:core`, Play Services impl in `:app-phone`, `LocationManager` fallback in `:app-headunit`.
