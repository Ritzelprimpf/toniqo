package de.ritzelprimpf.toniqo.keyfinder.domain.model

import de.ritzelprimpf.toniqo.common.model.Note

/**
 * The user-supplied query for the Key Finder: a set of notes plus an optional tonic.
 *
 * The notes are unordered (hence `Set`) — only pitch identity matters for matching, not the order
 * in which the user entered them. Enharmonic equivalents are treated as the same pitch class by
 * the matching logic; the original spelling is preserved at the UI level only.
 *
 * @property notes The collected notes to match against the 84 candidate diatonic scales.
 * @property tonic The user's chosen tonic, or `null` if no tonic was specified. When non-null,
 *   scales rooted on the tonic rank higher in the result list.
 */
data class KeyFinderInput(
    val notes: Set<Note>,
    val tonic: Note?,
)
