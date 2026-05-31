# Phase 7.2 — Shared Audio & Note Detection

## Goal

Make microphone note entry possible for Key Finder by (a) promoting the tuner's audio capture and YIN pitch detection into a shared `audio/` module **without changing the tuner's behaviour**, and (b) building, on top of it, a detector that turns a sustained played note into a single confident pitch class for Key Finder to add to the list.

By the end of 7.2 the tuner still works exactly as before, and Key Finder has a `suspend`/`Flow` API that emits "the user just played a stable note: pitch class X."

## Scope

- Create the top-level **`audio/`** module and move the tuner's capture + YIN code into it (parity-preserving refactor).
- Re-point the tuner to consume `audio/`. Tuner unit tests must pass unchanged.
- Build `keyfinder/data/StableNoteDetector` (and its domain interface) on top of `audio/`.
- `RECORD_AUDIO` permission plumbing reused from the tuner pattern.
- Tests for the new detector with a fake audio source. Tuner regression tests re-run.

## Out of Scope

- Note-list state and recompute → 7.3.
- Any UI, mic button, or permission-denied screen → 7.4.
- Changing the YIN algorithm or capture parameters → they move as-is; tuning them is not this phase.

## Reading Order Before Starting

1. `Phase7-PLAN.md` → "Architecture & Package Notes"
2. The existing tuner audio code (`tuner/data/` capture + `PitchDetector`/YIN) and `Phase5_2` docs
3. `DECISIONS.md` → the Phase 5 audio-parameter and YIN-threshold entries
4. `common/util/MusicTheory.kt` → `frequencyToNote` (already maps Hz → `Note` with sharp spelling)
5. This file

## Decisions To Lock At The Start Of 7.2

- [ ] **Module location & name.** Recommended: a new top-level `audio/` package (sibling to `common/`, `ui/`), not `common/audio/` (which is reserved for *pure* music theory). Record the deviation and rationale in `DECISIONS.md` per `CLAUDE.md` §3.
- [ ] **What moves vs. what stays.** Recommended split: the *generic* pieces move to `audio/` — `AudioCaptureSource` (the `AudioRecord` wrapper that emits buffers) and `PitchDetector` (YIN, buffer → `Double?` Hz). Tuner-*specific* logic (cents math, target/string selection, the 500 ms sustained-tune state machine) **stays** in `tuner/`. Confirm this boundary before moving code.
- [ ] **Detector confirmation rule** for Key Finder (below) — pick the debounce/stability thresholds and record them.

## Implementation Details

### `audio/` module (promoted)

- `audio/AudioCaptureSource` — interface + `AudioRecordCaptureSource` impl. Same capture parameters the tuner already uses (44.1 kHz mono PCM16, buffer floor per the Phase 5.2 decision). Exposes audio frames as a `Flow` and owns start/stop and resource release. **Behaviour must be byte-for-byte the tuner's existing behaviour** — this is a move, not a rewrite.
- `audio/PitchDetector` — interface + `YinPitchDetector` impl. `fun detect(buffer: FloatArray, sampleRate: Int): Double?` (fundamental Hz or null). The YIN threshold constant moves with it unchanged.
- A Hilt module under `audio/di/` binds the interfaces. The tuner's existing Hilt bindings for these classes are removed in favour of the shared ones.

> **Parity discipline.** After moving, the tuner's own unit tests are the regression gate. Do not "improve" the moved code. Any unavoidable signature change (e.g. a package rename in an import) is mechanical. If a genuine behavioural change is required, stop and ask.

### `keyfinder/domain/repository/NoteDetector.kt`

```kotlin
interface NoteDetector {
    /** Emits a pitch class (0..11) each time the user plays and holds a clear, stable note. */
    fun detectedNotes(): Flow<Int>
    suspend fun start()
    suspend fun stop()
}
```

### `keyfinder/data/StableNoteDetectorImpl.kt`

Composes `AudioCaptureSource` + `PitchDetector` + `MusicTheory.frequencyToNote`:

1. Collect capture frames, run YIN → Hz, map Hz → `Note` via `frequencyToNote` → pitch class.
2. **Confirmation rule** (Key Finder wants discrete notes, not a continuous stream): emit a pitch class only after the *same* pitch class has been detected on consecutive frames for a short sustained window (suggested **~150–250 ms**), then **debounce** so a single held note emits once, not repeatedly. After an emission, require either a gap of silence/`null` or a change of pitch class before the next emission. Exact window/debounce values are chosen and recorded at the start of 7.2.
3. `null` (silence/unclear) resets the in-progress confirmation.

This class is the only place audio meets Key Finder. It is constructor-injected; the use case / ViewModel in 7.3 consumes `NoteDetector`, never `AudioCaptureSource` directly.

### Permissions

Reuse the tuner's runtime `RECORD_AUDIO` flow (Activity Result API). 7.2 only needs the capability available to the detector; the Key Finder-specific permission-denied **UI** is 7.4. Do not duplicate the permission helper — if the tuner has a reusable helper, lift it to `ui/` or `audio/`; if it is screen-local, note it and 7.4 will share it.

## Tests

### `audio/`
- The moved `YinPitchDetector` keeps its existing tests (relocated alongside the code). Add none unless a gap exists.
- `AudioCaptureSource` — if the tuner had capture tests, they move; otherwise a light test that start/stop manages state without leaking is sufficient (true audio I/O is user-verified on device).

### `keyfinder/data/StableNoteDetectorImplTest`
With a **fake** `AudioCaptureSource` + **fake/mock** `PitchDetector` (feed scripted Hz/null sequences):
- A held in-tune note that persists past the confirmation window emits its pitch class **exactly once**.
- A note held continuously does **not** re-emit until pitch class changes or silence intervenes.
- A brief transient shorter than the window emits **nothing**.
- A sequence A → silence → A emits the pitch class **twice** (silence re-arms).
- Slightly detuned input still maps to the nearest pitch class (lean on `frequencyToNote`).
- `null` mid-confirmation resets and prevents emission.

### Tuner regression
- The entire existing tuner test suite runs green against the promoted `audio/` classes. This is an explicit acceptance item, not an afterthought.

## Steps

1. Lock the three start-of-phase decisions (module name, move boundary, confirmation thresholds); record in `DECISIONS.md`.
2. Create `audio/`; move `AudioCaptureSource` + `PitchDetector`/YIN with their tests; update imports.
3. Re-point tuner bindings/usages to `audio/`. Run the tuner suite — must be green.
4. Add `NoteDetector` interface (`keyfinder/domain/repository/`).
5. Implement `StableNoteDetectorImpl` (`keyfinder/data/`) with the confirmation/debounce rule. Tests.
6. Add the `keyfinder/di/` Hilt binding for `NoteDetector`.
7. Confirm/lift the `RECORD_AUDIO` permission helper for reuse.
8. Append `DECISIONS.md` entries (shared `audio/` module + deviation justification; detector confirmation thresholds).
9. Hand off with a summary, flagging that the tuner was refactored and its suite must be re-verified by the user on device.

## Completion Criteria

See `Phase7_2-REQUIREMENTS.md`.
