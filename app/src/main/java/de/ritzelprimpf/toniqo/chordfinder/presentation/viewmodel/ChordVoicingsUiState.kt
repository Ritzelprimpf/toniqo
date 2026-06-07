package de.ritzelprimpf.toniqo.chordfinder.presentation.viewmodel

import de.ritzelprimpf.toniqo.chordfinder.domain.model.Voicing

/**
 * Immutable snapshot of everything the Chord Voicings screen needs to render.
 *
 * @property chordName Full chord symbol (e.g. `"Am"`, `"Cmaj7"`). Shown as the screen title.
 * @property noteNames The chord's constituent note names, in root–third–fifth(–seventh) order.
 * @property rootNoteName The note name that should be highlighted with a mint pill; always the
 *   first entry of [noteNames] (root-position only in v1).
 * @property tuningLabel Human-readable tuning description (e.g. `"E Standard"`, `"E♭ Standard"`).
 *   Shown as the read-only tuning indicator on the screen.
 * @property tier Which resolution tier was used; governs whether the "shown for standard tuning"
 *   indicator is visible ([VoicingTier.UNSUPPORTED]) and whether the offset is meaningful
 *   ([VoicingTier.UNIFORM_OFFSET]).
 * @property voicings The resolved voicings to display, ordered by ascending base fret.
 * @property offsetSemitones The uniform semitone offset used for a [VoicingTier.UNIFORM_OFFSET]
 *   result (negative = tuned down from standard). `null` for other tiers.
 * @property isLoading `true` while the voicing lookup is in progress (initial state).
 */
data class ChordVoicingsUiState(
    val chordName: String = "",
    val noteNames: List<String> = emptyList(),
    val rootNoteName: String = "",
    val tuningLabel: String = "",
    val tier: VoicingTier = VoicingTier.STANDARD,
    val voicings: List<Voicing> = emptyList(),
    val offsetSemitones: Int? = null,
    val isLoading: Boolean = true,
)
