package de.ritzelprimpf.toniqo.chordfinder.presentation.ui

import de.ritzelprimpf.toniqo.chordfinder.domain.model.FretMark
import de.ritzelprimpf.toniqo.chordfinder.domain.model.Voicing

/**
 * Presentation model for a [FretboardDiagram]. This is the only type the composable accepts —
 * never a domain [Voicing] directly.
 *
 * All indices are 0-based (string 0 = lowest string). Fret positions within the visible window
 * are 1-indexed: position 1 is the first row below the nut/top edge.
 *
 * @property stringCount Number of strings (6, 7, or 8).
 * @property fretWindow Number of visible fret rows (always [FRET_WINDOW_SIZE]).
 * @property positionLabel Label drawn to the left when the voicing is not at the nut,
 *   e.g. `"3fr"`. `null` when [showNut] is `true`.
 * @property showNut `true` when the chord is at the nut region (`baseFret` ≤ 1);
 *   draws a thick nut bar instead of a thin fret line at the top edge.
 * @property dots Finger-pressed strings; each carries string/fret/finger/root information.
 * @property barre Optional barre spanning a range of strings at one fret within the window.
 * @property openStrings 0-based string indices whose strings are played open (○ above nut).
 * @property mutedStrings 0-based string indices whose strings are muted (× above nut).
 */
data class FretboardRenderModel(
    val stringCount: Int,
    val fretWindow: Int,
    val positionLabel: String?,
    val showNut: Boolean,
    val dots: List<Dot>,
    val barre: BarreSpan?,
    val openStrings: Set<Int>,
    val mutedStrings: Set<Int>,
) {
    /**
     * A single pressed string in the diagram.
     *
     * @property stringIndex 0-based string index (0 = lowest / thickest string).
     * @property fretWithinWindow 1-indexed fret position within the visible window.
     * @property finger Finger number 1–4, or `null` when no annotation is stored.
     * @property isRoot `true` when this string sounds the chord root; rendered in mint.
     */
    data class Dot(
        val stringIndex: Int,
        val fretWithinWindow: Int,
        val finger: Int?,
        val isRoot: Boolean,
    )

    /**
     * A barre: the first finger lays flat across a range of strings at one fret.
     *
     * @property fretWithinWindow 1-indexed fret position within the visible window.
     * @property fromString Lowest (thickest) string covered, 0-based inclusive.
     * @property toString Highest (thinnest) string covered, 0-based inclusive.
     */
    data class BarreSpan(
        val fretWithinWindow: Int,
        val fromString: Int,
        val toString: Int,
    )
}

/** Number of fret rows shown in a single diagram. Matches standard chord-diagram conventions. */
internal const val FRET_WINDOW_SIZE = 5

/**
 * Maps a domain [Voicing] to a [FretboardRenderModel] ready for canvas rendering.
 *
 * ### Windowing
 * The visible window spans [FRET_WINDOW_SIZE] rows starting at a computed base fret. An absolute
 * fret `f` maps to window position `f - base + 1`.
 *
 * - **Barre voicings** always window from [Voicing.baseFret] (the lowest fret in the shape, which
 *   for a valid barre chord is the barre itself) — per product decision, a barre diagram must
 *   start on the fret where the bar is, never nut-anchored.
 * - **Non-barre voicings** (open or fingered shapes) window from the nut (base = 1) whenever the
 *   whole shape fits within one window from there (`fretRange.last <= FRET_WINDOW_SIZE`), matching
 *   the conventional "cowboy chord" diagram convention instead of left-aligning to the lowest
 *   fretted note. Shapes that can't reach the nut fall back to windowing from [Voicing.baseFret].
 *
 * ### Nut vs position label
 * - `base ≤ 1` → [FretboardRenderModel.showNut] = `true`, no position label.
 * - `base > 1` → label `"${base}fr"` shown to the left, no nut drawn.
 */
fun Voicing.toRenderModel(): FretboardRenderModel {
    val base = if (barre == null && fretRange.last <= FRET_WINDOW_SIZE) {
        1
    } else {
        baseFret.coerceAtLeast(1)
    }
    val showNut = base <= 1
    val positionLabel = if (showNut) null else "${base}fr"

    val dots = mutableListOf<FretboardRenderModel.Dot>()
    val openStrings = mutableSetOf<Int>()
    val mutedStrings = mutableSetOf<Int>()

    marks.forEachIndexed { i, mark ->
        when (mark) {
            is FretMark.Open -> openStrings += i
            is FretMark.Muted -> mutedStrings += i
            is FretMark.Fretted -> dots += FretboardRenderModel.Dot(
                stringIndex = i,
                fretWithinWindow = mark.fret - base + 1,
                finger = fingers[i].takeIf { it > 0 },
                isRoot = i in rootStringIndices,
            )
        }
    }

    val barreSpan = barre?.let { b ->
        FretboardRenderModel.BarreSpan(
            fretWithinWindow = b.fret - base + 1,
            fromString = b.fromString,
            toString = b.toString,
        )
    }

    return FretboardRenderModel(
        stringCount = marks.size,
        fretWindow = FRET_WINDOW_SIZE,
        positionLabel = positionLabel,
        showNut = showNut,
        dots = dots,
        barre = barreSpan,
        openStrings = openStrings,
        mutedStrings = mutedStrings,
    )
}
