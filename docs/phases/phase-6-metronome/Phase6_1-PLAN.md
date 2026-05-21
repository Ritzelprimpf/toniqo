# Phase 6.1 — Click Synthesizer & Audio Format Foundation

## Goal

Implement the metronome's audio foundation: the click synthesizer that turns named parameters into PCM buffers, the audio-format configuration shared by synthesizer and player, and the pure helper functions that drive click-kind selection and tempo-descriptor lookup. This is **pure Kotlin** — no `AudioTrack`, no `AudioRecord`, no Android lifecycle, no ViewModel, no UI. Everything in this sub-phase is unit-testable in isolation on the JVM.

By the end of 6.1, the metronome's data and synthesis layer is complete and tested. Phases 6.2 (player), 6.3 (ViewModel), and 6.4 (UI) consume what 6.1 produces — they should never need to add a click kind, change a synthesis parameter outside its constant, or introduce a new helper that belongs here.

## Scope

- Implement `ClickSynthesizer` producing `ShortArray` buffers for each `ClickKind`.
- Define `ClickKind` enum (`ACCENTED`, `STANDARD`, `SUBDIVISION`).
- Define `MetronomeAudioFormat` config object with sample rate, channel config, and PCM encoding constants.
- Define `ClickParameters` (frequencies, envelope, durations, amplitudes) as named constants.
- Add `Subdivision.multiplier: Int` to the existing Phase 2 `Subdivision` enum.
- Implement pure helper functions: `clicksPerBar(numerator, subdivision)`, `clickKindFor(clickIndexInBar, subdivision)`, `tempoDescriptorFor(bpm)`.
- Define `TempoDescriptor` enum or string constants with the five labels and locked BPM boundaries.
- Exhaustive unit tests for the synthesizer and helpers.

## Out of Scope

- Anything involving `AudioTrack`, audio focus, playback scheduling → Phase 6.2.
- `AudioTrackMetronomePlayer` body — stays as the Phase 2 stub until 6.2.
- Persistence, `MetronomePreferences`, DataStore integration → Phase 6.2.
- `TapTempoCalculator` → Phase 6.2 (it pairs naturally with the player and its clock abstraction).
- `MetronomeViewModel`, `MetronomeUiState` enrichment, use cases → Phase 6.3.
- All UI work → Phase 6.4.

## Reading Order Before Starting

1. `APP_SPECIFICATION.md` → "Module: Metronome"
2. `DESIGN.md` → §8.2 (Metronome)
3. `DECISIONS.md` → all entries
4. `Phase6-Metronome-Decisions.md` (the planning decision log) → Items 1, 3, 7, 8, 15, 21
5. This file

## Decisions Locked In For 6.1

These are settled before implementation begins (full rationale in `Phase6-Metronome-Decisions.md`):

- ✅ **Audio source:** Synthesized in code, not bundled assets (Item 1).
- ✅ **Sample rate / format:** 48000 Hz, mono, 16-bit PCM (Item 15).
- ✅ **Waveform / envelope:** Sine wave; 1 ms linear attack; exponential decay over remaining duration; 30 ms total per click (Item 21).
- ✅ **Click parameters:** Frequencies 1500 / 1000 / 800 Hz, peak amplitudes 0.70 / 0.50 / 0.25, all named constants (Item 21).
- ✅ **Tempo boundaries:** Adagio 1–75, Andante 76–107, Moderato 108–119, Allegro 120–167, Presto 168–300 (Item 3).
- ✅ **Subdivision multipliers:** NONE=1, EIGHTHS=2, SIXTEENTHS=4, TRIPLETS=3 (Item 8).
- ✅ **Click kind selection:** Beat index 0 → ACCENTED; non-zero multiple of subdivision multiplier → STANDARD; otherwise → SUBDIVISION (Item 8).
- ✅ **Time signature semantics:** Denominator dictates beat unit (/4 = quarter, /8 = eighth). Numerator dictates segment count and main-beat count (Item 7).

## Implementation Details

### `metronome/data/audio/MetronomeAudioFormat.kt`

A single object holding the audio format constants. These are referenced by both `ClickSynthesizer` (output rate) and `AudioTrackMetronomePlayer` (in 6.2) for `AudioTrack` configuration.

```kotlin
internal object MetronomeAudioFormat {
    const val SAMPLE_RATE_HZ = 48_000
    const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
    const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
    const val BYTES_PER_SAMPLE = 2
}
```

The Android `AudioFormat` constants are referenced directly. This is the only Android dependency in 6.1 — and it's compile-only (no runtime behavior).

### `metronome/data/audio/ClickKind.kt`

```kotlin
internal enum class ClickKind {
    ACCENTED,
    STANDARD,
    SUBDIVISION,
}
```

### `metronome/data/audio/ClickParameters.kt`

All synthesis-tunable values live here as named constants. **Treat these as v1 starting values** — adjustments after listening (in 6.2) are expected.

```kotlin
internal object ClickParameters {
    // Universal envelope and timing
    const val CLICK_DURATION_MS = 30
    const val CLICK_ATTACK_MS = 1
    const val CLICK_DECAY_RATE = 160.0

    // Per-kind carrier frequencies
    const val FREQUENCY_HZ_ACCENTED = 1500.0
    const val FREQUENCY_HZ_STANDARD = 1000.0
    const val FREQUENCY_HZ_SUBDIVISION = 800.0

    // Per-kind peak amplitudes (fraction of full-scale)
    const val AMPLITUDE_ACCENTED = 0.70
    const val AMPLITUDE_STANDARD = 0.50
    const val AMPLITUDE_SUBDIVISION = 0.25

    // Reference: 16-bit PCM full-scale
    const val PCM16_FULL_SCALE = 32_767
}
```

### `metronome/data/audio/ClickSynthesizer.kt`

Pure synthesizer. Given a `ClickKind`, produces a `ShortArray` of PCM samples at the configured sample rate. No Android dependencies beyond the format constants object.

```kotlin
internal class ClickSynthesizer {
    /**
     * Generates a PCM buffer for the given click kind, using the parameters in [ClickParameters]
     * and the format in [MetronomeAudioFormat].
     */
    fun generate(kind: ClickKind): ShortArray { ... }
}
```

Algorithm (matches Item 21):

```
totalSamples  = (CLICK_DURATION_MS * SAMPLE_RATE_HZ) / 1000  // = 1440 at 48 kHz
attackSamples = (CLICK_ATTACK_MS  * SAMPLE_RATE_HZ) / 1000  // = 48
freqHz        = frequencyFor(kind)
peak          = amplitudeFor(kind)

for i in 0 until totalSamples:
    envelope =
        if i < attackSamples:
            i.toDouble() / attackSamples
        else:
            exp(-CLICK_DECAY_RATE * (i - attackSamples) / SAMPLE_RATE_HZ)
    val sample = peak * envelope * sin(2 * PI * freqHz * i / SAMPLE_RATE_HZ)
    out[i] = (sample * PCM16_FULL_SCALE).toInt().toShort()
```

`frequencyFor` and `amplitudeFor` are private `when` lookups against `ClickKind`. No magic numbers anywhere in the body — every numeric value resolves to a `ClickParameters` constant.

### `metronome/domain/model/Subdivision.kt` (update existing)

Phase 2 enum: `NONE, EIGHTHS, SIXTEENTHS, TRIPLETS`. Add a `multiplier: Int` property:

```kotlin
enum class Subdivision(val multiplier: Int) {
    NONE(1),
    EIGHTHS(2),
    SIXTEENTHS(4),
    TRIPLETS(3),
}
```

> The Phase 2 enum had no constructor argument. Adding the `multiplier` property is a non-trivial enum change. Record in `DECISIONS.md`.

### `metronome/domain/model/BeatPattern.kt` (new)

Pure helpers used by both the player (6.2) and the UI (6.4). Top-level functions in a single file:

```kotlin
/** Returns the total number of clicks per bar for the given signature numerator and subdivision. */
internal fun clicksPerBar(numerator: Int, subdivision: Subdivision): Int =
    numerator * subdivision.multiplier

/**
 * Returns which [ClickKind] should play at the given click index within a bar,
 * for the given subdivision setting. Click index 0 is the bar's downbeat.
 */
internal fun clickKindFor(clickIndexInBar: Int, subdivision: Subdivision): ClickKind {
    return when {
        clickIndexInBar == 0 -> ClickKind.ACCENTED
        clickIndexInBar % subdivision.multiplier == 0 -> ClickKind.STANDARD
        else -> ClickKind.SUBDIVISION
    }
}
```

### `metronome/domain/model/TempoDescriptor.kt` (new)

The five-label tempo descriptor (Item 3). Either an enum with a `displayKey` referencing a string resource, or a plain `Int -> StringResource` lookup. Recommended: an enum that the UI later resolves to a localized string.

```kotlin
internal enum class TempoDescriptor {
    ADAGIO,
    ANDANTE,
    MODERATO,
    ALLEGRO,
    PRESTO,
}

private const val TEMPO_BOUNDARY_ANDANTE  = 76
private const val TEMPO_BOUNDARY_MODERATO = 108
private const val TEMPO_BOUNDARY_ALLEGRO  = 120
private const val TEMPO_BOUNDARY_PRESTO   = 168

/** Maps a BPM in [1, 300] to its tempo descriptor per Item 3. */
internal fun tempoDescriptorFor(bpm: Int): TempoDescriptor = when {
    bpm < TEMPO_BOUNDARY_ANDANTE  -> TempoDescriptor.ADAGIO
    bpm < TEMPO_BOUNDARY_MODERATO -> TempoDescriptor.ANDANTE
    bpm < TEMPO_BOUNDARY_ALLEGRO  -> TempoDescriptor.MODERATO
    bpm < TEMPO_BOUNDARY_PRESTO   -> TempoDescriptor.ALLEGRO
    else                          -> TempoDescriptor.PRESTO
}
```

String resources (one per enum value) live in `res/values/strings.xml` and are wired by the UI in 6.4 — not 6.1.

## Tests

Tests in 6.1 are **exhaustive**, not token. All tests live under `app/src/test/java/de/ritzelprimpf/toniqo/metronome/`.

### `ClickSynthesizerTest`

- Buffer length equals `(CLICK_DURATION_MS * SAMPLE_RATE_HZ) / 1000` for each kind (== 1440 at 48 kHz).
- No sample exceeds `±PCM16_FULL_SCALE` — no clipping.
- The peak observed sample in `ACCENTED` is greater than in `STANDARD`, which is greater than in `SUBDIVISION`. (Verifies the amplitude hierarchy.)
- The peak observed sample in `ACCENTED` is within ±10% of `AMPLITUDE_ACCENTED * PCM16_FULL_SCALE`. Same kind of check for `STANDARD` and `SUBDIVISION`. (Verifies amplitudes are roughly hit.)
- First sample is exactly 0 (start of attack).
- Last sample magnitude is below 2% of peak (verifies decay envelope reaches near-silence).
- DC offset: the mean of all samples is within `|mean| < PCM16_FULL_SCALE / 1000`. (Loose check; a sine + envelope should average near zero.)
- Deterministic: calling `generate(ACCENTED)` twice produces identical `ShortArray` contents.

### `SubdivisionTest`

- `Subdivision.NONE.multiplier == 1`
- `Subdivision.EIGHTHS.multiplier == 2`
- `Subdivision.SIXTEENTHS.multiplier == 4`
- `Subdivision.TRIPLETS.multiplier == 3`

### `BeatPatternTest`

Exhaustive coverage of `clicksPerBar` and `clickKindFor` for every (signature, subdivision) combination from Item 8.

- `clicksPerBar`:
  - `clicksPerBar(4, NONE) == 4`, `clicksPerBar(4, EIGHTHS) == 8`, `clicksPerBar(4, SIXTEENTHS) == 16`, `clicksPerBar(4, TRIPLETS) == 12`.
  - `clicksPerBar(6, NONE) == 6`, `clicksPerBar(6, EIGHTHS) == 12`, `clicksPerBar(6, SIXTEENTHS) == 24`, `clicksPerBar(6, TRIPLETS) == 18`.
  - At least one assertion per (numerator ∈ {2, 3, 4, 5, 6, 7, 9, 12}, subdivision ∈ all 4 values) combination.
- `clickKindFor` per the rule in Item 8:
  - In 4/4 with NONE: index 0 → ACCENTED, indices 1, 2, 3 → STANDARD.
  - In 4/4 with EIGHTHS: index 0 → ACCENTED, indices 2, 4, 6 → STANDARD, indices 1, 3, 5, 7 → SUBDIVISION.
  - In 4/4 with SIXTEENTHS: index 0 → ACCENTED, indices 4, 8, 12 → STANDARD, all others → SUBDIVISION.
  - In 4/4 with TRIPLETS: index 0 → ACCENTED, indices 3, 6, 9 → STANDARD, indices 1, 2, 4, 5, 7, 8, 10, 11 → SUBDIVISION.
  - **EIGHTHS-in-/8 no-op identity:** in 6/8 with EIGHTHS, the click pattern produced by iterating `clickKindFor` over `clicksPerBar(6, EIGHTHS) = 12` indices, paired with `clicksPerBar(6, NONE) = 6` interpreted at twice the rate, produces the same audible sequence. The test asserts that index 0 → ACCENTED, all other even indices → STANDARD (since `2 % 2 == 0`), all odd indices → SUBDIVISION — capturing the actual function behavior. A comment in the test explains the no-op identity from the spec.

### `TempoDescriptorTest`

- `tempoDescriptorFor(1) == ADAGIO`
- `tempoDescriptorFor(75) == ADAGIO`
- `tempoDescriptorFor(76) == ANDANTE`
- `tempoDescriptorFor(107) == ANDANTE`
- `tempoDescriptorFor(108) == MODERATO`
- `tempoDescriptorFor(119) == MODERATO`
- `tempoDescriptorFor(120) == ALLEGRO`
- `tempoDescriptorFor(167) == ALLEGRO`
- `tempoDescriptorFor(168) == PRESTO`
- `tempoDescriptorFor(300) == PRESTO`
- Each enum value is returned for at least one BPM in [1, 300].

## Steps

1. Update `Subdivision` enum to carry the `multiplier: Int` constructor argument. Record the enum signature change in `DECISIONS.md`.
2. Create `metronome/data/audio/MetronomeAudioFormat.kt` with the format constants.
3. Create `metronome/data/audio/ClickKind.kt`.
4. Create `metronome/data/audio/ClickParameters.kt` with the v1 starting values.
5. Create `metronome/data/audio/ClickSynthesizer.kt` with the full synthesis implementation. Write `ClickSynthesizerTest`.
6. Create `metronome/domain/model/BeatPattern.kt` with `clicksPerBar` and `clickKindFor`. Write `BeatPatternTest`.
7. Create `metronome/domain/model/TempoDescriptor.kt` with the enum, boundary constants, and `tempoDescriptorFor`. Write `TempoDescriptorTest`.
8. Write `SubdivisionTest` for the new `multiplier` property.
9. Update `DECISIONS.md` with the entries listed in `Phase6_1-REQUIREMENTS.md` → "Documentation Updates".
10. Hand off to the user with a summary.

## Completion Criteria

See `Phase6_1-REQUIREMENTS.md`.
