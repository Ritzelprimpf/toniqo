package de.ritzelprimpf.toniqo.chordfinder.domain

import de.ritzelprimpf.toniqo.chordfinder.domain.model.Barre
import de.ritzelprimpf.toniqo.chordfinder.domain.model.FretMark
import de.ritzelprimpf.toniqo.chordfinder.domain.model.Voicing
import de.ritzelprimpf.toniqo.chordfinder.domain.model.VoicingCategory

/**
 * Tier-2 tuning transform: shifts a movable voicing up by Δ frets to preserve its sounding
 * pitch when played on an instrument tuned down by Δ semitones.
 *
 * This is a pure, stateless utility. It is the documented pure-utility exception to the
 * no-singleton rule (`CLAUDE.md` §4).
 *
 * **Why shift up?** If the instrument is tuned down by Δ (e.g. E♭ standard = Δ −1), every
 * open string sounds Δ semitones lower. To produce the same pitch as a standard-tuning
 * voicing, each fretted note must be played Δ frets higher up the neck.
 *
 * **Open voicings are excluded.** An open string's pitch is fixed by the tuning — it cannot
 * be shifted up. Including open voicings in tier-2 results would sound wrong notes.
 */
object VoicingTransposer {

    private const val MIN_FRET = 1

    /**
     * Shifts all fretted positions in [voicing] up by [deltaFrets] to preserve sounding pitch
     * when the instrument is tuned down by that many semitones.
     *
     * @param voicing The standard-tuning voicing to transpose.
     * @param deltaFrets Positive integer: the number of frets to shift upward. Must be ≥ 0.
     * @param maxFret The maximum fret allowed on the diagram window. Voicings whose shifted
     *   position exceeds this are filtered out.
     * @return The shifted voicing, or `null` if:
     *   - [voicing] is an OPEN-category voicing (has at least one [FretMark.Open] mark), or
     *   - any shifted fret would fall below [MIN_FRET] or above [maxFret].
     */
    fun shift(voicing: Voicing, deltaFrets: Int, maxFret: Int): Voicing? {
        require(deltaFrets >= 0) { "deltaFrets must be ≥ 0; got $deltaFrets" }
        if (voicing.category == VoicingCategory.OPEN) return null

        val shiftedMarks = voicing.marks.map { mark ->
            when (mark) {
                is FretMark.Muted -> mark
                is FretMark.Open -> return null // Open string in a non-OPEN category: defensive guard
                is FretMark.Fretted -> {
                    val newFret = mark.fret + deltaFrets
                    if (newFret > maxFret) return null
                    FretMark.Fretted(newFret)
                }
            }
        }

        val shiftedBarre = voicing.barre?.let { b ->
            val newFret = b.fret + deltaFrets
            if (newFret > maxFret) return null
            Barre(newFret, b.fromString, b.toString)
        }

        return voicing.copy(
            marks = shiftedMarks,
            barre = shiftedBarre,
            // fingers and rootStringIndices are unchanged: same strings, same finger layout
        )
    }
}
