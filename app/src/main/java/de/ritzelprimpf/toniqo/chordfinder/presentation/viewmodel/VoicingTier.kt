package de.ritzelprimpf.toniqo.chordfinder.presentation.viewmodel

/**
 * Which tier of the voicing lookup was used for the current chord + tuning.
 *
 * Mirrors the three-tier model described in `Phase8-PLAN.md` Stage 2:
 *
 * - [STANDARD] — the active tuning is exactly standard 6-string; curated diagrams render
 *   directly with no transformation.
 * - [UNIFORM_OFFSET] — the active tuning is a uniform semitone offset of standard 6-string;
 *   curated diagrams have been fret-shifted to preserve sounding pitch. Open-position voicings
 *   are excluded; [ChordVoicingsUiState.offsetSemitones] carries the shift amount.
 * - [UNSUPPORTED] — the active tuning is non-uniform (e.g. Drop D, DADGAD) or a different
 *   string count; v1 has no runtime generator for these. The screen shows standard-tuning
 *   diagrams with a "shown for standard tuning" indicator.
 */
enum class VoicingTier { STANDARD, UNIFORM_OFFSET, UNSUPPORTED }
