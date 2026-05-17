# Phase 2 — Requirements & Acceptance Criteria

All items below must be satisfied for Phase 2 to be considered complete.

## Common / Shared

- [ ] `Note` data class in `common/model/` with `name: NoteName` (enum), `octave: Int`, and a `frequencyHz(referencePitchHz: Double = 440.0): Double` method
- [ ] `NoteName` enum covering all 12 pitch classes (C, CSharp, D, DSharp, E, F, FSharp, G, GSharp, A, ASharp, B) with display/parse helpers for both sharp and flat names
- [ ] `Interval` data class with a `semitones: Int` field
- [ ] `Scale` data class with `root: Note`, `intervals: List<Interval>`, derived `notes: List<Note>`
- [ ] `Chord` data class with `root: Note`, `quality: ChordQuality`, `notes: List<Note>`, and `displayName()`
- [ ] `ChordQuality` enum (MAJOR, MINOR, DIMINISHED, AUGMENTED, plus seventh variants if implemented now)
- [ ] `Mode` enum covering the 7 diatonic modes, each carrying its interval pattern
- [ ] `MusicTheory` top-level `object` in `common/util/` with stub methods:
  - [ ] `noteToFrequency(note: Note, referencePitchHz: Double = 440.0): Double`
  - [ ] `frequencyToNote(frequencyHz: Double, referencePitchHz: Double = 440.0): Note`
  - [ ] `buildScale(root: Note, mode: Mode): Scale`
  - [ ] `buildTriads(scale: Scale): List<Chord>`
  - [ ] `buildSeventhChords(scale: Scale): List<Chord>`

## Guitar Tuner Module

- [ ] `TunerPreset` data class with `id`, `displayName`, `category`, `stringCount`, `notes`
- [ ] `TunerCategory` enum (STANDARD, OPEN, DROPPED)
- [ ] `TunerPresetRepository` interface with `getPresets()` and `getPresetById(id)`
- [ ] `TunerPresetRepositoryImpl` stub bound via Hilt
- [ ] `PitchDetector` interface with `detectPitch(audioBuffer: FloatArray, sampleRateHz: Int): Double?`
- [ ] `YinPitchDetector` stub bound via Hilt
- [ ] `DetectTunedStringUseCase` and `GetTunerPresetsUseCase` exist
- [ ] `TunerViewModel` extends `ViewModel`, exposes `StateFlow<TunerUiState>`
- [ ] `TunerUiState` includes `availablePresets`, `selectedPreset`, `currentStringIndex`, `detectedFrequencyHz`, `centsOffTarget`, `status`
- [ ] `TuningStatus` sealed interface or enum (IDLE, LISTENING, IN_TUNE, FLAT, SHARP, ALL_STRINGS_TUNED)

## Metronome Module

- [ ] `MetronomeConfig` data class with `bpm`, `timeSignatureNumerator`, `timeSignatureDenominator`, `subdivision`
- [ ] `Subdivision` enum (NONE, EIGHTHS, SIXTEENTHS, TRIPLETS)
- [ ] `MetronomePlayer` interface with `start`, `stop`, `updateConfig`, `currentBeat: Flow<Int>`
- [ ] `AudioTrackMetronomePlayer` stub bound via Hilt
- [ ] `StartMetronomeUseCase` and `StopMetronomeUseCase` exist
- [ ] `MetronomeViewModel` exposes `StateFlow<MetronomeUiState>`
- [ ] `MetronomeUiState` includes `isPlaying`, `config`, `currentBeat`

## Key Finder Module

- [ ] `KeyFinderInput` data class with `notes: Set<Note>` and `tonic: Note?`
- [ ] `KeyFinderResult` data class with `scale`, `modeName`, `matchScore: Float`, `isFullMatch: Boolean`, `matchesTonic: Boolean`
- [ ] `KeyFinderService` interface with `findKeys(input: KeyFinderInput): List<KeyFinderResult>`
- [ ] `KeyFinderServiceImpl` stub bound via Hilt
- [ ] `FindKeysUseCase` exists
- [ ] `KeyFinderViewModel` exposes `StateFlow<KeyFinderUiState>`
- [ ] `KeyFinderUiState` includes `inputNotes`, `tonic`, `results`

## Chord Finder Module

- [ ] `ChordFinderInput` data class with `root: Note`, `mode: Mode`, `includeSeventhChords: Boolean`
- [ ] `ChordFinderResult` data class wrapping `List<DegreeChord>`
- [ ] `DegreeChord` data class with `degree: Int`, `romanNumeral: String`, `chord: Chord`
- [ ] `ChordFinderService` interface with `findChords(input: ChordFinderInput): ChordFinderResult`
- [ ] `ChordFinderServiceImpl` stub bound via Hilt
- [ ] `FindChordsUseCase` exists
- [ ] `ChordFinderViewModel` exposes `StateFlow<ChordFinderUiState>`
- [ ] `ChordFinderUiState` includes `selectedRoot`, `selectedMode`, `includeSeventhChords`, `result`

## Dependency Injection

- [ ] Each feature has a Hilt `@Module` (`TunerModule`, `MetronomeModule`, `KeyFinderModule`, `ChordFinderModule`) under `<feature>/di/` that binds its interfaces to stub implementations
- [ ] The Hilt application class (`ToniqoApplication`) configured in Phase 1 remains valid
- [ ] All bindings use constructor injection (no `@Provides` for things that could be `@Inject constructor`)

## Code Quality

- [ ] All public classes, interfaces, methods, and non-trivial properties have KDoc comments
- [ ] All un-implemented method bodies use `TODO("Not yet implemented")` or return sensible default values explicitly justified in the KDoc
- [ ] No method body contains real business logic (this phase is stubs only)
- [ ] No magic numbers — constants live in `companion object` or top-level `object`s

## Tests

- [ ] At least one unit test per feature module verifying:
  - The ViewModel can be constructed and emits a sensible default `UiState`
  - The stub repository/service can be constructed; calling its `TODO()` methods throws `NotImplementedError` (asserted with `assertThrows<NotImplementedError>`)
- [ ] At least one unit test per data class verifying it constructs and `equals`/`hashCode` work as expected
- [ ] `./gradlew test` passes with all tests green

## Build

- [ ] `./gradlew assembleDebug` succeeds with zero errors
- [ ] No new lint warnings introduced beyond those already accepted at end of Phase 1

## Decision Log

- [ ] Any non-trivial decision taken during Phase 2 is recorded in `DECISIONS.md`
