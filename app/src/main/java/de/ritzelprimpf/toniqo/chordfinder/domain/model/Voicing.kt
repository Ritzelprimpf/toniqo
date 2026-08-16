package de.ritzelprimpf.toniqo.chordfinder.domain.model

import de.ritzelprimpf.toniqo.common.model.ChordQuality

/**
 * A single guitar voicing for a chord — everything the fretboard diagram needs to render.
 *
 * [category], [fretRange], and [baseFret] are **derived** properties so they can never drift
 * out of sync with [marks] and [barre].
 *
 * [marks] and [fingers] have variable length equal to the tuning's string count, supporting
 * 6-, 7-, and 8-string instruments from day one (FP-3 seam).
 *
 * @property labelKey Position-based 1-indexed label (1 = first shape in sorted order, etc.).
 * @property marks Fret state per string, lowest string first; size = tuning string count.
 * @property fingers Finger assignment per string (1–4) or 0 for open/muted; size = [marks].size.
 * @property barre Optional barre; if present, [barre].fret must appear in [marks] as a
 *   [FretMark.Fretted] value (or be lower than other fretted marks — the lowest fret in the shape).
 * @property rootStringIndices Indices of sounded strings whose pitch class equals the chord root.
 * @property bassDegree Role of the lowest sounded string's note — [ChordToneRole.ROOT] for a
 *   root-position voicing, [ChordToneRole.THIRD] / [ChordToneRole.FIFTH] for an inversion.
 *   [validated] computes the true value from [marks] and cross-checks it against whatever's
 *   passed in, the same way it does for [rootStringIndices] — never trust a caller-supplied
 *   value to already be correct.
 */
data class Voicing(
    val labelKey: Int,
    val marks: List<FretMark>,
    val fingers: List<Int>,
    val barre: Barre?,
    val rootStringIndices: Set<Int>,
    val bassDegree: ChordToneRole,
) {

    // ── Derived properties ────────────────────────────────────────────────────────

    /**
     * Classification derived from [marks] and [barre]:
     * - OPEN if any mark is [FretMark.Open] (takes precedence over barre).
     * - BARRE if no open strings but a barre is present.
     * - SHAPE otherwise.
     */
    val category: VoicingCategory
        get() = when {
            marks.any { it is FretMark.Open } -> VoicingCategory.OPEN
            barre != null -> VoicingCategory.BARRE
            else -> VoicingCategory.SHAPE
        }

    /**
     * The fret window spanned by this voicing: min..max over all [FretMark.Fretted] marks and
     * the [barre] fret. Returns `0..0` when no fretted marks are present (open-only chord).
     */
    val fretRange: IntRange
        get() {
            val frettedFrets = marks.filterIsInstance<FretMark.Fretted>().map { it.fret }
            val barreFrets = listOfNotNull(barre?.fret)
            val allFrets = frettedFrets + barreFrets
            return if (allFrets.isEmpty()) 0..0 else allFrets.min()..allFrets.max()
        }

    /**
     * The lowest fret position (= [fretRange].first). Used to sort voicings near-nut first.
     */
    val baseFret: Int get() = fretRange.first

    // ── Factory ───────────────────────────────────────────────────────────────────

    companion object {

        private const val MAX_FRET_SPAN = 6

        /** Highest playable fret a curated voicing may use; internal so the data layer can
         * reuse it instead of redeclaring its own copy. */
        internal const val MAX_FRET = 24
        private const val PITCH_CLASSES = 12

        /**
         * Constructs and validates a [Voicing], throwing [IllegalArgumentException] if any
         * invariant is violated.
         *
         * Invariants enforced:
         * 1. `marks.size == fingers.size == tuning.stringCount`.
         * 2. Every sounded string's pitch class ∈ the chord's pitch classes; all chord tones
         *    present across sounded strings.
         * 3. [bassDegree] matches the actual role (root / third / fifth) of the lowest sounded
         *    string's note. Root-position is no longer required — an inversion is valid — but
         *    the claimed degree must be correct; it is never taken on faith. (Invariant 2 already
         *    guarantees the bass note is one of the chord's own tones, never an unrelated pitch.)
         * 4. Fret span ≤ [MAX_FRET_SPAN]; all fretted marks in `1..[MAX_FRET]`.
         * 5. [rootStringIndices] exactly matches sounded strings whose pitch class is the root.
         *
         * @param labelKey Label index.
         * @param marks Per-string fret marks.
         * @param fingers Per-string finger assignments.
         * @param barre Optional barre.
         * @param rootStringIndices Claimed root-string indices.
         * @param bassDegree Claimed bass degree — root-position most of the time, but an
         *   inversion's third/fifth-in-bass is equally valid; see invariant 3.
         * @param chordKey The chord this voicing represents (for validation).
         * @param openNotes Open-string pitch classes, lowest first (same length as [marks]).
         */
        fun validated(
            labelKey: Int,
            marks: List<FretMark>,
            fingers: List<Int>,
            barre: Barre?,
            rootStringIndices: Set<Int>,
            bassDegree: ChordToneRole,
            chordKey: ChordKey,
            openNotes: List<Int>,
        ): Voicing {
            val stringCount = openNotes.size
            require(marks.size == stringCount) {
                "marks.size (${marks.size}) must equal stringCount ($stringCount)"
            }
            require(fingers.size == stringCount) {
                "fingers.size (${fingers.size}) must equal stringCount ($stringCount)"
            }

            // Invariant 4: fret range
            val frettedFrets = marks.filterIsInstance<FretMark.Fretted>().map { it.fret }
            val barreFrets = listOfNotNull(barre?.fret)
            val allFrets = frettedFrets + barreFrets
            if (allFrets.isNotEmpty()) {
                val min = allFrets.min()
                val max = allFrets.max()
                require(min >= 1) { "Fretted marks must be ≥ 1; got $min" }
                require(max <= MAX_FRET) { "Fretted marks must be ≤ $MAX_FRET; got $max" }
                require(max - min <= MAX_FRET_SPAN) {
                    "Fret span ${max - min} exceeds MAX_FRET_SPAN $MAX_FRET_SPAN"
                }
            }

            // Compute pitch classes for each sounded string. When chordKey carries a
            // seventhQuality, the chord has a 4th required tone beyond the triad's own three
            // (or two, for POWER) -- see ChordKey's kdoc for why a seventh chord is a distinct
            // key from its parent triad.
            val chordIntervals = chordKey.quality.intervalsFromRoot.toList() +
                listOfNotNull(chordKey.seventhQuality?.semitonesFromRoot)
            val chordPitchClasses = chordIntervals
                .map { (chordKey.rootPitchClass + it + PITCH_CLASSES) % PITCH_CLASSES }.toSet()
            val soundedPcs = mutableListOf<Pair<Int, Int>>() // (stringIndex, pc)
            for (i in marks.indices) {
                when (val m = marks[i]) {
                    is FretMark.Muted -> {} // not sounded
                    is FretMark.Open -> soundedPcs.add(i to openNotes[i])
                    is FretMark.Fretted -> soundedPcs.add(i to (openNotes[i] + m.fret) % PITCH_CLASSES)
                }
            }

            require(soundedPcs.isNotEmpty()) { "A voicing must sound at least one string" }

            // Invariant 2: sounded notes ⊆ chord tones
            for ((idx, pc) in soundedPcs) {
                require(pc in chordPitchClasses) {
                    "String $idx sounds pc $pc which is not in chord ${chordKey}: $chordPitchClasses"
                }
            }
            // All chord tones must appear across sounded strings
            val soundedPcSet = soundedPcs.map { it.second }.toSet()
            require(soundedPcSet == chordPitchClasses) {
                "Not all chord tones present: required=$chordPitchClasses, sounded=$soundedPcSet"
            }

            // Invariant 3: bassDegree matches the lowest sounded string's actual role. Root
            // position is no longer required -- an inversion (bass = third or fifth) is valid --
            // but whatever degree is claimed must be the truth, computed here rather than trusted.
            val lowestSounded = soundedPcs.minByOrNull { it.first }!!
            val expectedBassDegree = chordKey.classifyToneRole(lowestSounded.second)
            require(bassDegree == expectedBassDegree) {
                "bassDegree $bassDegree does not match the lowest sounded string's actual role " +
                    "$expectedBassDegree (string ${lowestSounded.first}, pc ${lowestSounded.second})"
            }

            // Invariant 5: rootStringIndices
            val expectedRoots = soundedPcs
                .filter { it.second == chordKey.rootPitchClass }
                .map { it.first }.toSet()
            require(rootStringIndices == expectedRoots) {
                "rootStringIndices $rootStringIndices ≠ expected $expectedRoots"
            }

            return Voicing(labelKey, marks, fingers, barre, rootStringIndices, bassDegree)
        }
    }
}
