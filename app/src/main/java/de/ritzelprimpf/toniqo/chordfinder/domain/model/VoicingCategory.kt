package de.ritzelprimpf.toniqo.chordfinder.domain.model

/**
 * Classification of a voicing by its primary hand technique.
 *
 * Derived automatically from the voicing's [FretMark]s and [Barre]:
 * - **OPEN** — contains at least one open string (takes precedence over BARRE).
 * - **BARRE** — no open strings but has a barre.
 * - **SHAPE** — all strings are either fretted or muted; no barre.
 */
enum class VoicingCategory { OPEN, BARRE, SHAPE }
