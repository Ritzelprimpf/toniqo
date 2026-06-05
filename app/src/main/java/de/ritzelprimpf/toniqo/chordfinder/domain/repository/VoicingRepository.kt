package de.ritzelprimpf.toniqo.chordfinder.domain.repository

import de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordKey
import de.ritzelprimpf.toniqo.chordfinder.domain.model.Voicing
import de.ritzelprimpf.toniqo.common.model.GuitarTuning

/**
 * The result of looking up voicings for a chord in a given tuning.
 *
 * - [Standard] — tier 1: the tuning is exactly `STANDARD_6`; curated voicings render directly.
 * - [UniformOffset] — tier 2: the tuning is a uniform semitone offset of `STANDARD_6`; curated
 *   voicings have been fret-shifted to preserve sounding pitch.
 * - [Unsupported] — tier 3: the tuning is non-uniform or a different string count; no voicings
 *   are available in v1. FP-3 (runtime generator) will handle this in a future phase.
 */
sealed interface VoicingLookupResult {

    /**
     * Voicings for standard 6-string tuning, rendered directly from the curated library.
     *
     * @property voicings Ordered by ascending [Voicing.baseFret], capped at MAX_VOICINGS.
     */
    data class Standard(val voicings: List<Voicing>) : VoicingLookupResult

    /**
     * Voicings derived from the standard library by shifting each movable voicing up by
     * [offsetSemitones] frets to preserve sounding pitch under a uniformly-detuned instrument.
     * Open voicings are excluded (they cannot be shifted).
     *
     * @property voicings Shifted voicings, ordered by ascending baseFret, capped at MAX_VOICINGS.
     * @property offsetSemitones The magnitude of the uniform offset (negative = tuned down;
     *   the shift applied to frets is `abs(offsetSemitones)` upward).
     */
    data class UniformOffset(
        val voicings: List<Voicing>,
        val offsetSemitones: Int,
    ) : VoicingLookupResult

    /**
     * The requested tuning is not supported by the v1 data model (tier 3: non-uniform offsets,
     * drop tunings, or non-6-string tunings). The voicings screen should inform the user and
     * offer a fallback (e.g. show standard-tuning diagrams with a label).
     *
     * @property tuning The unsupported tuning, for display in the fallback indicator.
     */
    data class Unsupported(val tuning: GuitarTuning) : VoicingLookupResult
}

/**
 * Repository that maps a chord + tuning to the appropriate set of fretboard voicings.
 *
 * The implementation loads the curated JSON asset once and caches it. All subsequent lookups
 * are in-memory. The runtime is therefore deterministic and fully testable.
 */
interface VoicingRepository {

    /**
     * Returns the voicings for [chord] under [tuning].
     *
     * `suspend` because the initial load involves asset I/O. All subsequent calls return the
     * cached result without suspension.
     *
     * @param chord The chord to look up.
     * @param tuning The current guitar tuning.
     * @return A [VoicingLookupResult] describing which tier was used and the resulting voicings.
     */
    suspend fun lookup(chord: ChordKey, tuning: GuitarTuning): VoicingLookupResult
}
