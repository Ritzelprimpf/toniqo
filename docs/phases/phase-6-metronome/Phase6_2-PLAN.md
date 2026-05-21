# Phase 6.2 — Metronome Player, Scheduler, Persistence

## Goal

Implement the metronome's audio engine and supporting infrastructure. By the end of 6.2, the app can actually play a metronome — beats land at the correct times, accents and subdivisions sound right, tempo changes mid-run re-anchor cleanly, and the player respects audio focus and screen lifecycle. Persistence is also wired in: BPM, time signature, and subdivision survive across app launches.

This is the first sub-phase where audio is heard. It's also the heaviest sub-phase by code volume because it touches the most real Android infrastructure (`AudioTrack`, audio focus, DataStore).

The deliverable is a fully testable audio stack: `AudioTrackMetronomePlayer` driven by the anchor-based scheduler from Item 2, a pure `TapTempoCalculator`, and a DataStore-backed `MetronomePreferences` with self-healing validation. No UI yet.

## Scope

- Implement `AudioTrackMetronomePlayer` — replace the Phase 2 stub.
- Implement the anchor-based drift-corrected scheduler (Item 2).
- Audio focus request / abandon / focus-loss handling (Item 5).
- `AudioTrack` initialization with failure handling (Item 19).
- Implement `TapTempoCalculator` (Item 6).
- Define and implement `MetronomePreferences` interface + DataStore-backed impl + in-memory fake for tests.
- Implement `validateOrDefault()` config validation with self-healing write-back (Item 17).
- Inject a clock abstraction for testability of time-dependent code.
- Hilt bindings for everything new.
- Exhaustive unit tests for the scheduler logic, tap tempo, and preferences.
- Manual listening smoke test by the user via a debug harness (no UI in this phase, so a temporary verification mechanism is needed).

## Out of Scope

- No `MetronomeViewModel` work — Phase 6.3.
- No `StartMetronomeUseCase` / `StopMetronomeUseCase` real bodies — Phase 6.3.
- No UI, no Compose, no screen — Phase 6.4.
- No accent customization (Item 11 — deferred).
- No haptics (Item 10 — deferred).
- No `Robolectric` or instrumented audio tests; `AudioTrack` paths verified by user smoke test.

## Reading Order Before Starting

1. `APP_SPECIFICATION.md` → "Module: Metronome"
2. `DESIGN.md` → §8.2
3. `DECISIONS.md` → all entries
4. `Phase6-Metronome-Decisions.md` → Items 2, 4, 5, 6, 14, 15, 16, 17, 19
5. The completed `Phase6_1-PLAN.md` — `ClickSynthesizer`, `MetronomeAudioFormat`, `clicksPerBar`, `clickKindFor` are all consumed here
6. This file

## Decisions Locked In For 6.2

These are settled before implementation begins (full rationale in `Phase6-Metronome-Decisions.md`):

- ✅ **Scheduling strategy:** Anchor-based drift-corrected `delay()` loop. Target time computed from a fixed start anchor in nanoseconds: `targetNs = startTimeNs + (clickIndex * intervalNs)`, where `intervalNs = 60_000_000_000L / bpm / subdivision.multiplier`. (Item 2)
- ✅ **Clock abstraction:** `Clock` interface with `nanoTime(): Long` injected via constructor. Production impl wraps `System.nanoTime()`; test impl virtualizes time. (Item 2)
- ✅ **Visual / audio sync:** UI `currentBeat` emission precedes the audible click by `AudioTrack` output latency. Accept visual leading slightly for v1. Revisit only if it looks wrong in practice. (Item 2)
- ✅ **Tempo changes mid-run:** Re-anchor on the next beat — the next beat becomes the new anchor for the new tempo. (Item 2)
- ✅ **Signature / subdivision changes mid-run:** Restart the beat cycle from beat 1 on the next downbeat. (Item 2 + APP_SPECIFICATION.md)
- ✅ **Audio focus:** Request on start, abandon on stop. Any focus-loss event (transient or permanent) triggers `stop()`. No auto-resume. (Item 5)
- ✅ **Lifecycle binding:** The player runs only while collected. No internal lifecycle tracking — the collector's scope governs lifetime. The screen (6.4) will collect with `collectAsStateWithLifecycle`. (Item 5)
- ✅ **Persistence scope:** BPM, time signature numerator, time signature denominator, subdivision. Not `isPlaying`, not `currentBeat`. (Item 4)
- ✅ **DataStore:** `androidx.datastore:datastore-preferences` (already added in Phase 5.3). Separate file `metronome_preferences` from the tuner's. (Item 4)
- ✅ **Invalid persisted config:** Whole-config replacement with `MetronomeConfig.DEFAULT`. Repaired config is written back so subsequent reads are clean. (Item 17)
- ✅ **Defaults:** `MetronomeConfig.DEFAULT = MetronomeConfig(bpm = 120, timeSignatureNumerator = 4, timeSignatureDenominator = 4, subdivision = Subdivision.NONE)`. (Item 4)
- ✅ **Tap tempo algorithm:** Rolling window of last 5 taps (4 intervals). BPM emitted starting from tap 2. 2-second reset timeout. No outlier rejection. Result clamped to [1, 300] and rounded to integer. (Item 6)
- ✅ **Audio format:** Per `MetronomeAudioFormat` from 6.1 — 48 kHz, mono, 16-bit PCM.
- ✅ **Audio attributes:** `USAGE_MEDIA` + `CONTENT_TYPE_SONIFICATION`. (Item 12)
- ✅ **Error handling:** `AudioTrack` init failure or audio focus denial returns a failure result from `start()`; the player remains stopped; resources released. UI surfaces the error in 6.3/6.4. (Item 19)

## Implementation Details

### `common/util/Clock.kt`

A small abstraction so time-dependent code is unit-testable. Lives in `common/util/` rather than `metronome/` because the tap-tempo calculator and the scheduler both need it, and other modules may want it later.

```kotlin
/** A monotonic nanosecond clock; injected to make time-dependent code testable. */
interface Clock {
    fun nanoTime(): Long
}

internal class SystemClock @Inject constructor() : Clock {
    override fun nanoTime(): Long = System.nanoTime()
}
```

Hilt binding in `common/di/CommonModule.kt`:
```kotlin
@Binds abstract fun bindClock(impl: SystemClock): Clock
```

> If `CommonModule.kt` doesn't exist yet (it should, from Phase 5.2), create it.

### `metronome/data/MetronomeConfig.kt` (update existing)

Add a companion `DEFAULT` constant:

```kotlin
data class MetronomeConfig(
    val bpm: Int,
    val timeSignatureNumerator: Int,
    val timeSignatureDenominator: Int,
    val subdivision: Subdivision,
) {
    companion object {
        val DEFAULT = MetronomeConfig(
            bpm = 120,
            timeSignatureNumerator = 4,
            timeSignatureDenominator = 4,
            subdivision = Subdivision.NONE,
        )

        const val BPM_MIN = 1
        const val BPM_MAX = 300

        val SUPPORTED_SIGNATURES: Set<Pair<Int, Int>> = setOf(
            2 to 4, 3 to 4, 4 to 4, 5 to 4,
            6 to 8, 7 to 8, 9 to 8, 12 to 8,
        )
    }
}
```

### `metronome/domain/repository/MetronomePlayer.kt` (refine existing)

Phase 2 had `start(config)`, `stop()`, `updateConfig(config)`, `currentBeat: Flow<Int>`. Refine to better fit the flow-as-lifetime pattern from 5.2:

```kotlin
/**
 * Audio playback engine for the metronome.
 *
 * The metronome plays for as long as a collector is collecting [run]. Cancelling the collector
 * stops playback and releases all resources (audio focus, AudioTrack). There is no separate
 * start/stop API.
 */
interface MetronomePlayer {
    /**
     * Starts playback with the given [initialConfig]. Subsequent config updates flow through
     * [configFlow]. The returned flow emits one [PlayerEvent.BeatTick] per click, plus
     * lifecycle events ([PlayerEvent.Started], [PlayerEvent.Failed]).
     *
     * Cancelling the collector stops playback. The player handles audio focus internally.
     */
    fun run(initialConfig: MetronomeConfig, configFlow: Flow<MetronomeConfig>): Flow<PlayerEvent>
}
```

> The Phase 2 imperative `start`/`stop`/`updateConfig` is **superseded**. The new flow-based shape matches the tuner's `MicrophoneAudioSource` lifetime pattern from 5.2. Record in `DECISIONS.md`.

### `metronome/data/PlayerEvent.kt`

```kotlin
internal sealed interface PlayerEvent {
    /** Emitted once when playback begins. */
    data object Started : PlayerEvent

    /** Emitted on every click. [beatIndexInBar] is the **main beat** index (0-based); subdivision-only clicks do not emit. */
    data class BeatTick(val beatIndexInBar: Int) : PlayerEvent

    /** Emitted before the flow terminates if playback could not start or could not continue. */
    data class Failed(val reason: PlayerFailureReason) : PlayerEvent
}

internal enum class PlayerFailureReason {
    AUDIO_TRACK_INIT_FAILED,
    AUDIO_FOCUS_DENIED,
}
```

> The `BeatTick` event reports main beat index only — the UI cares about main beats for the indicator (per `DESIGN.md` §8.2). Subdivision clicks are heard but not surfaced to the UI. This keeps the UI emission rate bounded by signature numerator, independent of subdivision.

### `metronome/data/AudioTrackMetronomePlayer.kt`

Replace the Phase 2 `TODO()` body. Pseudocode shape:

```kotlin
internal class AudioTrackMetronomePlayer @Inject constructor(
    private val context: Context,
    private val clickSynthesizer: ClickSynthesizer,
    private val clock: Clock,
) : MetronomePlayer {

    override fun run(
        initialConfig: MetronomeConfig,
        configFlow: Flow<MetronomeConfig>,
    ): Flow<PlayerEvent> = callbackFlow {
        // 1. Pre-generate click buffers (one ShortArray per ClickKind).
        // 2. Build AudioTrack with MetronomeAudioFormat constants and SONIFICATION attributes.
        //    If state != STATE_INITIALIZED → emit Failed(AUDIO_TRACK_INIT_FAILED), close.
        // 3. Request audio focus via AudioManager. If denied → emit Failed(AUDIO_FOCUS_DENIED), close.
        //    Install an OnAudioFocusChangeListener that calls close(cause) on any focus loss.
        // 4. emit(Started).
        // 5. Launch the scheduler coroutine. It owns:
        //      var currentConfig = initialConfig
        //      var anchorNs = clock.nanoTime()
        //      var clickIndexInBar = 0
        //      Loop forever:
        //          - playClick(clickKindFor(clickIndexInBar, currentConfig.subdivision))
        //          - if main beat (index % multiplier == 0): emit BeatTick(clickIndexInBar / multiplier)
        //          - clickIndexInBar = (clickIndexInBar + 1) % clicksPerBar(...)
        //          - sleepNs = nextTargetNs - clock.nanoTime(); delay(max(0, sleepNs / 1_000_000))
        //          - If configFlow has produced a new value: re-anchor based on the rule below.
        // 6. Collect configFlow concurrently; updates write into a shared mutable holder that the
        //    scheduler reads at the top of each loop iteration.
        //
        // Re-anchor rule:
        //   - BPM change only: anchorNs = clock.nanoTime(); clickIndexInBar unchanged; the next
        //     beat happens at anchorNs + intervalNs(newBpm, newSubdivision).
        //   - Signature or subdivision change: anchorNs = clock.nanoTime(); clickIndexInBar = 0
        //     (downbeat).
        //
        // awaitClose:
        //   - Cancel scheduler.
        //   - audioTrack.stop(); audioTrack.release().
        //   - audioManager.abandonAudioFocusRequest(focusRequest).
        //   - Unregister the focus change listener.
    }
}
```

Real implementation notes:
- Use `AudioFocusRequest.Builder()` for API 26+; the minSdk is 31 so no compatibility shim needed.
- `playClick(kind)` writes the corresponding pre-generated `ShortArray` to `AudioTrack` with `WRITE_BLOCKING`. The write blocks only if the hardware buffer is full — at our 30 ms click duration vs. typical buffer sizes, this is non-blocking in practice.
- BPM and config math live in `intervalNanos(bpm: Int, subdivision: Subdivision): Long` — a pure helper that can be unit-tested independently.
- The scheduler's loop reads `currentConfig` at the start of each iteration. Updates to it (via `configFlow.collect`) set a volatile field; race conditions are tolerable because beats are 50 ms apart at worst.

### `metronome/data/TapTempoCalculator.kt`

Pure logic, no Android dependencies, no `AudioTrack`. Tests cover every behavior from Item 6.

```kotlin
/**
 * Computes BPM from tap timestamps using a rolling window of the most recent 5 taps
 * (4 intervals). Emits null until the second tap of a session. After 2 seconds of no
 * tapping, the next tap starts a new session.
 */
internal class TapTempoCalculator @Inject constructor(
    private val clock: Clock,
) {
    private val windowMs: ArrayDeque<Long> = ArrayDeque()

    /**
     * Record a tap at the current clock time. Returns the new BPM in [BPM_MIN, BPM_MAX],
     * or null if not enough taps yet (first tap of a session).
     */
    fun onTap(): Int? {
        val now = clock.nanoTime() / 1_000_000  // ns → ms
        // If the previous tap was > RESET_TIMEOUT_MS ago, start a new session.
        if (windowMs.isNotEmpty() && now - windowMs.last() > RESET_TIMEOUT_MS) {
            windowMs.clear()
        }
        windowMs.addLast(now)
        while (windowMs.size > WINDOW_SIZE) windowMs.removeFirst()
        if (windowMs.size < 2) return null
        val intervals = windowMs.zipWithNext { a, b -> (b - a).toDouble() }
        val meanIntervalMs = intervals.average()
        val bpm = (60_000.0 / meanIntervalMs).roundToInt()
        return bpm.coerceIn(MetronomeConfig.BPM_MIN, MetronomeConfig.BPM_MAX)
    }

    /** Clears the window. Useful when the user disengages from tap-tempo (e.g., navigates away). */
    fun reset() { windowMs.clear() }

    private companion object {
        const val WINDOW_SIZE = 5
        const val RESET_TIMEOUT_MS = 2_000L
    }
}
```

### `metronome/data/MetronomePreferences.kt`

Interface + DataStore-backed impl, mirroring `TunerPreferences` from Phase 5.3.

```kotlin
interface MetronomePreferences {
    val config: Flow<MetronomeConfig>
    suspend fun setConfig(config: MetronomeConfig)
}
```

```kotlin
private val Context.metronomeDataStore by preferencesDataStore(name = "metronome_preferences")

internal class MetronomePreferencesImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : MetronomePreferences {

    private val keyBpm                = intPreferencesKey("bpm")
    private val keyNumerator          = intPreferencesKey("time_sig_numerator")
    private val keyDenominator        = intPreferencesKey("time_sig_denominator")
    private val keySubdivision        = stringPreferencesKey("subdivision")

    override val config: Flow<MetronomeConfig> = context.metronomeDataStore.data
        .map { prefs ->
            val raw = RawMetronomeConfig(
                bpm = prefs[keyBpm],
                numerator = prefs[keyNumerator],
                denominator = prefs[keyDenominator],
                subdivisionName = prefs[keySubdivision],
            )
            val validated = validateOrDefault(raw)
            // Self-healing: if the persisted form differed from the validated form, write it back.
            if (raw.requiresRepair(validated)) {
                context.metronomeDataStore.edit { writeAll(it, validated) }
            }
            validated
        }

    override suspend fun setConfig(config: MetronomeConfig) {
        context.metronomeDataStore.edit { writeAll(it, config) }
    }

    private fun writeAll(prefs: MutablePreferences, config: MetronomeConfig) {
        prefs[keyBpm] = config.bpm
        prefs[keyNumerator] = config.timeSignatureNumerator
        prefs[keyDenominator] = config.timeSignatureDenominator
        prefs[keySubdivision] = config.subdivision.name
    }
}
```

`RawMetronomeConfig` is an internal data class holding the nullable raw values; `validateOrDefault(raw)` returns either a valid `MetronomeConfig` parsed from the raw fields, or `MetronomeConfig.DEFAULT`. The `requiresRepair` extension just checks whether the persisted-as-stored fields agree with the validated config (so we don't write-back churn on every read of a clean config).

```kotlin
internal fun validateOrDefault(raw: RawMetronomeConfig): MetronomeConfig {
    val bpm = raw.bpm ?: return MetronomeConfig.DEFAULT
    val num = raw.numerator ?: return MetronomeConfig.DEFAULT
    val den = raw.denominator ?: return MetronomeConfig.DEFAULT
    val subName = raw.subdivisionName ?: return MetronomeConfig.DEFAULT
    val subdivision = Subdivision.entries.firstOrNull { it.name == subName }
        ?: return MetronomeConfig.DEFAULT

    val sigOk = (num to den) in MetronomeConfig.SUPPORTED_SIGNATURES
    val bpmOk = bpm in MetronomeConfig.BPM_MIN..MetronomeConfig.BPM_MAX
    return if (sigOk && bpmOk) {
        MetronomeConfig(bpm, num, den, subdivision)
    } else MetronomeConfig.DEFAULT
}
```

### `metronome/test/FakeMetronomePreferences.kt`

In-memory fake for tests. Lives under `app/src/test/java/de/ritzelprimpf/toniqo/metronome/fakes/`.

```kotlin
internal class FakeMetronomePreferences : MetronomePreferences {
    private val _config = MutableStateFlow(MetronomeConfig.DEFAULT)
    override val config: Flow<MetronomeConfig> = _config.asStateFlow()
    override suspend fun setConfig(config: MetronomeConfig) { _config.value = config }
}
```

### `metronome/di/MetronomeModule.kt` (update existing)

Add the new bindings:
- `MetronomePlayer` → `AudioTrackMetronomePlayer` (was a Phase 2 stub binding).
- `MetronomePreferences` → `MetronomePreferencesImpl`.
- `TapTempoCalculator` is constructor-injected (no binding needed beyond `@Inject constructor`).
- `ClickSynthesizer` is constructor-injected (no binding needed).

`Clock` is bound in `CommonModule.kt`.

### Debug harness for smoke testing (temporary)

Since 6.2 has no UI but produces real audio, the user needs a way to verify "does it actually sound like a metronome?" Two acceptable approaches:

**Option A: Temporary "Play" button in the existing Phase 4 metronome placeholder screen.** Wire a single button that calls into `AudioTrackMetronomePlayer.run(MetronomeConfig.DEFAULT, flowOf(MetronomeConfig.DEFAULT)).collect { /* log events */ }` from a coroutine launched in the screen. Remove or replace this in 6.4.

**Option B: An instrumented manual test that the user runs from Android Studio.** Slightly heavier; verifies the same thing.

**Recommendation: Option A.** Cheaper, surfaces the audio behavior on a real device, and the cleanup happens naturally when 6.4 replaces the placeholder with the real screen.

The harness is **debug-only scaffolding** and is explicitly called out in the handoff as something to verify and then forget about until 6.4 erases it.

## Tests

All tests live under `app/src/test/java/de/ritzelprimpf/toniqo/metronome/`. They are exhaustive — every behavior locked in by the decision log gets a test.

### `IntervalMathTest`

Tests the pure `intervalNanos(bpm, subdivision)` helper.

- `intervalNanos(120, NONE)` ≈ 500_000_000 ns (500 ms).
- `intervalNanos(120, EIGHTHS)` == `intervalNanos(120, NONE) / 2`.
- `intervalNanos(120, TRIPLETS)` ≈ 166_666_666 ns.
- Edge BPM values 1 and 300 return sane positive numbers.
- Returned value is positive for all BPMs in [1, 300] × all subdivisions.

### `AudioTrackMetronomePlayerTest` — limited

Only the **pure scheduling logic** is unit-testable without a real `AudioTrack`. Extract the scheduler's tick-generation and re-anchor logic into a testable helper if needed.

- **Drift-free over many beats:** With BPM=120 and a virtualized clock, simulate 1000 beats. The total elapsed virtual time is within 1 ms of `1000 * 500 ms`. (Verifies anchor-based math; integer-division drift is impossible.)
- **Tempo change re-anchors:** Start at BPM=120, after 10 beats inject BPM=240. The next beat occurs at `previousTargetNs + intervalNanos(240, ...)`, not at the BPM=120 interval.
- **Signature change resets to downbeat:** Mid-bar, inject a new signature. The next emitted `BeatTick.beatIndexInBar == 0`.
- **Subdivision change resets to downbeat.** Same shape.
- **BeatTick frequency:** With subdivision=EIGHTHS, only every second click produces a `BeatTick`. Verify by counting events over a simulated bar.

`AudioTrack` itself (write paths, focus listener wiring) is verified by the user's manual smoke test, not by unit tests.

### `TapTempoCalculatorTest`

- **First tap returns null.** A single `onTap()` call returns null.
- **Second tap returns a BPM.** Two `onTap()` calls 500 ms apart return ~120 BPM.
- **Three taps update with rolling average.** Taps at 500, 500, 500 ms intervals → ~120 BPM throughout.
- **Window slides.** Tap at 600 ms intervals (100 BPM) five times, then start tapping at 400 ms intervals (150 BPM). After 5 taps at 400 ms, the BPM has converged on ~150.
- **Reset timeout.** Two taps at 500 ms, wait 3 seconds (more than `RESET_TIMEOUT_MS`), tap again — that third tap returns null (treated as the first of a new session).
- **Clamping high.** Two taps at 100 ms apart (would be 600 BPM) → clamped to 300.
- **Clamping low.** Two taps very far apart (within the reset timeout) → still clamped to 1.
- **Rounding.** Two taps at 433 ms apart → ~138.57 BPM → returns 139.

Tests inject a fake `Clock` that returns deterministic ns values.

### `ValidateOrDefaultTest`

- All fields valid + supported signature → returns parsed config.
- Missing any field → returns `DEFAULT`.
- BPM out of [1, 300] → returns `DEFAULT`.
- Unsupported signature (e.g., 5/8) → returns `DEFAULT`.
- Unrecognized subdivision name → returns `DEFAULT`.
- Default config (BPM=120, 4/4, NONE) round-trips: `validateOrDefault(toRaw(DEFAULT)) == DEFAULT`.

### `FakeMetronomePreferencesTest`

Trivial — verifies the in-memory fake behaves as a `StateFlow`: initial value is `DEFAULT`, `setConfig` updates the flow.

### `MetronomePreferencesImplTest`

DataStore is hard to test on the JVM. Same approach as `TunerPreferencesImpl` in Phase 5.3: **skipped at the unit-test level**, verified by the manual smoke test in this phase (set BPM, close app, relaunch, observe persisted value) and by the ViewModel integration in 6.3 using the fake.

## Steps

1. Create `common/util/Clock.kt` (interface + `SystemClock` impl). Bind in `common/di/CommonModule.kt`.
2. Update `metronome/data/MetronomeConfig.kt` to add `DEFAULT`, `BPM_MIN`, `BPM_MAX`, `SUPPORTED_SIGNATURES`.
3. Refine `metronome/domain/repository/MetronomePlayer.kt` to the flow-based shape. Record in `DECISIONS.md`.
4. Create `metronome/data/PlayerEvent.kt` (sealed interface + `PlayerFailureReason` enum).
5. Create `metronome/data/IntervalMath.kt` with the pure `intervalNanos` helper. Write `IntervalMathTest`.
6. Create `metronome/data/AudioTrackMetronomePlayer.kt` with the full implementation per the pseudocode above.
7. Extract the scheduler tick logic into a testable helper. Write `AudioTrackMetronomePlayerTest` for the testable parts.
8. Create `metronome/data/TapTempoCalculator.kt`. Write `TapTempoCalculatorTest`.
9. Create `metronome/data/RawMetronomeConfig.kt` and `validateOrDefault` function. Write `ValidateOrDefaultTest`.
10. Create `metronome/data/MetronomePreferences.kt` (interface) and `MetronomePreferencesImpl.kt` (impl).
11. Create `metronome/fakes/FakeMetronomePreferences.kt` in the test source set. Write `FakeMetronomePreferencesTest`.
12. Update `metronome/di/MetronomeModule.kt`: bind `MetronomePlayer → AudioTrackMetronomePlayer`, `MetronomePreferences → MetronomePreferencesImpl`.
13. Add the debug harness to the existing Phase 4 metronome placeholder screen — a single "Play" / "Stop" button wired to `AudioTrackMetronomePlayer`. Mark with a `// TODO(6.4): replace with real screen` comment.
14. Update `DECISIONS.md` with: the `MetronomePlayer` interface shape change (imperative → flow-based), anchor-based scheduling rationale, screen-lifecycle-only playback policy, `metronome_preferences` DataStore file, whole-config replacement on invalid persisted data.
15. Hand off to the user. Smoke test plan:
    - **Audio sanity:** Tap the debug Play button. Verify a metronome clicks at 120 BPM with the default config. Verify accent on beat 1. Verify the click cadence is steady.
    - **Persistence sanity:** (Will be more fully exercised in 6.3 when there's a UI to change config. For 6.2: hardcode an alternate config in the debug harness, run it, kill the app, relaunch, verify the persisted config is read back.)
    - **Audio focus sanity:** While the metronome is playing, receive a notification with a sound (or trigger another media app). Verify the metronome stops.
    - **Lifecycle sanity:** While the metronome is playing, navigate to another tab. Verify the metronome stops (the debug button is on the metronome tab; leaving cancels the collector).
16. **Listening tweaks:** If any click sounds wrong (too loud / too quiet / wrong pitch / muddy), adjust the corresponding `ClickParameters` constant and record the change in `DECISIONS.md`. This is expected and not a defect.

## Completion Criteria

See `Phase6_2-REQUIREMENTS.md`.
