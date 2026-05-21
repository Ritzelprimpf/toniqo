# Phase 6.2 — Requirements & Acceptance Criteria

Phase 6.2 produces real audio playback but no proper UI yet — only a temporary debug harness in the existing Phase 4 placeholder screen. The phase is complete when the agent checklist and user checklist both pass, including the manual audio smoke tests.

## Agent Responsibilities

### `common/util/Clock.kt`

- [ ] Interface `Clock` with `fun nanoTime(): Long`.
- [ ] Production impl `SystemClock` constructor-injected; wraps `System.nanoTime()`.
- [ ] Bound in `common/di/CommonModule.kt` (create the module if absent).

### `metronome/data/MetronomeConfig.kt`

- [ ] Companion object exposes:
  - `val DEFAULT = MetronomeConfig(bpm = 120, timeSignatureNumerator = 4, timeSignatureDenominator = 4, subdivision = Subdivision.NONE)`
  - `const val BPM_MIN = 1`
  - `const val BPM_MAX = 300`
  - `val SUPPORTED_SIGNATURES: Set<Pair<Int, Int>>` containing exactly the 8 signatures from `APP_SPECIFICATION.md`.

### `metronome/domain/repository/MetronomePlayer.kt`

- [ ] Old Phase 2 imperative API (`start(config)`, `stop()`, `updateConfig(config)`, `currentBeat: Flow<Int>`) is **removed**.
- [ ] New API: `fun run(initialConfig: MetronomeConfig, configFlow: Flow<MetronomeConfig>): Flow<PlayerEvent>`.
- [ ] KDoc explains the flow-as-lifetime contract.
- [ ] Interface change recorded in `DECISIONS.md`.

### `metronome/data/PlayerEvent.kt`

- [ ] `sealed interface PlayerEvent` with three subtypes:
  - `data object Started : PlayerEvent`
  - `data class BeatTick(val beatIndexInBar: Int) : PlayerEvent`
  - `data class Failed(val reason: PlayerFailureReason) : PlayerEvent`
- [ ] `enum class PlayerFailureReason { AUDIO_TRACK_INIT_FAILED, AUDIO_FOCUS_DENIED }`.
- [ ] `BeatTick` is emitted only on **main beats** (not on subdivision-only ticks). KDoc states this explicitly.

### `metronome/data/IntervalMath.kt`

- [ ] Pure top-level function `intervalNanos(bpm: Int, subdivision: Subdivision): Long`.
- [ ] Returns `60_000_000_000L / bpm / subdivision.multiplier`.
- [ ] No magic numbers; the literal `60_000_000_000L` is named (`NANOS_PER_MINUTE`).

### `metronome/data/AudioTrackMetronomePlayer.kt`

- [ ] Replaces the Phase 2 stub. Implements `MetronomePlayer.run(...)`.
- [ ] Constructor-injects `Context`, `ClickSynthesizer`, `Clock`.
- [ ] `AudioTrack` built with `MetronomeAudioFormat` constants and `USAGE_MEDIA` / `CONTENT_TYPE_SONIFICATION` attributes.
- [ ] On `AudioTrack` init failure: emits `PlayerEvent.Failed(AUDIO_TRACK_INIT_FAILED)` and closes the flow. No resources leak.
- [ ] Requests audio focus via `AudioFocusRequest.Builder()`. On denial: emits `PlayerEvent.Failed(AUDIO_FOCUS_DENIED)` and closes the flow.
- [ ] Installs an `OnAudioFocusChangeListener` that closes the flow on any focus loss (transient or permanent). No auto-resume.
- [ ] Scheduler uses the anchor-based math from Item 2: target times computed from `startTimeNs + clickIndex * intervalNs(...)`.
- [ ] **BPM-only change** while running: anchor reset to `clock.nanoTime()`, `clickIndexInBar` preserved.
- [ ] **Signature or subdivision change** while running: anchor reset to `clock.nanoTime()`, `clickIndexInBar = 0` (next click is the new downbeat).
- [ ] Pre-generates one `ShortArray` per `ClickKind` once at start; does not regenerate per click.
- [ ] `awaitClose` block stops and releases `AudioTrack`, abandons audio focus, unregisters listener.
- [ ] No magic numbers anywhere in the body.

### `metronome/data/TapTempoCalculator.kt`

- [ ] Constructor-injects `Clock`.
- [ ] `fun onTap(): Int?` records a tap and returns the resulting BPM (or null for the first tap of a session).
- [ ] Uses a rolling window of the last 5 taps (4 intervals).
- [ ] BPM is computed as the simple mean of the intervals, rounded to integer, clamped to `[BPM_MIN, BPM_MAX]`.
- [ ] If the gap since the previous tap exceeds 2000 ms, the window is cleared before recording the new tap; returns null.
- [ ] `fun reset()` clears the window.
- [ ] Named constants `WINDOW_SIZE = 5` and `RESET_TIMEOUT_MS = 2_000L`. No magic numbers.

### `metronome/data/RawMetronomeConfig.kt` + `validateOrDefault`

- [ ] `internal data class RawMetronomeConfig(val bpm: Int?, val numerator: Int?, val denominator: Int?, val subdivisionName: String?)`.
- [ ] `internal fun validateOrDefault(raw: RawMetronomeConfig): MetronomeConfig` returns the parsed config if every field validates; else returns `MetronomeConfig.DEFAULT`.
- [ ] Validation checks: every field non-null; BPM in `[BPM_MIN, BPM_MAX]`; `(numerator to denominator) in SUPPORTED_SIGNATURES`; `subdivisionName` matches a `Subdivision` enum value.

### `metronome/data/MetronomePreferences.kt`

- [ ] Interface `MetronomePreferences`:
  - `val config: Flow<MetronomeConfig>`
  - `suspend fun setConfig(config: MetronomeConfig)`
- [ ] `MetronomePreferencesImpl` backed by a DataStore file named `metronome_preferences` (separate from the tuner's).
- [ ] On read, invokes `validateOrDefault`. If the persisted form differs from the validated form, writes the validated form back to DataStore (self-healing).
- [ ] On first launch (missing keys), returns `MetronomeConfig.DEFAULT` from the flow without churning DataStore (no write-back unless something was actually wrong).

### `metronome/fakes/FakeMetronomePreferences.kt` (test source set)

- [ ] Lives under `app/src/test/java/de/ritzelprimpf/toniqo/metronome/fakes/`.
- [ ] Backed by `MutableStateFlow<MetronomeConfig>` initialized to `DEFAULT`.
- [ ] `setConfig` updates the state flow.

### `metronome/di/MetronomeModule.kt`

- [ ] `MetronomePlayer` is bound to `AudioTrackMetronomePlayer` (replaces the Phase 2 stub binding).
- [ ] `MetronomePreferences` is bound to `MetronomePreferencesImpl`.
- [ ] No `@Provides` for things that could be `@Inject constructor` — `ClickSynthesizer` and `TapTempoCalculator` are constructor-injected.

### Debug Harness (temporary)

- [ ] The existing Phase 4 metronome placeholder screen gains a single "Play / Stop" toggle button wired to `AudioTrackMetronomePlayer.run(...)`. The button collects from the flow in a `LaunchedEffect` and logs events.
- [ ] A `// TODO(6.4): replace with real screen` comment marks the harness.
- [ ] The harness is explicitly NOT to be polished — it exists only to enable 6.2's smoke testing.

### Tests

- [ ] `IntervalMathTest` covers the cases listed in `Phase6_2-PLAN.md` → "Tests" — known BPM/subdivision pairs match expected nanoseconds; edge BPMs return positive numbers.
- [ ] `AudioTrackMetronomePlayerTest` covers the testable scheduling behavior — drift-free over many beats with virtual time, tempo change re-anchors, signature/subdivision change resets to downbeat, `BeatTick` cadence matches subdivision.
- [ ] `TapTempoCalculatorTest` covers all behaviors from Item 6: first tap null; second tap returns BPM; rolling window slides; reset timeout starts new session; clamping at both extremes; rounding.
- [ ] `ValidateOrDefaultTest` covers all-valid round trip and every failure path (missing field, out-of-range BPM, unsupported signature, unrecognized subdivision name).
- [ ] `FakeMetronomePreferencesTest` verifies the fake behaves as a `StateFlow`.
- [ ] `MetronomePreferencesImplTest` is **not created** — DataStore unit tests require `Context` and are deferred to manual smoke testing in this phase and to the ViewModel integration in 6.3 (which uses the fake).

### Dependencies

- [ ] `androidx.datastore:datastore-preferences` is already present from Phase 5.3 — verified, not re-added.

### Documentation Updates

- [ ] `DECISIONS.md` gains entries (one per decision, dated, append-only) for:
  - **MetronomePlayer API change** — Phase 2's imperative `start`/`stop`/`updateConfig`/`currentBeat` is superseded by the flow-based `run(initialConfig, configFlow): Flow<PlayerEvent>`. Mirrors the `MicrophoneAudioSource` pattern from 5.2.
  - **Anchor-based scheduling** — beat times are computed from a fixed start anchor in nanoseconds; drift is impossible by construction. Sample-accurate scheduling deferred.
  - **Screen-lifecycle-only playback** — metronome runs only while the metronome screen is the active foreground screen; no foreground service.
  - **`metronome_preferences` DataStore file** — separate from the tuner's.
  - **Whole-config replacement on invalid persisted data** — any invalid field replaces the entire config with `DEFAULT`.
  - **Tap tempo algorithm parameters** — rolling 5-tap window, 4-interval mean, 2-second reset timeout, no outlier rejection.
  - **Audio attributes** — `USAGE_MEDIA` + `CONTENT_TYPE_SONIFICATION` (correct for non-musical click sounds; respects system media volume).

### Code Quality

- [ ] No `TODO("Not yet implemented")` remains in any 6.2-touched file (the debug-harness `TODO(6.4)` is allowed and distinct).
- [ ] All public types and methods have KDoc comments.
- [ ] No magic numbers anywhere in the new code — every numeric value resolves to a named constant.
- [ ] All new internal types declared `internal` where appropriate.
- [ ] `AudioTrack` resources are released in `awaitClose`. Verified by code review.

### Handoff

- [ ] Summary message to the user lists files added, modified, and removed.
- [ ] Summary explicitly calls out: "the metronome placeholder now has a debug Play button — this is 6.2 scaffolding to enable smoke testing and will be replaced in 6.4."
- [ ] Summary lists the smoke tests the user is expected to perform (see "User Responsibilities" below).
- [ ] Summary notes that if any click sounds wrong, the relevant `ClickParameters` constant should be adjusted and the change recorded in `DECISIONS.md` — this is expected, not a defect.

## User Responsibilities (Verification in Android Studio + on a Real Device)

- [ ] **Gradle sync** completes without errors after pulling the changes.
- [ ] **Build → Make Project** completes successfully.
- [ ] **Run All Tests** reports all tests green.
- [ ] App launches on an Android 12+ device or emulator. The metronome placeholder screen now shows a Play button.

### Smoke tests (real device strongly preferred for audio quality assessment)

- [ ] **Audio plays:** Tap Play. A metronome ticks at 120 BPM with the default config (4/4, no subdivision). Verify clicks are clean — no clicks-on-clicks, no obvious distortion, no clipping.
- [ ] **Beat 1 is accented.** Listen for a higher / louder click every fourth beat. (Adjust `ClickParameters` constants if any kind sounds wrong.)
- [ ] **Cadence is steady.** Over a 30-second listen, the click cadence does not noticeably drift, speed up, or slow down.
- [ ] **Stop works.** Tap Stop (or whatever the toggle does). Clicks stop within ~1 beat.
- [ ] **Audio focus loss stops playback:** Start the metronome. From another app, play a music sample. The metronome stops. (System notification sounds may or may not stop it depending on transient-vs-permanent focus loss; behavior must at minimum stop on a clear "another media app started playing.")
- [ ] **Lifecycle stop:** Start the metronome. Navigate to a different bottom-nav tab. The metronome stops (because the collector is cancelled when the screen leaves `STARTED`).
- [ ] **No leaks:** Repeatedly play/stop ~20 times. No `AudioTrack` errors in Logcat. No memory growth alarming in the profiler (rough visual check, not an exact assertion).
- [ ] **Persistence sanity (if exposed by harness):** If the debug harness supports changing config (e.g., a hardcoded "play 90 BPM in 3/4 with EIGHTHS" alternate path), exercise it, kill the app, relaunch — `MetronomeConfig.DEFAULT` is what loads (until 6.3 wires the ViewModel to read preferences and feed them in).
- [ ] **No exceptions in Logcat** during any of the above.

### Listening tweaks

- [ ] If any click sounds off, ask the agent to adjust the relevant `ClickParameters` constant and update `DECISIONS.md`. This is expected; the v1 values are starting points.

## Decision Log

- [ ] All decisions listed under "Documentation Updates" are recorded in `DECISIONS.md` before the phase is marked complete.
- [ ] Any `ClickParameters` adjustments made during smoke testing are recorded in `DECISIONS.md`.
