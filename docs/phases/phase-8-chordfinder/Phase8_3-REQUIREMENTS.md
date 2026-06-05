# Phase 8.3 — Requirements & Acceptance Criteria

8.3 makes the module functional in logic (drivable from tests) without its real screens.

## Agent Responsibilities

### Shared stores
- [ ] `LatestKeyResultStore` and `SelectedTuningStore` are `@Singleton`, `StateFlow`-based, app-scoped.
- [ ] `KeyFinderViewModel` publishes its top result on every recompute; Key Finder's own behaviour is otherwise unchanged (parity test).
- [ ] The tuner publishes its selected tuning via `TuningPresetMapper`; default is `STANDARD_6` until the tuner is used.

### Persistence
- [ ] `ChordFinderSelectionRepository` persists `{rootPitchClass, scaleType, includeSeventhChords, hasUserSelection}` using the metronome's mechanism (no new persistence dependency).

### `ChordFinderViewModel`
- [ ] Implements the seed algorithm exactly: persisted-wins → else Key-Finder-top (1:1) → else A Aeolian.
- [ ] Reads the Key Finder store **once**; never overrides a user-owned selection afterward.
- [ ] `setRoot`/`setScaleType`/`toggleSevenths` recompute via `FindChordsUseCase`, persist, and set `hasUserSelection`.
- [ ] Exposes the selected `ChordKey` + display name for navigation.

### `ChordVoicingsViewModel`
- [ ] Reads `SelectedTuningStore`, calls `VoicingRepository.lookup` off the main thread, and maps to `STANDARD | UNIFORM_OFFSET | UNSUPPORTED` with offset and voicings.
- [ ] Tier-3 still exposes standard voicings plus the unsupported flag.

### DI
- [ ] `ChordFinderModule` provides the use case, repositories, and the two singleton stores, shared correctly across modules.

### Tests
- [ ] `ChordFinderViewModelTest`, `ChordVoicingsViewModelTest`, and the store/writer tests cover every case in `Phase8_3-PLAN.md` → "Tests".
- [ ] The seed-once and user-owned-stickiness behaviours are explicitly asserted.

### Documentation Updates
- [ ] `DECISIONS.md`: the two cross-module stores (and why in-memory suffices); `GuitarTuning` promoted to `common/` because the tuner is its second consumer; the seed-once-then-user-owned rule; tuning inherited read-only (in-Chord-Finder picker deferred).

### Code Quality
- [ ] No `TODO()`. KDoc throughout. No magic numbers. Strings in `strings.xml`.

### Handoff
- [ ] Summary lists files added/modified (including the two cross-module edits) and confirms other modules still pass their tests.

## User Responsibilities (Verification in Android Studio)
- [ ] Gradle sync, Make Project, Run All Tests succeed.
- [ ] App launches; Tuner, Metronome, Key Finder behave as before (the writes are additive).

## Decision Log
- [ ] All 8.3 decision entries recorded before the sub-phase closes.
