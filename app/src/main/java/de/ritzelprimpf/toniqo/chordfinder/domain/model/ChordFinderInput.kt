package de.ritzelprimpf.toniqo.chordfinder.domain.model

import de.ritzelprimpf.toniqo.common.model.Mode
import de.ritzelprimpf.toniqo.common.model.Note

/**
 * The user-supplied query for the Chord Finder: a root note and a mode, plus the seventh toggle.
 *
 * @property root The chosen root note. The chord-finder will harmonise the scale built from this
 *   root and [mode].
 * @property mode The chosen mode.
 * @property includeSeventhChords When `true`, returned chords include their diatonic sevenths;
 *   when `false`, they are plain triads.
 */
data class ChordFinderInput(
    val root: Note,
    val mode: Mode,
    val includeSeventhChords: Boolean,
)
