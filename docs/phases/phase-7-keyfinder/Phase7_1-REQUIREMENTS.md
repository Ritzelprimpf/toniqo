# Phase 7.1 — Requirements & Acceptance Criteria

Phase 7.1 produces no user-visible functionality. Every requirement is verifiable in code or by running unit tests. The phase is complete when both checklists pass.

## Agent Responsibilities

### `common/model/ScaleFamily.kt`
- [ ] Enum with exactly `DIATONIC`, `HARMONIC_MINOR`, `MELODIC_MINOR`.

### `common/model/ScaleType.kt`
- [ ] 14 entries matching the `Phase7-PLAN.md` inventory.
- [ ] Each carries `family`, `intervalsFromRoot: IntArray` (7 ascending offsets starting at 0, all 0–11), and `rankOrder: Int`.
- [ ] The 7 diatonic patterns equal the corresponding `Mode.intervalsFromRoot` (asserted by test, not duplicated silently).
- [ ] `rankOrder` values are 0–13, unique, and match the common-first order.
- [ ] Primary-label and subtitle **string-resource keys** are exposed; no display text is hardcoded in the enum.
- [ ] KDoc on each exotic type names its parent scale and degree.

### `common/util/ScaleSpeller.kt`
- [ ] Pure `object`; no state, no I/O, no Android.
- [ ] `rootName(rootPitchClass, type)` returns the conventional root spelling from the canonical table (with documented overrides if any).
- [ ] `scaleNoteNames(rootPitchClass, type)` returns 7 names, one per degree, each on a distinct letter A–G with the correct accidental.
- [ ] Accidental glyphs are named constants, not inline literals.

### `keyfinder/domain/model/`
- [ ] `ScaleCandidate(rootPitchClass, type)` with derived `pitchClasses: Set<Int>` (7 values incl. the root).
- [ ] `KeyFinderInput(pitchClasses: Set<Int>, rootPitchClass: Int?)`.
- [ ] `ScaleMatch(candidate, percent, isFull, isRootMatch, rank)`; carries **no** display strings or resources.

### `keyfinder/domain/ScaleCatalog.kt`
- [ ] Produces exactly 168 candidates (12 roots × 14 types), each pair once.
- [ ] Pure; the candidate list is an immutable constant.

### `keyfinder/domain/usecase/MatchScalesUseCase.kt`
- [ ] Returns `emptyList()` when distinct pitch classes < 3.
- [ ] Computes `percent` per the locked formula (`points/maxPoints`, round half up).
- [ ] Sets `isFull` (`covered == n`) and `isRootMatch` correctly.
- [ ] Excludes `covered == 0`.
- [ ] Sorts percent desc → `rankOrder` asc → root pc asc; returns at most 7 with `rank` 1..7.
- [ ] Synchronous pure function (not `suspend`); no Android dependency.
- [ ] All thresholds are named constants (`MIN_NOTES_TO_MATCH=3`, `MAX_RESULTS=7`, `SCALE_SIZE=7`, `PITCH_CLASSES=12`).

### Tests
- [ ] `ScaleTypeTest`, `ScaleSpellerTest`, `ScaleCatalogTest`, `MatchScalesUseCaseTest` all present and covering the cases in `Phase7_1-PLAN.md` → "Tests".
- [ ] Every canonical worked example from `Phase7-PLAN.md` (43%, 50%/38% split, 100% tie, root-breaks-tie 100%/88%, stray-note 88%, cap at 7, `isFull` at low percent) has a passing assertion.
- [ ] The letter-per-degree spelling rule is verified across all 12 roots × 14 types.
- [ ] The diatonic-equals-`Mode` cross-check passes.

### Documentation Updates
- [ ] `DECISIONS.md` gains dated, append-only entries for: the match-scoring formula (**explicitly superseding** the `APP_SPECIFICATION.md` "% of input notes" definition); single root note folded into the percentage; the 14-type inventory with exclusions; the ranking rule (**explicitly superseding** the separate-tonic-ranking step); the ≥3 gate; conventional spelling; and the `ScaleType` superset alongside the retained `Mode`.

### Code Quality
- [ ] No `TODO()` left in any 7.1 file.
- [ ] KDoc on all public types and functions.
- [ ] No magic numbers; no hardcoded note-name strings inside algorithms (work in pitch classes / semitone offsets, convert to display at the `ScaleSpeller` boundary only).
- [ ] All user-visible label/subtitle text is in `strings.xml`.

### Handoff
- [ ] Summary lists files added/modified and notes that 7.1 adds no UI and no DI bindings the app launches against (so app startup is unaffected; this is a logic-only drop).

## User Responsibilities (Verification in Android Studio)
- [ ] Gradle sync succeeds.
- [ ] Build → Make Project succeeds.
- [ ] Run All Tests is green.
- [ ] App still launches and the existing Tuner, Metronome, and the Key Finder placeholder screen behave as before (7.1 changes no runtime behaviour).

## Decision Log
- [ ] All seven decision entries above are recorded in `DECISIONS.md` before the sub-phase is closed.
