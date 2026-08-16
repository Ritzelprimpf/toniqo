package de.ritzelprimpf.toniqo.chordfinder.domain.model

import de.ritzelprimpf.toniqo.common.model.ChordQuality

/**
 * The identity key used to look up voicings in the library.
 *
 * Voicings are keyed by chord identity — never by mode or scale — so each shape is stored once
 * and reused across every mode that contains it (e.g. G major appears in C Ionian, G Mixolydian,
 * D Dorian, etc., but the voicing library has exactly one entry for G MAJOR).
 *
 * [seventhQuality] is `null` for a plain triad and non-null for a seventh chord — the two are
 * distinct keys even at the same root/triad quality (e.g. `ChordKey(0, MAJOR)` for C major is a
 * different library entry from `ChordKey(0, MAJOR, MAJOR_SEVENTH)` for Cmaj7), each backed by
 * its own curated voicing set.
 *
 * @property rootPitchClass The chord root, 0 (C) through 11 (B).
 * @property quality The triad quality.
 * @property seventhQuality The seventh-chord quality, or `null` for a plain triad.
 */
data class ChordKey(
    val rootPitchClass: Int,
    val quality: ChordQuality,
    val seventhQuality: SeventhQuality? = null,
)

private const val PITCH_CLASSES = 12

// Interval values (semitones above root) that count as "a third" / "a fifth", regardless of
// which index they sit at in ChordQuality.intervalsFromRoot. Index-based lookup (e.g. "the third
// is always intervalsFromRoot[1]") breaks for POWER, whose 2-element array has no third at all
// and puts the fifth at index 1, not 2 -- classifying by interval *value* instead is correct for
// every quality, present and future, without the caller needing to know each one's shape.
private val THIRD_INTERVALS = setOf(3, 4) // minor third, major third
private val FIFTH_INTERVALS = setOf(6, 7, 8) // diminished, perfect, augmented fifth

/**
 * Classifies [pitchClass] as the role it plays within this chord: [ChordToneRole.ROOT] if it's
 * the root, [ChordToneRole.THIRD] / [ChordToneRole.FIFTH] if it matches this chord's actual
 * third/fifth (a quality may have neither, e.g. [ChordQuality.POWER] has no third),
 * [ChordToneRole.SEVENTH] if [seventhQuality] is present and it matches the seventh, or
 * [ChordToneRole.OTHER] if it matches none of the above.
 *
 * Used to compute (never just trust) a voicing's [Voicing.bassDegree] from its lowest sounded
 * string, in both [Voicing.validated] and the JSON parser.
 */
internal fun ChordKey.classifyToneRole(pitchClass: Int): ChordToneRole {
    val root = ((rootPitchClass % PITCH_CLASSES) + PITCH_CLASSES) % PITCH_CLASSES
    if (pitchClass == root) return ChordToneRole.ROOT

    val thirdPc = quality.intervalsFromRoot
        .firstOrNull { it in THIRD_INTERVALS }
        ?.let { (rootPitchClass + it + PITCH_CLASSES) % PITCH_CLASSES }
    if (pitchClass == thirdPc) return ChordToneRole.THIRD

    val fifthPc = quality.intervalsFromRoot
        .firstOrNull { it in FIFTH_INTERVALS }
        ?.let { (rootPitchClass + it + PITCH_CLASSES) % PITCH_CLASSES }
    if (pitchClass == fifthPc) return ChordToneRole.FIFTH

    val seventhPc = seventhQuality
        ?.let { (rootPitchClass + it.semitonesFromRoot + PITCH_CLASSES) % PITCH_CLASSES }
    if (pitchClass == seventhPc) return ChordToneRole.SEVENTH

    return ChordToneRole.OTHER
}
