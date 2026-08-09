package de.ritzelprimpf.toniqo.chordfinder.domain.usecase

import de.ritzelprimpf.toniqo.chordfinder.domain.ChordQualityResolver
import de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordFinderInput
import de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordFinderResult
import de.ritzelprimpf.toniqo.chordfinder.domain.model.DegreeChord
import de.ritzelprimpf.toniqo.common.model.ChordQuality
import de.ritzelprimpf.toniqo.common.util.ScaleSpeller
import javax.inject.Inject

/**
 * Pure diatonic chord engine: given a root pitch class, a scale type, and a seventh-chord flag,
 * returns the 7 diatonic chords of that key in scale-degree order (I … VII).
 *
 * The engine works for all 14 [de.ritzelprimpf.toniqo.common.model.ScaleType]s and derives every
 * chord quality purely from the actual scale intervals — no major-scale assumption is made.
 *
 * This is a synchronous pure function; it has no Android dependency, no I/O, and requires no
 * coroutine. Hilt injects it by its `@Inject constructor` — no Hilt module binding is needed.
 */
class FindChordsUseCase @Inject constructor() {

    // ── Constants ─────────────────────────────────────────────────────────────────

    private companion object {
        const val SCALE_SIZE = 7
        const val PITCH_CLASSES = 12

        /** Step distance from root degree to third: every other scale degree. */
        const val THIRD_STEP = 2

        /** Step distance from root degree to fifth: every other scale degree. */
        const val FIFTH_STEP = 4

        /** Step distance from root degree to seventh: every other scale degree. */
        const val SEVENTH_STEP = 6

        /** Roman numeral base strings, index 0 = degree I. */
        val ROMAN_BASES = arrayOf("I", "II", "III", "IV", "V", "VI", "VII")

        // Triad-only chord symbol suffixes (parallel to ChordQuality.symbol for explicitness)
        const val TRIAD_SUFFIX_MAJOR = ""
        const val TRIAD_SUFFIX_MINOR = "m"
        const val TRIAD_SUFFIX_DIMINISHED = "dim"
        const val TRIAD_SUFFIX_AUGMENTED = "aug"
    }

    // ── Entrypoint ────────────────────────────────────────────────────────────────

    /**
     * Returns the 7 diatonic chords of the key described by [input], in degree order I … VII.
     *
     * Algorithm:
     * 1. Compute the 7 pitch classes from [ChordFinderInput.rootPitchClass] + offsets.
     * 2. Spell the 7 notes via [ScaleSpeller.scaleNoteNames].
     * 3. For each degree *i*, derive the triad (and optional seventh) quality purely from the
     *    semitone intervals between stacked scale degrees.
     *
     * @param input The root pitch class, scale type, and seventh-chord flag.
     * @return A [ChordFinderResult] with exactly 7 [DegreeChord]s.
     */
    operator fun invoke(input: ChordFinderInput): ChordFinderResult {
        val intervals = input.scaleType.intervalsFromRoot
        val pitchClasses = IntArray(SCALE_SIZE) { i ->
            (input.rootPitchClass + intervals[i]) % PITCH_CLASSES
        }
        val spelledNames = ScaleSpeller.scaleNoteNames(input.rootPitchClass, input.scaleType)

        val chords = List(SCALE_SIZE) { i ->
            buildDegreeChord(i, pitchClasses, spelledNames, input.includeSeventhChords)
        }
        return ChordFinderResult(chords)
    }

    // ── Private helpers ───────────────────────────────────────────────────────────

    private fun buildDegreeChord(
        degreeIndex: Int,
        pitchClasses: IntArray,
        spelledNames: List<String>,
        includeSevenths: Boolean,
    ): DegreeChord {
        val rootPc = pitchClasses[degreeIndex]
        val thirdIndex = (degreeIndex + THIRD_STEP) % SCALE_SIZE
        val fifthIndex = (degreeIndex + FIFTH_STEP) % SCALE_SIZE
        val seventhIndex = (degreeIndex + SEVENTH_STEP) % SCALE_SIZE

        val thirdInterval = (pitchClasses[thirdIndex] - rootPc + PITCH_CLASSES) % PITCH_CLASSES
        val fifthInterval = (pitchClasses[fifthIndex] - rootPc + PITCH_CLASSES) % PITCH_CLASSES
        val triadQuality = ChordQualityResolver.triad(thirdInterval, fifthInterval)

        val rootName = spelledNames[degreeIndex]

        return if (includeSevenths) {
            val seventhInterval =
                (pitchClasses[seventhIndex] - rootPc + PITCH_CLASSES) % PITCH_CLASSES
            val seventhQuality = ChordQualityResolver.seventh(triadQuality, seventhInterval)
            DegreeChord(
                degree = degreeIndex + 1,
                romanNumeral = buildRomanNumeral(degreeIndex, triadQuality),
                triadQuality = triadQuality,
                seventhQuality = seventhQuality,
                rootName = rootName,
                noteNames = listOf(
                    spelledNames[degreeIndex],
                    spelledNames[thirdIndex],
                    spelledNames[fifthIndex],
                    spelledNames[seventhIndex],
                ),
                symbol = rootName + seventhQuality.suffix,
            )
        } else {
            DegreeChord(
                degree = degreeIndex + 1,
                romanNumeral = buildRomanNumeral(degreeIndex, triadQuality),
                triadQuality = triadQuality,
                seventhQuality = null,
                rootName = rootName,
                noteNames = listOf(
                    spelledNames[degreeIndex],
                    spelledNames[thirdIndex],
                    spelledNames[fifthIndex],
                ),
                symbol = rootName + triadSuffix(triadQuality),
            )
        }
    }

    /** Returns the triad-only chord-symbol suffix for the given quality. */
    private fun triadSuffix(quality: ChordQuality): String = when (quality) {
        ChordQuality.MAJOR -> TRIAD_SUFFIX_MAJOR
        ChordQuality.MINOR -> TRIAD_SUFFIX_MINOR
        ChordQuality.DIMINISHED -> TRIAD_SUFFIX_DIMINISHED
        ChordQuality.AUGMENTED -> TRIAD_SUFFIX_AUGMENTED
        // Unreachable: quality is always ChordQualityResolver.triad()'s return value, which
        // never resolves to POWER (it isn't a diatonic triad — see ChordQuality's kdoc).
        ChordQuality.POWER -> ChordQuality.POWER.symbol
    }

    /**
     * Builds the Roman numeral for a scale degree and triad quality.
     *
     * - MAJOR/AUGMENTED → uppercase base.
     * - MINOR/DIMINISHED → lowercase base.
     * - DIMINISHED → `°` suffix.
     * - AUGMENTED → `+` suffix.
     */
    private fun buildRomanNumeral(degreeIndex: Int, quality: ChordQuality): String {
        val base = ROMAN_BASES[degreeIndex]
        return when (quality) {
            ChordQuality.MAJOR -> base
            ChordQuality.MINOR -> base.lowercase()
            ChordQuality.DIMINISHED -> "${base.lowercase()}°"
            ChordQuality.AUGMENTED -> "${base}+"
            // Unreachable: quality is always ChordQualityResolver.triad()'s return value, which
            // never resolves to POWER (it isn't a diatonic triad — see ChordQuality's kdoc).
            ChordQuality.POWER -> "$base${ChordQuality.POWER.symbol}"
        }
    }
}
