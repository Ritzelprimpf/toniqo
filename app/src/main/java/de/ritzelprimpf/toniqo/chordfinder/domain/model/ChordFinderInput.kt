package de.ritzelprimpf.toniqo.chordfinder.domain.model

import de.ritzelprimpf.toniqo.common.model.ScaleType

/**
 * The user-supplied query for the diatonic chord engine.
 *
 * Works with all 14 [ScaleType]s (the 7 diatonic modes plus the harmonic- and melodic-minor
 * families). This supersedes the Phase 2 stub that was limited to the 7 [de.ritzelprimpf.toniqo.common.model.Mode]s.
 *
 * @property rootPitchClass Pitch class of the scale root: 0 (C) through 11 (B).
 * @property scaleType The scale type to harmonise.
 * @property includeSeventhChords When `true`, returned chords include their diatonic seventh;
 *   when `false`, they are plain triads.
 */
data class ChordFinderInput(
    val rootPitchClass: Int,
    val scaleType: ScaleType,
    val includeSeventhChords: Boolean,
)
