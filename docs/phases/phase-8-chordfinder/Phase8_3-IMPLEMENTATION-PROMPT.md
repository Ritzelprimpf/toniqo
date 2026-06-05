# Implementation Prompt — Phase 8.3 (Chord Finder · ViewModel, State, Seeding & Tuning Source)

> Paste this to start a fresh implementation session for Phase 8.3. Phases 8.1 and 8.2 must be complete.

---

You are implementing **Phase 8.3 — ViewModel, State, Seeding & Tuning Source** of Toniqo. You write code; the user owns the build, device, and Git. Do not run Gradle or git.

**Before writing any code, read, in this order:**
1. `PROJECT_PLAN.md`, `CLAUDE.md`, `IMPLEMENTATION_NOTES.md`, `DECISIONS.md`.
2. `Phase8-PLAN.md` → "The Tuning Source" and "Key Finder → Chord Finder Seeding".
3. `Phase8_3-PLAN.md` and `Phase8_3-REQUIREMENTS.md` — your scope.
4. `Phase8_1-PLAN.md`, `Phase8_2-PLAN.md` (the engines you compose).
5. The Phase 6 metronome **persistence** decision in `DECISIONS.md` (reuse that mechanism — likely DataStore Preferences; confirm what it actually is before using it).
6. The Phase 7.3/7.4 `KeyFinderViewModel` (state-shape precedent and the place you add a one-line publish hook).

**Hard constraints:** as before. Additionally: ViewModels expose immutable state via `StateFlow`; no business logic in composables; the two cross-module stores are `@Singleton`. **Stop and ask** on ambiguity — especially if the metronome's persistence mechanism or `Note`'s octave/pitch-class API differ from what `Phase8_3-PLAN.md` assumes.

**Two assumptions to verify in your first step (adjust + flag if wrong, don't redesign):**
- The metronome's persistence is reusable for Chord Finder's selection.
- `Note` exposes an absolute, octave-aware semitone value (needed so `GuitarTuning.uniformOffsetFrom` from 8.2 is meaningful). If not, add a small pure helper rather than changing `Note`.

**Your task this session (state + integration; no composables, no nav routes):**
1. `common/state/LatestKeyResultStore.kt` and `common/state/SelectedTuningStore.kt` — `@Singleton`, `StateFlow`-based, app-scoped.
2. Additive writers: `KeyFinderViewModel` publishes `results.firstOrNull()` on recompute (no other behaviour change); the tuner publishes its selected tuning via a `TuningPresetMapper` (lives in the tuner module, outputs the shared `common` `GuitarTuning`).
3. `chordfinder/data/ChordFinderSelectionRepository.kt` (+ impl) — persist `{rootPitchClass, scaleType, includeSeventhChords, hasUserSelection}` on the metronome's mechanism.
4. `chordfinder/presentation/viewmodel/ChordFinderViewModel.kt` + `ChordFinderUiState.kt` — root/mode/toggle intents, chord list via `FindChordsUseCase`, the **seed-once-then-user-owned** algorithm, and the selected-`ChordKey` exposure for navigation.
5. `chordfinder/presentation/viewmodel/ChordVoicingsViewModel.kt` + `ChordVoicingsUiState.kt` — reads `SelectedTuningStore`, calls `VoicingRepository.lookup` off the main thread, maps to `STANDARD | UNIFORM_OFFSET | UNSUPPORTED`.
6. `chordfinder/di/ChordFinderModule.kt` — bind the use case, repositories, and the two singleton stores (shared across modules).

**The seed algorithm is the heart of this.** Unit-test exactly (per `Phase8_3-PLAN.md` → "Tests"): persisted selection wins; else Key Finder top seeds 1:1; else A Aeolian; the store is read **once** (a later `publish` never changes a seeded state); a user edit persists, sets `hasUserSelection`, and makes subsequent `publish`es no-ops. Also test the voicings ViewModel across the three tiers, both stores, and the `TuningPresetMapper` (e.g. E♭ standard → Δ=−1 from `STANDARD_6`). Include a Key Finder **parity test** proving its own behaviour is unchanged.

**When done:**
- Append the 8.3 decisions to `DECISIONS.md`: the two cross-module stores (+ why in-memory suffices); `GuitarTuning` promoted to `common/` because the tuner is its second consumer; seed-once-then-user-owned; tuning inherited read-only (in-Chord-Finder picker deferred).
- Summary: files added/modified (including the two cross-module edits); confirm other modules still pass their tests.
- Map to commits (e.g. `feat: cross-module result/tuning stores`, `feat: chord finder viewmodels + seeding`). Do not commit.

Confirm you have read the docs and verified the two assumptions, then proceed file by file with tests.
