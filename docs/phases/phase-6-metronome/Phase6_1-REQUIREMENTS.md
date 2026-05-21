# Phase 6.1 — Requirements & Acceptance Criteria

Phase 6.1 produces no user-visible functionality. Every requirement below is verifiable in the code or by running unit tests. The phase is complete when both the agent checklist and the user checklist pass.

## Agent Responsibilities

### `metronome/domain/model/Subdivision.kt`

- [ ] Enum gains a constructor argument `val multiplier: Int`.
- [ ] `NONE.multiplier == 1`, `EIGHTHS.multiplier == 2`, `SIXTEENTHS.multiplier == 4`, `TRIPLETS.multiplier == 3`.
- [ ] Signature change recorded in `DECISIONS.md`.

### `metronome/data/audio/MetronomeAudioFormat.kt`

- [ ] `internal object MetronomeAudioFormat` exposed with named constants:
  - `SAMPLE_RATE_HZ = 48_000`
  - `CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO`
  - `ENCODING = AudioFormat.ENCODING_PCM_16BIT`
  - `BYTES_PER_SAMPLE = 2`
- [ ] No other public members.

### `metronome/data/audio/ClickKind.kt`

- [ ] `internal enum class ClickKind` with values `ACCENTED, STANDARD, SUBDIVISION`.
- [ ] No constructor arguments, no properties.

### `metronome/data/audio/ClickParameters.kt`

- [ ] `internal object ClickParameters` exposed with the named constants:
  - `CLICK_DURATION_MS = 30`
  - `CLICK_ATTACK_MS = 1`
  - `CLICK_DECAY_RATE = 160.0`
  - `FREQUENCY_HZ_ACCENTED = 1500.0`
  - `FREQUENCY_HZ_STANDARD = 1000.0`
  - `FREQUENCY_HZ_SUBDIVISION = 800.0`
  - `AMPLITUDE_ACCENTED = 0.70`
  - `AMPLITUDE_STANDARD = 0.50`
  - `AMPLITUDE_SUBDIVISION = 0.25`
  - `PCM16_FULL_SCALE = 32_767`
- [ ] KDoc above the object explicitly notes these are v1 starting values, expected to be adjusted during 6.2's smoke testing.

### `metronome/data/audio/ClickSynthesizer.kt`

- [ ] `internal class ClickSynthesizer` with a single public method `fun generate(kind: ClickKind): ShortArray`.
- [ ] Returned buffer length equals `(CLICK_DURATION_MS * SAMPLE_RATE_HZ) / 1000` for every `ClickKind`.
- [ ] No samples exceed `±PCM16_FULL_SCALE`.
- [ ] No magic numbers anywhere in the body — every numeric value resolves to a `ClickParameters` or `MetronomeAudioFormat` constant.
- [ ] No Android dependencies beyond the `AudioFormat` constants imported via `MetronomeAudioFormat`.
- [ ] Class is allocation-light: `generate` allocates exactly one `ShortArray` of the computed length.
- [ ] Deterministic: calling `generate(kind)` twice produces identical `ShortArray` contents.

### `metronome/domain/model/BeatPattern.kt`

- [ ] Top-level internal function `clicksPerBar(numerator: Int, subdivision: Subdivision): Int` implemented as `numerator * subdivision.multiplier`.
- [ ] Top-level internal function `clickKindFor(clickIndexInBar: Int, subdivision: Subdivision): ClickKind` implemented per Item 8 of the decision log:
  - `clickIndexInBar == 0` → `ACCENTED`
  - `clickIndexInBar % subdivision.multiplier == 0` (and non-zero index) → `STANDARD`
  - otherwise → `SUBDIVISION`
- [ ] Both functions are pure (no I/O, no mutation, no Android dependencies).
- [ ] KDoc on both functions documents the rule and references the relevant decision log items.

### `metronome/domain/model/TempoDescriptor.kt`

- [ ] `internal enum class TempoDescriptor` with values `ADAGIO, ANDANTE, MODERATO, ALLEGRO, PRESTO`.
- [ ] Boundary constants in the same file (private or file-internal):
  - `TEMPO_BOUNDARY_ANDANTE = 76`
  - `TEMPO_BOUNDARY_MODERATO = 108`
  - `TEMPO_BOUNDARY_ALLEGRO = 120`
  - `TEMPO_BOUNDARY_PRESTO = 168`
- [ ] Top-level function `tempoDescriptorFor(bpm: Int): TempoDescriptor` implements the lookup per Item 3:
  - `bpm < 76` → `ADAGIO`
  - `bpm < 108` → `ANDANTE`
  - `bpm < 120` → `MODERATO`
  - `bpm < 168` → `ALLEGRO`
  - else → `PRESTO`
- [ ] Function is pure and total over all `Int` inputs (out-of-band BPMs still resolve to a label; clamping is a higher-layer concern).

### Tests

- [ ] `ClickSynthesizerTest` covers all cases listed under "Tests" in `Phase6_1-PLAN.md`: buffer length, no clipping, amplitude hierarchy, peak amplitude matches expected within tolerance, first sample is zero, last sample magnitude below 2% of peak, near-zero DC offset, deterministic output.
- [ ] `SubdivisionTest` asserts the `multiplier` property for all 4 enum values.
- [ ] `BeatPatternTest` covers every (numerator ∈ {2, 3, 4, 5, 6, 7, 9, 12}, subdivision ∈ all 4 values) combination for `clicksPerBar`, and at least one full-bar walkthrough per subdivision in 4/4 for `clickKindFor`. The EIGHTHS-in-/8 no-op identity is asserted explicitly.
- [ ] `TempoDescriptorTest` asserts each labeled BPM boundary (1, 75, 76, 107, 108, 119, 120, 167, 168, 300) plus general round-trip coverage so every enum value is produced.

### Documentation Updates

- [ ] `DECISIONS.md` gains entries (one per decision, dated, append-only) for:
  - **Subdivision enum signature change** — `Subdivision` enum gains a `multiplier: Int` property. Phase 2's no-argument enum is superseded.
  - **Click synthesis over assets** — metronome clicks are synthesized in code rather than bundled as audio assets. Rationale: no sound designer available; consistency with tuner's pure-code audio approach; extensibility for future timbres.
  - **Click parameters as v1 starting values** — frequencies, envelope, durations, and amplitudes are explicitly tunable; adjustments expected during 6.2's manual listening pass.

### Code Quality

- [ ] No `TODO("Not yet implemented")` remains in any 6.1-touched file.
- [ ] All public types and methods have KDoc comments.
- [ ] No magic numbers anywhere in the new code — every numeric value resolves to a named constant.
- [ ] All new files declared `internal` unless explicitly required to be public (`Subdivision` already exists as the public Phase 2 enum and stays so).

### Handoff

- [ ] Summary message to the user lists files added and modified, and notes that 6.1 produces no user-visible UI changes — the metronome placeholder from Phase 4 still appears. Sanity check is "app still launches; no DI errors."

## User Responsibilities (Verification in Android Studio)

- [ ] After applying the changes, **File → Sync Project with Gradle Files** completes without errors.
- [ ] **Build → Make Project** completes successfully.
- [ ] **Run → Run All Tests** (or `./gradlew test`) reports all tests green.
- [ ] App still launches on an Android 12+ emulator/device. The metronome placeholder from Phase 4 still appears (6.1 produces no UI changes).
- [ ] No exceptions in Logcat during launch — particularly no Hilt binding errors.

## Decision Log

- [ ] All decisions listed under "Documentation Updates" are recorded in `DECISIONS.md` before the phase is marked complete.
