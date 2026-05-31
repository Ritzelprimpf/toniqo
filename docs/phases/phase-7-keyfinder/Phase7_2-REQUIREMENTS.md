# Phase 7.2 — Requirements & Acceptance Criteria

Phase 7.2 adds no Key Finder screen yet, but it changes the tuner's internals (a parity-preserving move), so the tuner regression is a first-class acceptance item.

## Agent Responsibilities

### Shared `audio/` module
- [ ] New top-level `audio/` package exists (sibling to `common/` and `ui/`).
- [ ] `AudioCaptureSource` interface + `AudioRecordCaptureSource` impl moved here, capture parameters unchanged from the tuner.
- [ ] `PitchDetector` interface + `YinPitchDetector` impl moved here, YIN threshold unchanged.
- [ ] `audio/di/` Hilt module binds both interfaces; tuner's old bindings for these are removed.
- [ ] No behavioural change to the moved code (move, not rewrite).

### Tuner re-point
- [ ] Tuner consumes the `audio/` interfaces; no `tuner/` code references its own former capture/YIN classes.
- [ ] Tuner-specific logic (cents, target selection, 500 ms sustained-tune state machine) remains in `tuner/`.

### `keyfinder/domain/repository/NoteDetector.kt`
- [ ] Interface with `detectedNotes(): Flow<Int>`, `suspend fun start()`, `suspend fun stop()`.

### `keyfinder/data/StableNoteDetectorImpl.kt`
- [ ] Composes `AudioCaptureSource` + `PitchDetector` + `MusicTheory.frequencyToNote`.
- [ ] Emits a pitch class only after a sustained confirmation window; debounces so a single held note emits once.
- [ ] Re-arms on silence/`null` or pitch-class change.
- [ ] Confirmation window and debounce values are named constants and recorded in `DECISIONS.md`.

### DI & permissions
- [ ] `keyfinder/di/` binds `NoteDetector` → `StableNoteDetectorImpl`.
- [ ] `RECORD_AUDIO` runtime flow reused (helper lifted to a shared location if it was tuner-local), no duplication.

### Tests
- [ ] `StableNoteDetectorImplTest` covers: single emission for a held note, no re-emit until change/silence, transient shorter than window emits nothing, A→silence→A emits twice, detune maps to nearest pitch class, `null` resets.
- [ ] Relocated `audio/` tests pass in their new package.
- [ ] **The full existing tuner test suite passes unchanged.**

### Documentation Updates
- [ ] `DECISIONS.md`: shared `audio/` module created — feature-first deviation justified per `CLAUDE.md` §3; and the Key Finder detector confirmation/debounce thresholds.

### Code Quality
- [ ] No `TODO()` in 7.2 files. KDoc on public APIs. No magic numbers (windows, thresholds, sample rate all named).

### Handoff
- [ ] Summary explicitly flags the tuner refactor and lists what the user must re-verify (tuner builds, tuner tests green, tuner still tunes correctly on device).

## User Responsibilities (Verification in Android Studio)
- [ ] Gradle sync + Build → Make Project succeed.
- [ ] Run All Tests green — **including the tuner suite**.
- [ ] On device: the **Tuner still works end-to-end exactly as before** (no regression from the audio move).
- [ ] No Hilt/DI errors in Logcat at launch (the audio bindings moved).

## Decision Log
- [ ] Shared `audio/` module + detector thresholds recorded in `DECISIONS.md` before close.
