package de.ritzelprimpf.toniqo.tuner.domain.model

import de.ritzelprimpf.toniqo.common.model.Note

/**
 * A named tuning configuration: the ordered list of target notes the guitar's open strings
 * should produce.
 *
 * @property id A stable, machine-readable identifier (e.g. `six_string_standard_e`). Used for
 *   persisting the user's last selection and for analytics; never displayed.
 * @property displayName Human-readable name (e.g. `E Standard`) suitable for the preset picker.
 * @property category The high-level bucket this preset belongs to.
 * @property stringCount The number of strings on the guitar — must match `notes.size`.
 * @property notes The target notes for each string, ordered **lowest to highest** — the order
 *   the tuner cycles through in sequential mode.
 */
data class TunerPreset(
    val id: String,
    val displayName: String,
    val category: TunerCategory,
    val stringCount: Int,
    val notes: List<Note>,
)
