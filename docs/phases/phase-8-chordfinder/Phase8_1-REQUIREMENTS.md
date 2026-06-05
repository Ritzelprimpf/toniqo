# Phase 8.1 — Requirements & Acceptance Criteria

Phase 8.1 produces no user-visible functionality. Every requirement is verifiable in code or by unit tests.

## Agent Responsibilities

### `common/model/ChordQuality.kt`
- [ ] Exactly `MAJOR, MINOR, DIMINISHED, AUGMENTED`; no duplicate definition if one already exists.

### `chordfinder/domain/model/SeventhQuality.kt`
- [ ] Seven entries matching `Phase8-PLAN.md` Stage 1; each exposes a **suffix string-resource key**, no baked text.

### `chordfinder/domain/ChordQualityResolver.kt`
- [ ] Pure `object`; `triad(third, fifth)` covers the four valid pairs and throws on any other; `seventh(triad, seventh)` covers the seven valid combinations and throws otherwise.
- [ ] All interval values are named constants.

### `chordfinder/domain/model/`
- [ ] `DegreeChord` carries `degree`, `romanNumeral`, `triadQuality`, `seventhQuality?`, `rootName`, `noteNames`, `symbol`.
- [ ] `ChordFinderInput(rootPitchClass, scaleType, includeSeventhChords)` — references `ScaleType`, not `Mode`.
- [ ] `ChordFinderResult(chords)` always length 7, in degree order.

### `chordfinder/domain/usecase/FindChordsUseCase.kt`
- [ ] Derives every chord's quality from intervals (no major-scale assumption).
- [ ] Uses `ScaleSpeller` for all note names; no hardcoded note strings in the algorithm.
- [ ] Triads → 3 notes / no seventh; sevenths → 4 notes + seventh quality.
- [ ] Correct Roman-numeral case and `°`/`+` symbols.
- [ ] Synchronous pure function (not `suspend`); no Android dependency.
- [ ] Thresholds/offsets are named constants.

### Tests
- [ ] `ChordQualityResolverTest` and `FindChordsUseCaseTest` cover every fixture in `Phase8_1-PLAN.md` → "Tests".
- [ ] The 12×14 parameterised sweep passes (7 chords, valid qualities, letter-correct names, no resolver throw).
- [ ] The A-harmonic-minor sevenths fixture (augmented + `mMaj7`/`dim7`/`maj7♯5` + `G♯` spelling) passes.

### Documentation Updates
- [ ] `DECISIONS.md` gains dated, append-only entries: 14-`ScaleType` target (**explicitly superseding** `APP_SPECIFICATION.md`); interval-derived quality + the enumerated seventh set; guitar-voicings-added scope note (**explicitly superseding** `APP_SPECIFICATION.md`, engine to follow in 8.2); and the removal of the Phase 2 `ChordFinderService` stub in favour of the use case.

### Code Quality
- [ ] No `TODO()`. KDoc on all public types/functions. No magic numbers. All user-visible text in `strings.xml`.

### Handoff
- [ ] Summary lists files added/modified and notes 8.1 is logic-only (no UI, no DI the app launches against).

## User Responsibilities (Verification in Android Studio)
- [ ] Gradle sync, Make Project, and Run All Tests succeed.
- [ ] App still launches; Tuner, Metronome, Key Finder, and the Chord Finder placeholder behave as before.

## Decision Log
- [ ] All four decision entries above are recorded in `DECISIONS.md` before the sub-phase closes.
