package de.ritzelprimpf.toniqo.metronome.domain.model

/**
 * Descriptive tempo label displayed beneath the BPM number on the metronome screen.
 *
 * The five labels and their BPM boundaries are locked by `Phase6-Metronome-Decisions.md` Item 3.
 * Each enum value maps to a string resource (wired in Phase 6.4). The lookup is performed by
 * [tempoDescriptorFor].
 */
enum class TempoDescriptor {
    /** Very slow: BPM 1–75. */
    ADAGIO,

    /** Walking pace: BPM 76–107. */
    ANDANTE,

    /** Moderate: BPM 108–119. */
    MODERATO,

    /** Fast: BPM 120–167. */
    ALLEGRO,

    /** Very fast: BPM 168–300. */
    PRESTO,
}

private const val TEMPO_BOUNDARY_ANDANTE = 76
private const val TEMPO_BOUNDARY_MODERATO = 108
private const val TEMPO_BOUNDARY_ALLEGRO = 120
private const val TEMPO_BOUNDARY_PRESTO = 168

/**
 * Maps [bpm] to its [TempoDescriptor] label.
 *
 * The lookup is a simple ordered range check using the four named boundaries from
 * `Phase6-Metronome-Decisions.md` Item 3. The function is total over all [Int] inputs:
 * out-of-range BPMs (< 1 or > 300) still resolve to a label; range clamping is a higher-layer
 * concern.
 *
 * @param bpm Beats per minute. Meaningful range: [1, 300].
 * @return The corresponding [TempoDescriptor].
 */
fun tempoDescriptorFor(bpm: Int): TempoDescriptor = when {
    bpm < TEMPO_BOUNDARY_ANDANTE -> TempoDescriptor.ADAGIO
    bpm < TEMPO_BOUNDARY_MODERATO -> TempoDescriptor.ANDANTE
    bpm < TEMPO_BOUNDARY_ALLEGRO -> TempoDescriptor.MODERATO
    bpm < TEMPO_BOUNDARY_PRESTO -> TempoDescriptor.ALLEGRO
    else -> TempoDescriptor.PRESTO
}
