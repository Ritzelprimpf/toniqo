package de.ritzelprimpf.toniqo.keyfinder.presentation.viewmodel

import de.ritzelprimpf.toniqo.keyfinder.domain.model.ScaleMatch

/**
 * A single note chip displayed in the Key Finder's input rail.
 *
 * Chips are keyed by pitch class — at most one chip per pitch class (0–11). The display name
 * and root flag are pre-computed by the ViewModel so the UI can render with no additional logic.
 *
 * @property pitchClass The pitch class this chip represents (0 = C, 1 = C♯/D♭, …, 11 = B).
 * @property displayName Sharp-spelled pitch-class name shown on the chip (e.g. `"C#"`, `"G"`).
 *   No octave number; the chip represents the pitch class, not a specific octave.
 * @property isRoot `true` when this pitch class is the currently-marked root.
 */
data class NoteChip(
    val pitchClass: Int,
    val displayName: String,
    val isRoot: Boolean,
)

/**
 * Immutable snapshot of all state the Key Finder screen needs to render.
 *
 * Defaults represent the empty/idle state: no notes, no root, mic off, no results.
 *
 * Display strings for result cards (primary labels, subtitles, conventionally-spelled note
 * lists) are **not** stored here — they are derived in Phase 7.4 via
 * [de.ritzelprimpf.toniqo.common.model.ScaleType] resource keys and
 * [de.ritzelprimpf.toniqo.common.util.ScaleSpeller], keeping this class free of `Context`.
 *
 * @property notes Note chips in insertion order, de-duplicated by pitch class. At most one
 *   chip per pitch class; the first-added spelling is kept if the same pitch class is added
 *   again from a different source (dropdown vs. mic).
 * @property rootPitchClass The pitch class marked as the root (0–11), or `null` if none.
 * @property isListening `true` while the microphone is active and routing notes into the list.
 * @property results Ranked [ScaleMatch] list from [de.ritzelprimpf.toniqo.keyfinder.domain.usecase.MatchScalesUseCase].
 *   Empty when fewer than three distinct pitch classes are present (the ≥3 gate in the use case).
 * @property matchCount `results.size`, exposed separately for the "N MATCHES" header in 7.4.
 */
data class KeyFinderUiState(
    val notes: List<NoteChip> = emptyList(),
    val rootPitchClass: Int? = null,
    val isListening: Boolean = false,
    val results: List<ScaleMatch> = emptyList(),
    val matchCount: Int = 0,
)
