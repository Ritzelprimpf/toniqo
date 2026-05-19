# Phase 5.2 — Pitch Detection

## Goal

Implement the audio-capture and pitch-detection layer for the Guitar Tuner. By the end of 5.2, the app can listen to the device microphone and produce a stream of detected fundamental frequencies. No UI, no ViewModel state machine, no comparison to target notes — those land in 5.3 and 5.4.

The deliverable is two cleanly separated components: a `MicrophoneAudioSource` that emits raw audio buffers as a Flow, and a `YinPitchDetector` that turns one buffer into one frequency (or `null`). The two communicate through a plain `FloatArray` type — neither knows about the other.

## Scope

- Implement `YinPitchDetector` (the YIN algorithm, pure Kotlin) and replace the Phase 2 stub.
- Define a `YinConfig` data class for tunable algorithm parameters.
- Define a `MicrophoneAudioSource` interface and a `MicrophoneAudioSourceImpl` that wraps `AudioRecord` with `callbackFlow`.
- Define `CaptureEvent` as the sealed result type emitted by `MicrophoneAudioSource`.
- Define `AudioPermissionChecker` in `common/permission/` and its Android implementation.
- Hilt bindings for everything new.
- Exhaustive unit tests for the YIN algorithm using synthetic audio.

## Out of Scope

- No `TunerViewModel` work, no `DetectTunedStringUseCase` implementation — Phase 5.3.
- No UI, no permission-request flow, no permission-denied screen — Phase 5.4.
- No comparison of detected pitch to target notes — Phase 5.3 (consumes `MusicTheory.centsBetween()` from 5.1).
- No instrumented tests, no Robolectric. `MicrophoneAudioSource` is verified by the user on a real device.

## Reading Order Before Starting

1. `APP_SPECIFICATION.md` → "Module: Guitar Tuner" → "Permissions" and "Technical Notes"
2. `IMPLEMENTATION_NOTES.md` → "Audio"
3. `DECISIONS.md` → all entries (especially the new ones from 5.1 and the audio-source choice locked here)
4. `Phase5-PLAN.md` → "Decisions Already Resolved" and the 5.2 open questions
5. This file

## Decisions Locked In For 5.2

These are settled before implementation begins:

- ✅ **Audio capture parameters:** **44100 Hz sample rate, mono, PCM 16-bit, buffer size 4096 frames**. `AudioRecord.getMinBufferSize()` is consulted; the actual buffer used is `max(getMinBufferSize, 4096 frames)`.
- ✅ **Audio source:** **`UNPROCESSED` preferred, `MIC` fallback.** Try `UNPROCESSED` first; if `AudioRecord.Builder.setAudioSource(UNPROCESSED)` produces a recorder in `STATE_UNINITIALIZED`, fall back to `MIC`. Record which source was actually used at info-level for debugging (no PII).
- ✅ **YIN threshold:** **0.15** (the original paper's default).
- ✅ **YIN location:** `common/util/YinPitchDetector.kt` — pure Kotlin, reusable, no Android dependencies.
- ✅ **AudioCapture integration:** **`MicrophoneAudioSource` exposes `Flow<CaptureEvent>`; the detector is composed downstream by the use case in 5.3.** Single Responsibility, fully unit-testable.
- ✅ **Coroutine structure:** **`callbackFlow`** wrapping the `AudioRecord` lifecycle, with `awaitClose { ... }` releasing the recorder. Lifetime tied to the collector's scope — no `start()`/`stop()` methods, no mutable state in the source.
- ✅ **Permission checking:** Behind an `AudioPermissionChecker` interface in `common/permission/`. Hilt-bound to an Android implementation that calls `ContextCompat.checkSelfPermission`.
- ✅ **CaptureEvent shape:** A sealed interface emitted via the flow. First emission is always `PermissionDenied`, `Listening`, or `Failed`. While listening, subsequent emissions are `Samples(buffer)`.
- ✅ **MicrophoneAudioSource testing:** Pure unit tests only for the surrounding logic where possible. Real-device verification by the user replaces any test that would require Robolectric or instrumented tests.

## Implementation Details

### `common/permission/AudioPermissionChecker.kt`

```kotlin
/** Returns whether the calling app currently holds RECORD_AUDIO permission. */
interface AudioPermissionChecker {
    fun hasRecordAudioPermission(): Boolean
}
```

### `common/permission/AndroidAudioPermissionChecker.kt`

Concrete implementation using `ContextCompat.checkSelfPermission` with the application context. Annotated for Hilt constructor injection. Bound to the interface in `common/di/CommonModule.kt` (create the module if it doesn't yet exist).

### `common/util/YinConfig.kt`

```kotlin
/**
 * Tunable parameters for the YIN pitch-detection algorithm.
 *
 * @property threshold YIN's cumulative-mean-normalized-difference threshold.
 *   Lower values are more permissive (will report a pitch more readily) but
 *   may produce spurious results on noise. The original YIN paper recommends 0.15.
 * @property absoluteMinFrequencyHz Frequencies below this are rejected even if
 *   the algorithm reports a candidate. Protects against subharmonic confusion
 *   on the very low end. Default 30.0 Hz (below the lowest 8-string drop tuning).
 * @property absoluteMaxFrequencyHz Frequencies above this are rejected. Default
 *   2000.0 Hz (well above any guitar fundamental; harmonics aren't fundamentals).
 */
data class YinConfig(
    val threshold: Double = 0.15,
    val absoluteMinFrequencyHz: Double = 30.0,
    val absoluteMaxFrequencyHz: Double = 2000.0,
)
```

### `common/util/YinPitchDetector.kt`

Implements the `PitchDetector` interface declared in `tuner/domain/repository/PitchDetector.kt` from Phase 2.

> Phase 2 placed the `PitchDetector` interface inside `tuner/`. Now that the implementation moves to `common/`, the interface should also move to `common/util/`. This is a tiny refactor (rename one import path). Record the move in `DECISIONS.md`.

The class signature:

```kotlin
class YinPitchDetector @Inject constructor(
    private val config: YinConfig,
) : PitchDetector {
    override fun detectPitch(audioBuffer: FloatArray, sampleRateHz: Int): Double?
}
```

The algorithm follows the standard YIN reference, with four steps inside `detectPitch`:

1. **Difference function** — for each lag τ in `[0, bufferSize/2)`, compute `d(τ) = Σ (x[i] - x[i+τ])²` for `i in [0, bufferSize/2)`.
2. **Cumulative mean normalized difference** — transform `d(τ)` into `d'(τ)` where `d'(0) = 1` and `d'(τ) = d(τ) / ((1/τ) × Σ_{j=1..τ} d(j))`.
3. **Absolute threshold** — find the first τ where `d'(τ) < config.threshold` and is a local minimum. If no τ satisfies this, return `null`.
4. **Parabolic interpolation** — refine the chosen τ by parabolic interpolation of the three values `d'(τ-1)`, `d'(τ)`, `d'(τ+1)` to get a sub-sample-accurate τ.

The detected frequency is `sampleRateHz / refinedTau`. Reject (return `null`) if outside `[config.absoluteMinFrequencyHz, config.absoluteMaxFrequencyHz]` or if `refinedTau <= 0`.

Implementation notes:
- All math in `Double`. The `audioBuffer: FloatArray` parameter is converted internally.
- The algorithm allocates two internal buffers of size `bufferSize / 2`. These can be allocated per call in 5.2 (correctness first); a future optimization phase can pool them if profiling shows GC pressure.
- KDoc the method body with the four-step structure and a citation to "de Cheveigné & Kawahara, 2002 — YIN, a fundamental frequency estimator for speech and music."

### `tuner/data/MicrophoneAudioSource.kt` (interface)

```kotlin
/**
 * Streams microphone audio as a flow of capture events.
 *
 * The flow's first emission is one of [CaptureEvent.PermissionDenied],
 * [CaptureEvent.Listening], or [CaptureEvent.Failed]. While listening,
 * subsequent emissions are [CaptureEvent.Samples] until the collector
 * cancels or an error occurs.
 *
 * The AudioRecord lifecycle is bound to the collector's coroutine scope:
 * cancelling the collection releases all audio resources.
 */
interface MicrophoneAudioSource {
    fun samples(): Flow<CaptureEvent>
}
```

> The Phase 2 stubs created a `PitchDetector` interface but did not create any AudioRecord wrapper. `MicrophoneAudioSource` is **net-new in 5.2**. Record the addition in `DECISIONS.md`.

### `tuner/data/CaptureEvent.kt`

```kotlin
sealed interface CaptureEvent {
    /** Permission to record audio is not granted. Terminal — the flow completes after emitting this. */
    object PermissionDenied : CaptureEvent

    /** Capture has started successfully. Subsequent emissions will be [Samples]. */
    data class Listening(val sampleRateHz: Int, val bufferFrames: Int, val source: AudioSourceKind) : CaptureEvent

    /** A buffer of audio samples normalized to [-1.0f, 1.0f]. */
    data class Samples(val buffer: FloatArray) : CaptureEvent

    /** Capture has failed for a non-permission reason. Terminal. */
    data class Failed(val reason: String, val cause: Throwable? = null) : CaptureEvent
}

enum class AudioSourceKind { UNPROCESSED, MIC }
```

`Samples` overrides `equals` and `hashCode` to skip array contents (data classes' default equals on `FloatArray` compares references, which is fine for testing identity but should be explicit). Implementers should accept this; the buffer reference identity is fine for downstream consumers.

### `tuner/data/MicrophoneAudioSourceImpl.kt`

The `callbackFlow` implementation. The body in pseudocode:

```kotlin
class MicrophoneAudioSourceImpl @Inject constructor(
    private val permissionChecker: AudioPermissionChecker,
) : MicrophoneAudioSource {

    override fun samples(): Flow<CaptureEvent> = callbackFlow {
        if (!permissionChecker.hasRecordAudioPermission()) {
            trySend(CaptureEvent.PermissionDenied)
            close()
            return@callbackFlow
        }

        val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE_HZ, CHANNEL_MONO, ENCODING_PCM_16BIT)
        if (minBuffer <= 0) {
            trySend(CaptureEvent.Failed("AudioRecord.getMinBufferSize returned $minBuffer"))
            close()
            return@callbackFlow
        }

        val frames = maxOf(BUFFER_FRAMES_DEFAULT, minBuffer / BYTES_PER_SAMPLE)
        val (record, sourceUsed) = buildRecorder(frames)
            ?: run {
                trySend(CaptureEvent.Failed("AudioRecord failed to initialize"))
                close()
                return@callbackFlow
            }

        try {
            record.startRecording()
            trySend(CaptureEvent.Listening(SAMPLE_RATE_HZ, frames, sourceUsed))

            val shortBuffer = ShortArray(frames)
            val floatBuffer = FloatArray(frames)
            while (!isClosedForSend) {
                val read = record.read(shortBuffer, 0, frames)
                if (read <= 0) continue  // -3, -6, etc. — see AudioRecord error codes
                for (i in 0 until read) {
                    floatBuffer[i] = shortBuffer[i] / SHORT_MAX_AS_FLOAT
                }
                // Defensive copy because downstream consumers may receive on a different dispatcher
                val emitted = floatBuffer.copyOf(read)
                trySend(CaptureEvent.Samples(emitted))
            }
        } catch (e: Throwable) {
            trySend(CaptureEvent.Failed(e.message ?: "audio capture error", e))
        } finally {
            try { record.stop() } catch (_: Throwable) { /* already stopped */ }
            record.release()
        }

        awaitClose { /* finally block above handles cleanup */ }
    }.flowOn(Dispatchers.IO)

    private fun buildRecorder(frames: Int): Pair<AudioRecord, AudioSourceKind>? {
        // Try UNPROCESSED first
        runCatching {
            val r = AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.UNPROCESSED)
                .setAudioFormat(/* 44100, MONO, PCM_16BIT */)
                .setBufferSizeInBytes(frames * BYTES_PER_SAMPLE)
                .build()
            if (r.state == AudioRecord.STATE_INITIALIZED) return r to AudioSourceKind.UNPROCESSED
            r.release()
        }
        // Fallback to MIC
        runCatching {
            val r = AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(/* 44100, MONO, PCM_16BIT */)
                .setBufferSizeInBytes(frames * BYTES_PER_SAMPLE)
                .build()
            if (r.state == AudioRecord.STATE_INITIALIZED) return r to AudioSourceKind.MIC
            r.release()
        }
        return null
    }

    private companion object {
        const val SAMPLE_RATE_HZ = 44_100
        const val BUFFER_FRAMES_DEFAULT = 4096
        const val BYTES_PER_SAMPLE = 2
        const val SHORT_MAX_AS_FLOAT = 32_768.0f
        // CHANNEL_MONO and ENCODING_PCM_16BIT from AudioFormat
    }
}
```

The actual implementation must use the real Android constants (`AudioFormat.CHANNEL_IN_MONO`, `AudioFormat.ENCODING_PCM_16BIT`, etc.) — the pseudocode placeholders above are for readability in this plan.

### `tuner/di/TunerModule.kt`

The Phase 2 module already binds `PitchDetector` to `YinPitchDetector` (stub) and `TunerPresetRepository` to its impl. Update bindings:

- `PitchDetector` → `YinPitchDetector` — binding stays, but the impl class now lives in `common/util/`. Update the import.
- `MicrophoneAudioSource` → `MicrophoneAudioSourceImpl` — new binding.
- Provide a `YinConfig` instance — `@Provides fun provideYinConfig() = YinConfig()` is sufficient; the defaults are the production values.

### `common/di/CommonModule.kt`

Create if it doesn't exist. Bindings:

- `AudioPermissionChecker` → `AndroidAudioPermissionChecker`.

### `AndroidManifest.xml`

- Add `<uses-permission android:name="android.permission.RECORD_AUDIO" />` if not already present.
- No `<uses-feature>` declaration for microphone — keep the app installable on devices that report no mic (we have a clean PermissionDenied/Failed pathway, and gating the install would be heavy-handed).

## Tests

### `YinPitchDetectorTest` (exhaustive)

Located at `app/src/test/java/de/ritzelprimpf/toniqo/common/util/YinPitchDetectorTest.kt`.

The test fixture builds synthetic audio buffers as sine waves at known frequencies:

```kotlin
private fun sine(frequencyHz: Double, sampleRateHz: Int, durationSamples: Int): FloatArray {
    val twoPi = 2 * Math.PI
    return FloatArray(durationSamples) { i ->
        (sin(twoPi * frequencyHz * i / sampleRateHz)).toFloat()
    }
}
```

Test cases:

- **Standard guitar strings** — for each note in `[E2, A2, D3, G3, B3, E4]`, generate a 4096-sample sine wave at that frequency and assert the detector returns a value within ±0.5 Hz of the input. Repeat at 432 Hz reference (the detector is reference-agnostic, but covers the wider frequency range).
- **Drop-tuned low end** — `D1 = 36.71 Hz` and `A0 = 27.5 Hz` (just to confirm the lower bound). `D1` should detect within ±0.5 Hz; `A0` is below the `absoluteMinFrequencyHz` floor and should return `null`.
- **High end** — `E5`, `A5`. Detection within ±1 Hz.
- **Silence** — buffer of all zeros returns `null`.
- **White noise** — buffer of random values in `[-0.1, 0.1]` returns `null` (noise floor — YIN's threshold should reject it).
- **Out-of-range frequencies** — synthetic 3000 Hz sine returns `null` (above `absoluteMaxFrequencyHz`).
- **Buffer too short** — buffer of 256 samples at 100 Hz returns `null` (not enough periods for detection).
- **Reference-pitch independence** — the same 220 Hz sine returns the same frequency regardless of the test's setup pretense about reference; YIN doesn't know about reference pitch.
- **Threshold sensitivity** — building a `YinConfig(threshold = 0.01)` and feeding it noise still returns `null`; building a `YinConfig(threshold = 0.50)` and feeding it a clean sine still detects within tolerance.
- **Parabolic refinement** — feed two different frequencies that fall *between* integer-sample lags (e.g. 440.7 Hz vs. 441.3 Hz at 44.1 kHz) and assert the detector distinguishes them (a non-interpolating implementation would round both to the same integer tau).

Tolerances: within ±0.5 Hz below 200 Hz, within ±1 Hz above 200 Hz, within ±2 Hz above 1000 Hz.

### `YinConfigTest`

Trivial: default-construct it, assert the default values. One test.

### `MicrophoneAudioSourceTest` — limited

Only the **permission-denied path** is unit-testable without an emulator. Inject a fake `AudioPermissionChecker` that returns `false`; collect the flow; assert the only event is `CaptureEvent.PermissionDenied` and that the flow completes.

All other paths (Listening, Samples, Failed, the UNPROCESSED→MIC fallback) require a real `AudioRecord` and are verified in Phase 5.4 once the tuner UI provides a way to observe capture behavior. This is documented at the top of the test class with a `// Real-device paths are verified in Phase 5.4; see Phase5.2-REQUIREMENTS.md` comment.

### `AndroidAudioPermissionCheckerTest`

Not unit-testable without `Context`. Skipped — exercised end-to-end in Phase 5.4 once the tuner UI surfaces the permission state.

## Steps

1. Add `<uses-permission android:name="android.permission.RECORD_AUDIO" />` to `AndroidManifest.xml`.
2. Move the Phase 2 `PitchDetector` interface from `tuner/domain/repository/` to `common/util/`. Update imports.
3. Create `common/permission/AudioPermissionChecker.kt` (interface) and `AndroidAudioPermissionChecker.kt` (impl).
4. Create `common/util/YinConfig.kt`.
5. Create `common/util/YinPitchDetector.kt` with the full algorithm. Replace the Phase 2 stub.
6. Write `YinPitchDetectorTest` and verify the exhaustive test cases above pass.
7. Create `tuner/data/CaptureEvent.kt` (sealed interface + AudioSourceKind enum).
8. Create `tuner/data/MicrophoneAudioSource.kt` (interface).
9. Create `tuner/data/MicrophoneAudioSourceImpl.kt` with the `callbackFlow` implementation.
10. Write `MicrophoneAudioSourceTest` for the permission-denied path.
11. Create or update `common/di/CommonModule.kt` to bind `AudioPermissionChecker`.
12. Update `tuner/di/TunerModule.kt` to bind `MicrophoneAudioSource` and provide `YinConfig`.
13. Update `DECISIONS.md` with: the audio capture parameter choices, the audio source preference and fallback, the YIN threshold value, the `MicrophoneAudioSource` API shape, the `callbackFlow` lifetime model, the `AudioPermissionChecker` abstraction, and the move of `PitchDetector` to `common/util/`.
14. Hand off to the user with a summary. End-to-end audio verification is deferred to Phase 5.4; this phase's user-side check is limited to Gradle sync, build, tests, and a launch sanity check.

## Completion Criteria

See `Phase5.2-REQUIREMENTS.md`.
