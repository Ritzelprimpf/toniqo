package de.ritzelprimpf.toniqo.keyfinder.domain.model

/**
 * The reduced, pitch-class form of the user's Key Finder query.
 *
 * Octave information is discarded; enharmonic equivalents collapse to the same pitch class
 * (e.g. C# and Db both become pitch class 1). The caller (ViewModel in Phase 7.3) converts
 * the user's [Note] list into this form before passing it to
 * [de.ritzelprimpf.toniqo.keyfinder.domain.usecase.MatchScalesUseCase].
 *
 * @property pitchClasses Distinct pitch classes contributed by the user's notes, each in 0–11.
 * @property rootPitchClass The pitch class of the note the user marked as the root, or `null`
 *   if no root has been chosen. When non-null, scales rooted on this pitch class receive a
 *   +1 bonus point in the scoring formula.
 */
data class KeyFinderInput(
    val pitchClasses: Set<Int>,
    val rootPitchClass: Int?,
)
