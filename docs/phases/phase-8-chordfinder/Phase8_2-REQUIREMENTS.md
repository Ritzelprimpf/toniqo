# Phase 8.2 — Requirements & Acceptance Criteria

8.2 adds no screen, but it adds the data and tuning logic the voicings screen renders. Verifiable by unit tests and the library validation test.

## Agent Responsibilities

### `common/model/GuitarTuning.kt`
- [ ] `id`, `openNotes`, derived `stringCount`; `STANDARD_6` constant correct (E2 A2 D3 G3 B3 E4).
- [ ] `uniformOffsetFrom(base)` returns Δ only when string counts match and every string shares the same offset; else null.

### `chordfinder/domain/model/` voicing types
- [ ] `FretMark` sealed (Open/Muted/Fretted), `Barre`, `VoicingCategory`, `ChordToneRole`, `ChordKey`, `Voicing`.
- [ ] `Voicing` has **variable-length** `marks`/`fingers`; `category`, `fretRange`, `baseFret` are **derived**, not stored.
- [ ] `bassDegree` present, `ROOT` in all v1 data.
- [ ] `validated(...)` enforces all five invariants and throws on violation.

### `chordfinder/domain/VoicingTransposer.kt`
- [ ] Shifts movable voicings by Δ frets (fingers/roots unchanged, barre shifted); returns null for open voicings and off-window results.

### `chordfinder/domain/repository/VoicingRepository.kt` + `data/` impl
- [ ] `lookup` returns `Standard` (tier 1), `UniformOffset` (tier 2, with offset), or `Unsupported` (tier 3).
- [ ] Results ordered by ascending `baseFret`, capped at `MAX_VOICINGS`.
- [ ] JSON loaded once and cached.

### Curated data + generator
- [ ] `assets/chordfinder/voicings_standard_6.json` covers 12 roots × {MAJOR, MINOR, DIMINISHED, AUGMENTED}, ≥ 1 valid voicing each, human-readable.
- [ ] The offline generator exists as a dev-only artifact, **not** wired into the app graph, and is documented as throwaway.

### Tests
- [ ] `GuitarTuningTest`, `VoicingTest`, `VoicingTransposerTest`, `VoicingRepositoryImplTest`, and `VoicingLibraryValidationTest` all present and covering `Phase8_2-PLAN.md` → "Tests".
- [ ] The library validation test loads the **shipped** JSON and asserts all five invariants for every entry plus full chord-key coverage.

### Documentation Updates
- [ ] `DECISIONS.md`: data-driven/chord-keyed voicings + why-not-CAGED; the JSON parser choice; the tier-2 uniform-offset transform rule (shift to preserve sounding pitch, omit open, filter off-neck); neutral labels; variable string count; `bassDegree` seam.

### Code Quality
- [ ] No `TODO()`. KDoc throughout. No magic numbers (`MAX_FRET_SPAN`, `MAX_FRET`, `MAX_VOICINGS`, `PITCH_CLASSES` named). All labels/tags in `strings.xml`.

### Handoff
- [ ] Summary lists files + the asset + the generator; notes the runtime is data-driven and deterministic; flags the parser dependency decision taken.

## User Responsibilities (Verification in Android Studio)
- [ ] Gradle sync, Make Project, Run All Tests succeed (including the library validation test).
- [ ] If kotlinx.serialization was chosen, the user approved the dependency.
- [ ] App still launches; other modules unaffected.

## Decision Log
- [ ] All 8.2 decision entries recorded before the sub-phase closes.
