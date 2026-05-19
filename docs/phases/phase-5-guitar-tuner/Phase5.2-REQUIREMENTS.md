# Phase 5.2 — Requirements & Acceptance Criteria

Phase 5.2 produces a working audio capture and pitch detection layer. Most acceptance is automated (the YIN algorithm is exhaustively unit-tested), but the `AudioRecord` wrapper requires a real-device check by the user. The phase is complete when both checklists pass.

## Agent Responsibilities

### `AndroidManifest.xml`

- [ ] `<uses-permission android:name="android.permission.RECORD_AUDIO" />` is declared.
- [ ] No `<uses-feature android:name="android.hardware.microphone" />` requirement is added (the app should remain installable on mic-less devices and degrade via `CaptureEvent.PermissionDenied`/`Failed`).

### `common/permission/AudioPermissionChecker.kt`

- [ ] Interface with one method: `fun hasRecordAudioPermission(): Boolean`.
- [ ] KDoc explaining what the method returns and that it does not request permission, only checks.

### `common/permission/AndroidAudioPermissionChecker.kt`

- [ ] Implementation calling `ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.RECORD_AUDIO)`.
- [ ] Constructor-injected via Hilt with `@ApplicationContext`.
- [ ] Bound to the interface in `common/di/CommonModule.kt`.

### `common/util/PitchDetector.kt`

- [ ] Interface moved here from `tuner/domain/repository/`.
- [ ] Signature unchanged: `fun detectPitch(audioBuffer: FloatArray, sampleRateHz: Int): Double?`.
- [ ] Move recorded in `DECISIONS.md`.

### `common/util/YinConfig.kt`

- [ ] Data class with `threshold: Double = 0.15`, `absoluteMinFrequencyHz: Double = 30.0`, `absoluteMaxFrequencyHz: Double = 2000.0`.
- [ ] KDoc per property explaining the tradeoff and the default rationale.

### `common/util/YinPitchDetector.kt`

- [ ] Implements `PitchDetector`.
- [ ] Constructor-injected `YinConfig`.
- [ ] Implements the four-step YIN algorithm: difference function, cumulative mean normalized difference, absolute threshold, parabolic interpolation.
- [ ] Returns `null` for: buffers too short for the lowest target frequency, no τ satisfying the threshold, refined τ ≤ 0, frequency outside the config's min/max bounds.
- [ ] All math in `Double`; input `FloatArray` converted internally.
- [ ] Internal buffer allocation is per-call (correctness first; optimization deferred).
- [ ] No mutable state on the detector instance — multiple concurrent calls (different scopes) must be safe.
- [ ] KDoc cites the YIN paper and outlines the four-step structure.

### `tuner/data/CaptureEvent.kt`

- [ ] Sealed interface `CaptureEvent`.
- [ ] `object PermissionDenied : CaptureEvent` — terminal.
- [ ] `data class Listening(val sampleRateHz: Int, val bufferFrames: Int, val source: AudioSourceKind) : CaptureEvent`.
- [ ] `data class Samples(val buffer: FloatArray) : CaptureEvent` — overrides `equals`/`hashCode` deliberately (document the choice in KDoc).
- [ ] `data class Failed(val reason: String, val cause: Throwable? = null) : CaptureEvent` — terminal.
- [ ] `enum class AudioSourceKind { UNPROCESSED, MIC }`.

### `tuner/data/MicrophoneAudioSource.kt`

- [ ] Interface with one method: `fun samples(): Flow<CaptureEvent>`.
- [ ] KDoc specifies that the first emission is one of `PermissionDenied`/`Listening`/`Failed`, that `Samples` only follows `Listening`, and that the `AudioRecord` lifecycle is bound to the collector's coroutine scope.
- [ ] No `start()`, no `stop()`, no mutable state on the interface or implementation.

### `tuner/data/MicrophoneAudioSourceImpl.kt`

- [ ] Implemented via `callbackFlow { ... }.flowOn(Dispatchers.IO)`.
- [ ] Permission check is the first thing in the flow body; on denial, emits `PermissionDenied` and closes.
- [ ] `AudioRecord` is constructed via `AudioRecord.Builder`, attempting `UNPROCESSED` first and falling back to `MIC` if the first attempt does not yield `STATE_INITIALIZED`.
- [ ] Capture parameters: 44100 Hz, mono, PCM 16-bit, buffer frames = `max(4096, AudioRecord.getMinBufferSize / 2)`.
- [ ] Inside the read loop, `ShortArray` reads are converted to a `FloatArray` in `[-1.0, 1.0]` and a **defensive copy** is emitted.
- [ ] `awaitClose { ... }` plus the `try/finally` in the flow body together guarantee `record.stop()` and `record.release()` always run.
- [ ] `Failed` is emitted for any non-permission error path, including `getMinBufferSize <= 0` and any `Throwable` during recording.
- [ ] Constructor-injected via Hilt; bound to the interface in `TunerModule`.
- [ ] No usage of `GlobalScope` or any explicit `CoroutineScope` field — lifetime is fully driven by the collector.

### `tuner/di/TunerModule.kt`

- [ ] `PitchDetector` binding updated to point at the new `common/util/` location.
- [ ] `MicrophoneAudioSource` → `MicrophoneAudioSourceImpl` binding added.
- [ ] `YinConfig` provided via `@Provides fun provideYinConfig() = YinConfig()`.

### `common/di/CommonModule.kt`

- [ ] Exists (create if absent) and binds `AudioPermissionChecker` → `AndroidAudioPermissionChecker`.

### Tests

- [ ] `YinPitchDetectorTest` exists and covers, at minimum:
  - [ ] Each standard 6-string guitar string at 44.1 kHz: detection within ±0.5 Hz.
  - [ ] `D1 = 36.71 Hz` (low end of 8-string drop tunings): detection within ±0.5 Hz.
  - [ ] `A0 = 27.5 Hz`: returns `null` (below `absoluteMinFrequencyHz`).
  - [ ] High end (`E5`, `A5`): detection within ±1 Hz.
  - [ ] Silence (all-zero buffer): returns `null`.
  - [ ] White noise: returns `null`.
  - [ ] 3000 Hz sine: returns `null` (above `absoluteMaxFrequencyHz`).
  - [ ] Buffer of 256 samples at 100 Hz: returns `null`.
  - [ ] Parabolic refinement: 440.7 Hz and 441.3 Hz sines yield distinct detected frequencies.
  - [ ] Threshold sensitivity: `YinConfig(threshold = 0.01)` rejects noise; `YinConfig(threshold = 0.5)` still detects a clean sine.
- [ ] `YinConfigTest` asserts default values.
- [ ] `MicrophoneAudioSourceTest` covers only the permission-denied path:
  - [ ] Injected fake `AudioPermissionChecker` returns `false`.
  - [ ] Collected flow emits exactly one event: `CaptureEvent.PermissionDenied`.
  - [ ] Flow completes after that emission.
- [ ] A top-of-file comment in `MicrophoneAudioSourceTest` documents that other paths are user-verified per `Phase5.2-REQUIREMENTS.md` and intentionally not covered by unit tests.

### Documentation Updates

- [ ] `DECISIONS.md` gains entries for:
  - Audio capture parameters: 44100 Hz / mono / PCM 16-bit / 4096 frames (max with `getMinBufferSize`).
  - Audio source preference: UNPROCESSED preferred, MIC fallback.
  - YIN threshold default: 0.15.
  - `MicrophoneAudioSource` API shape: `Flow<CaptureEvent>`, no start/stop, single responsibility.
  - `callbackFlow` for lifetime safety.
  - `AudioPermissionChecker` abstraction in `common/permission/`.
  - `PitchDetector` interface moved from `tuner/` to `common/util/`.
- [ ] `FUTURE_IMPROVEMENTS.md` may receive new entries if any are surfaced during implementation (e.g. allocating YIN's internal buffers from a pool to reduce GC pressure if profiling later shows it's needed).

### Code Quality

- [ ] No magic numbers in audio code — sample rate, buffer frames, bytes-per-sample, short-max-as-float are named constants.
- [ ] No `runBlocking` outside of test code.
- [ ] No swallowed exceptions — every `catch` either re-emits as `CaptureEvent.Failed` or has a comment explaining why the swallow is safe (currently only `record.stop()` in the finally block, which can throw if the record is already stopped).
- [ ] No leaked `AudioRecord` — verified by code inspection of the finally block.
- [ ] All new public types and methods have KDoc.

### Handoff

- [ ] Summary message to the user includes:
  - Files added, modified, removed.
  - A reminder that the tuner placeholder screen from Phase 4 still appears; no UI changes in 5.2.
  - A note that end-to-end audio verification is intentionally deferred to Phase 5.4 once the tuner UI exists.
  - Anything to double-check after Gradle sync, particularly that no new Hilt binding errors appear in Logcat on launch.

## User Responsibilities (Verification in Android Studio)

### Build & Tests

- [ ] After applying the changes, **File → Sync Project with Gradle Files** completes without errors.
- [ ] **Build → Make Project** completes successfully.
- [ ] **Run → Run All Tests** reports all tests green, including the new `YinPitchDetectorTest`.

### Sanity Check

- [ ] App launches and the tuner placeholder screen from Phase 4 still appears, unchanged.
- [ ] No exceptions or `RuntimeException` traces in Logcat during launch (particularly no Hilt binding errors from the new `MicrophoneAudioSource` and `AudioPermissionChecker` bindings).

> **End-to-end verification of `MicrophoneAudioSource` (permission flow, Listening event, sample stream, UNPROCESSED-vs-MIC source selection, cleanup on backgrounding) is deferred to Phase 5.4**, when the tuner UI gives the user a real way to observe and verify capture behavior. There is no test composable or throwaway wiring in this phase.

## Decision Log

- [ ] All decisions listed under "Documentation Updates" are recorded in `DECISIONS.md` before the phase is marked complete.
