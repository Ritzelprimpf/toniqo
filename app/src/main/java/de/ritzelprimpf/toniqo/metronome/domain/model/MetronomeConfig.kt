package de.ritzelprimpf.toniqo.metronome.domain.model

/**
 * The tempo, meter, and subdivision settings that fully describe how the metronome should play.
 *
 * Defaults match the values in `APP_SPECIFICATION.md` (Metronome > Parameters): 120 BPM in 4/4
 * with no subdivision. The valid BPM range is [BPM_MIN]..[BPM_MAX]; the time-signature numerator
 * and denominator are not validated at the type level (any positive integer is accepted) — the
 * UI restricts the user-facing choices.
 *
 * @property bpm Beats per minute.
 * @property timeSignatureNumerator The top number of the time signature (beats per measure).
 * @property timeSignatureDenominator The bottom number of the time signature (the note value that
 *   gets the beat — 4 for quarter notes, 8 for eighth notes, etc.).
 * @property subdivision The internal subdivision of each beat.
 * @property accentedBeats Zero-based main-beat indices (range `[0, timeSignatureNumerator)`) that
 *   play an [de.ritzelprimpf.toniqo.metronome.domain.model.ClickKind.ACCENTED] click instead of
 *   [de.ritzelprimpf.toniqo.metronome.domain.model.ClickKind.STANDARD]. Subdivision-only clicks
 *   are never affected — this only retargets which *main* beats are accented. Defaults to beat 1
 *   only ([DEFAULT_ACCENTED_BEATS]). Resets to that default whenever the time signature changes
 *   (see `docs/DECISIONS.md`, 2026-10-01 "per-beat accent customization" entry) — a pattern sized
 *   for one bar length does not carry over cleanly to a bar of a different length.
 */
data class MetronomeConfig(
    val bpm: Int = DEFAULT_BPM,
    val timeSignatureNumerator: Int = DEFAULT_TIME_SIGNATURE_NUMERATOR,
    val timeSignatureDenominator: Int = DEFAULT_TIME_SIGNATURE_DENOMINATOR,
    val subdivision: Subdivision = Subdivision.NONE,
    val accentedBeats: Set<Int> = DEFAULT_ACCENTED_BEATS,
) {
    companion object {
        /** Minimum beats per minute the metronome will accept. */
        const val BPM_MIN: Int = 1

        /** Maximum beats per minute the metronome will accept. */
        const val BPM_MAX: Int = 300

        /** Default BPM when the user has not yet chosen a tempo. */
        const val DEFAULT_BPM: Int = 120

        /** Default time-signature numerator (4 — as in 4/4). */
        const val DEFAULT_TIME_SIGNATURE_NUMERATOR: Int = 4

        /** Default time-signature denominator (4 — as in 4/4). */
        const val DEFAULT_TIME_SIGNATURE_DENOMINATOR: Int = 4

        /** Default accent pattern: beat 1 only. Also the fallback whenever a signature change invalidates a custom pattern. */
        val DEFAULT_ACCENTED_BEATS: Set<Int> = setOf(0)

        /**
         * The spec-defined defaults: 120 BPM, 4/4, no subdivision.
         *
         * Used as the first-launch value and as the fallback whenever a persisted config fails
         * validation (see `Phase6-Metronome-Decisions.md` Item 17).
         */
        val DEFAULT: MetronomeConfig = MetronomeConfig(
            bpm = DEFAULT_BPM,
            timeSignatureNumerator = DEFAULT_TIME_SIGNATURE_NUMERATOR,
            timeSignatureDenominator = DEFAULT_TIME_SIGNATURE_DENOMINATOR,
            subdivision = Subdivision.NONE,
            accentedBeats = DEFAULT_ACCENTED_BEATS,
        )

        /**
         * The eight curated time signatures offered as quick picks in the signature dropdown, as
         * (numerator, denominator) pairs. This is a **menu convenience list**, not the full
         * validity rule — see [isSupportedTimeSignature] for what's actually accepted (any custom
         * signature within range, entered via the dropdown's "Custom…" dialog).
         */
        val SUPPORTED_SIGNATURES: Set<Pair<Int, Int>> = setOf(
            2 to 4, 3 to 4, 4 to 4, 5 to 4,
            6 to 8, 7 to 8, 9 to 8, 12 to 8,
        )

        /** Minimum time-signature numerator accepted (including custom, free-form input). */
        const val TIME_SIGNATURE_NUMERATOR_MIN: Int = 1

        /**
         * Maximum time-signature numerator accepted. 32 covers every realistic meter, including
         * the odd/complex ones found in progressive-rock and metal repertoire, while keeping the
         * beat indicator's segment count within what a horizontally-scrollable row of properly
         * sized (44dp, accessible) tap targets can reasonably present.
         */
        const val TIME_SIGNATURE_NUMERATOR_MAX: Int = 32

        /**
         * Denominators accepted for a custom time signature: every power of two from a whole note
         * (1) to a 32nd note (32) — real musical note values, matching
         * [TIME_SIGNATURE_NUMERATOR_MAX]'s "32 covers most" bound. Any other denominator (e.g. 3,
         * 5, 6) doesn't correspond to a real note duration and is rejected.
         */
        val SUPPORTED_DENOMINATORS: Set<Int> = setOf(1, 2, 4, 8, 16, 32)

        /**
         * Returns whether ([numerator], [denominator]) is an acceptable time signature — either
         * one of the curated [SUPPORTED_SIGNATURES] quick picks, or any custom pair within
         * [TIME_SIGNATURE_NUMERATOR_MIN]..[TIME_SIGNATURE_NUMERATOR_MAX] with a denominator in
         * [SUPPORTED_DENOMINATORS]. [SUPPORTED_SIGNATURES] is already a subset of this broader
         * rule, so there is only one validity rule here, not two competing ones.
         */
        fun isSupportedTimeSignature(numerator: Int, denominator: Int): Boolean =
            numerator in TIME_SIGNATURE_NUMERATOR_MIN..TIME_SIGNATURE_NUMERATOR_MAX &&
                denominator in SUPPORTED_DENOMINATORS
    }
}
