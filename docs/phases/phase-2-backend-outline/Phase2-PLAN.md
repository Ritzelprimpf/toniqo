# Phase 2 — Backend Outline

## Goal

Define the full backend structure of the app — all packages, classes, interfaces, and public methods — without implementing any business logic. Every public class, interface, and method must have a KDoc comment. Method bodies return stub values (`TODO("Not yet implemented")` or sensible defaults).

The output of this phase is a complete "skeleton" that communicates the architecture clearly to any future implementer (human or AI).

## Scope

- Define all shared domain models in `common/model/`
- Define the `MusicTheory` utility object in `common/util/` (stubbed)
- Define all interfaces, stub implementations, use cases, and ViewModels for each feature module
- Set up Hilt modules with bindings to the stub implementations
- No UI, no real logic, no audio I/O

## Out of Scope

- Any actual algorithm or audio processing implementation
- Any UI composables or fragments
- Navigation wiring (Phase 4)

## Per-Module Structure (Feature-First)

Each feature module lives at `de.ritzelprimpf.toniqo.<feature>/` with this internal layout:

```
<feature>/
├── data/
│   └── <FeatureRepositoryImpl>.kt            ← Stub repository implementations
├── domain/
│   ├── model/                                ← Feature-specific domain models
│   ├── repository/                           ← Repository / service interfaces
│   └── usecase/                              ← Use case classes (stubs)
├── presentation/
│   ├── viewmodel/                            ← ViewModel + UI state
│   └── ui/                                   ← (empty in Phase 2 — Phase 4 fills it)
└── di/
    └── <Feature>Module.kt                    ← Hilt @Module binding interfaces to impls
```

Shared:
```
common/
├── model/        ← Note, Interval, Scale, Mode, Chord
└── util/         ← MusicTheory (object — stateless, see CLAUDE.md §4)
```

## Classes to Create

### `common/model/`

- `Note` — data class: `name: NoteName` (enum: C, CSharp, D, …, B), `octave: Int`. Provides `frequencyHz(referencePitchHz: Double = 440.0): Double`.
- `Interval` — data class: `semitones: Int`. Companion provides named intervals (UNISON, MINOR_SECOND, …, OCTAVE).
- `Scale` — data class: `root: Note`, `intervals: List<Interval>`, `notes: List<Note>` (derived).
- `Chord` — data class: `root: Note`, `quality: ChordQuality` (enum), `notes: List<Note>`. Provides `displayName(): String`.
- `Mode` — enum class with the 7 diatonic modes, each carrying its interval pattern from the root.

### `common/util/`

- `MusicTheory` — top-level `object` (per the documented exception). Stub methods:
  - `noteToFrequency(note: Note, referencePitchHz: Double = 440.0): Double`
  - `frequencyToNote(frequencyHz: Double, referencePitchHz: Double = 440.0): Note`
  - `buildScale(root: Note, mode: Mode): Scale`
  - `buildTriads(scale: Scale): List<Chord>`
  - `buildSeventhChords(scale: Scale): List<Chord>`

### `tuner/`

- `domain/model/TunerPreset` — data class: `id: String`, `displayName: String`, `category: TunerCategory` (enum: STANDARD, OPEN, DROPPED), `stringCount: Int`, `notes: List<Note>` (lowest first).
- `domain/repository/TunerPresetRepository` — interface: `suspend fun getPresets(): List<TunerPreset>`, `suspend fun getPresetById(id: String): TunerPreset?`.
- `data/TunerPresetRepositoryImpl` — stub.
- `domain/repository/PitchDetector` — interface: `fun detectPitch(audioBuffer: FloatArray, sampleRateHz: Int): Double?`.
- `data/YinPitchDetector` — stub (algorithm choice locked in `DECISIONS.md` during Phase 4.1; the *interface* exists in Phase 2 so the rest of the stack can compile).
- `domain/usecase/DetectTunedStringUseCase` — coordinates pitch detection with target frequency comparison (stub).
- `domain/usecase/GetTunerPresetsUseCase` — wraps the repository (stub).
- `presentation/viewmodel/TunerViewModel` — exposes `StateFlow<TunerUiState>`.
- `presentation/viewmodel/TunerUiState` — data class: `availablePresets`, `selectedPreset`, `currentStringIndex`, `detectedFrequencyHz: Double?`, `centsOffTarget: Float?`, `status: TuningStatus` (enum: IDLE, LISTENING, IN_TUNE, FLAT, SHARP, ALL_STRINGS_TUNED).
- `di/TunerModule` — Hilt `@Module` binding interfaces.

### `metronome/`

- `domain/model/MetronomeConfig` — data class: `bpm: Int`, `timeSignatureNumerator: Int`, `timeSignatureDenominator: Int`, `subdivision: Subdivision` (enum: NONE, EIGHTHS, SIXTEENTHS, TRIPLETS).
- `domain/repository/MetronomePlayer` — interface: `fun start(config: MetronomeConfig)`, `fun stop()`, `fun updateConfig(config: MetronomeConfig)`, `val currentBeat: Flow<Int>`.
- `data/AudioTrackMetronomePlayer` — stub.
- `domain/usecase/StartMetronomeUseCase` / `StopMetronomeUseCase` — stubs.
- `presentation/viewmodel/MetronomeViewModel` — exposes `StateFlow<MetronomeUiState>`.
- `presentation/viewmodel/MetronomeUiState` — data class: `isPlaying`, `config: MetronomeConfig`, `currentBeat: Int`.
- `di/MetronomeModule`.

### `keyfinder/`

- `domain/model/KeyFinderInput` — data class: `notes: Set<Note>`, `tonic: Note?`.
- `domain/model/KeyFinderResult` — data class: `scale: Scale`, `modeName: String`, `matchScore: Float` (0.0–1.0), `isFullMatch: Boolean`, `matchesTonic: Boolean`.
- `domain/repository/KeyFinderService` — interface: `fun findKeys(input: KeyFinderInput): List<KeyFinderResult>`.
- `data/KeyFinderServiceImpl` — stub.
- `domain/usecase/FindKeysUseCase` — wraps the service (stub).
- `presentation/viewmodel/KeyFinderViewModel` — exposes `StateFlow<KeyFinderUiState>`.
- `presentation/viewmodel/KeyFinderUiState` — data class: `inputNotes`, `tonic`, `results`.
- `di/KeyFinderModule`.

### `chordfinder/`

- `domain/model/ChordFinderInput` — data class: `root: Note`, `mode: Mode`, `includeSeventhChords: Boolean`.
- `domain/model/ChordFinderResult` — data class: `chords: List<DegreeChord>` where `DegreeChord` carries `degree: Int`, `romanNumeral: String`, `chord: Chord`.
- `domain/repository/ChordFinderService` — interface: `fun findChords(input: ChordFinderInput): ChordFinderResult`.
- `data/ChordFinderServiceImpl` — stub.
- `domain/usecase/FindChordsUseCase` — wraps the service (stub).
- `presentation/viewmodel/ChordFinderViewModel` — exposes `StateFlow<ChordFinderUiState>`.
- `presentation/viewmodel/ChordFinderUiState` — data class: `selectedRoot`, `selectedMode`, `includeSeventhChords`, `result`.
- `di/ChordFinderModule`.

## Steps

1. Create the shared domain models in `common/model/`
2. Create the stub `MusicTheory` object in `common/util/`
3. For each feature in order (tuner → metronome → keyfinder → chordfinder):
   1. Create domain models in `<feature>/domain/model/`
   2. Create interfaces in `<feature>/domain/repository/`
   3. Create use cases in `<feature>/domain/usecase/` (stubs)
   4. Create stub implementations in `<feature>/data/`
   5. Create the ViewModel and UI state in `<feature>/presentation/viewmodel/`
   6. Create the Hilt module in `<feature>/di/`
4. Write unit tests asserting:
   - Each stub repository/service can be constructed and its methods can be called without throwing (except for `TODO()` which should throw `NotImplementedError` — verify with `assertThrows`)
   - Each ViewModel can be constructed and its initial `StateFlow` emits a sensible default state

## Completion Criteria

See `Phase2-REQUIREMENTS.md`.
