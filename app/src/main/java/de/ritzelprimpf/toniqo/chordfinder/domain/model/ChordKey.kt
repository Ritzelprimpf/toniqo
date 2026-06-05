package de.ritzelprimpf.toniqo.chordfinder.domain.model

import de.ritzelprimpf.toniqo.common.model.ChordQuality

/**
 * The identity key used to look up voicings in the library.
 *
 * Voicings are keyed by chord identity — never by mode or scale — so each shape is stored once
 * and reused across every mode that contains it (e.g. G major appears in C Ionian, G Mixolydian,
 * D Dorian, etc., but the voicing library has exactly one entry for G MAJOR).
 *
 * @property rootPitchClass The chord root, 0 (C) through 11 (B).
 * @property quality The triad quality.
 */
data class ChordKey(
    val rootPitchClass: Int,
    val quality: ChordQuality,
)
