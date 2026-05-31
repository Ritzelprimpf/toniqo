package de.ritzelprimpf.toniqo.keyfinder.domain.model

import de.ritzelprimpf.toniqo.common.model.ScaleType

/**
 * A single scale instance in the Key Finder catalog: a root pitch class paired with a scale type.
 *
 * [pitchClasses] is derived at construction time by applying [type.intervalsFromRoot] to
 * [rootPitchClass] modulo 12, so it is always consistent with the root and type.
 *
 * @property rootPitchClass The root note as a pitch class, 0 (C) through 11 (B).
 * @property type The scale type that defines the interval pattern.
 * @property pitchClasses The set of 7 pitch classes that belong to this scale, including the root.
 */
data class ScaleCandidate(
    val rootPitchClass: Int,
    val type: ScaleType,
) {
    val pitchClasses: Set<Int> = type.intervalsFromRoot
        .map { interval -> (rootPitchClass + interval) % 12 }
        .toSet()
}
