package de.ritzelprimpf.toniqo.tuner.data

import de.ritzelprimpf.toniqo.common.model.Note
import de.ritzelprimpf.toniqo.tuner.domain.model.TunerCategory
import de.ritzelprimpf.toniqo.tuner.domain.model.TunerCategory.DROPPED
import de.ritzelprimpf.toniqo.tuner.domain.model.TunerCategory.OPEN
import de.ritzelprimpf.toniqo.tuner.domain.model.TunerCategory.STANDARD
import de.ritzelprimpf.toniqo.tuner.domain.model.TunerPreset

/**
 * Hardcoded tuning-preset catalog for the guitar tuner.
 *
 * Exposes the full catalog as [all] (flat list, display order) and [grouped] (nested by string
 * count and category, ready for the preset picker). Any parse error in a note spec means a typo
 * in this file and is reported immediately via [error] — these are compile-time constants.
 *
 * Coverage: every 6/7/8-string preset listed in `APP_SPECIFICATION.md`, including the
 * modern-metal additions decided on 2026-05-17.
 */
internal object TunerPresets {

    val all: List<TunerPreset> = listOf(

        // ── 6-string Standard ────────────────────────────────────────────────────
        preset("six_string_standard_e",  "E Standard",                        6, STANDARD, "E2 A2 D3 G3 B3 E4"),
        preset("six_string_standard_eb", "Eb Standard (Half Step Down)",      6, STANDARD, "Eb2 Ab2 Db3 Gb3 Bb3 Eb4"),
        preset("six_string_standard_d",  "D Standard (Whole Step Down)",      6, STANDARD, "D2 G2 C3 F3 A3 D4"),
        preset("six_string_standard_cs", "C# Standard",                       6, STANDARD, "C#2 F#2 B2 E3 G#3 C#4"),

        // ── 6-string Open ────────────────────────────────────────────────────────
        preset("six_string_open_d",      "Open D",                            6, OPEN,     "D2 A2 D3 F#3 A3 D4"),
        preset("six_string_open_g",      "Open G",                            6, OPEN,     "D2 G2 D3 G3 B3 D4"),
        preset("six_string_open_e",      "Open E",                            6, OPEN,     "E2 B2 E3 G#3 B3 E4"),
        preset("six_string_open_c",      "Open C",                            6, OPEN,     "C2 G2 C3 G3 C4 E4"),
        preset("six_string_open_a",      "Open A",                            6, OPEN,     "E2 A2 E3 A3 C#4 E4"),
        preset("six_string_open_dadgad", "DADGAD",                            6, OPEN,     "D2 A2 D3 G3 A3 D4"),

        // ── 6-string Dropped ─────────────────────────────────────────────────────
        preset("six_string_drop_d",      "Drop D",                            6, DROPPED,  "D2 A2 D3 G3 B3 E4"),
        preset("six_string_drop_cs",     "Drop C#",                           6, DROPPED,  "C#2 G#2 C#3 F#3 A#3 D#4"),
        preset("six_string_drop_c",      "Drop C",                            6, DROPPED,  "C2 G2 C3 F3 A3 D4"),
        preset("six_string_drop_b",      "Drop B",                            6, DROPPED,  "B1 F#2 B2 E3 G#3 C#4"),
        preset("six_string_drop_bb",     "Drop A#/Bb",                        6, DROPPED,  "A#1 F2 A#2 D#3 G3 C4"),
        preset("six_string_drop_a",      "Drop A",                            6, DROPPED,  "A1 E2 A2 D3 F#3 B3"),

        // ── 7-string Standard ────────────────────────────────────────────────────
        preset("seven_string_standard_b",  "B Standard",                      7, STANDARD, "B1 E2 A2 D3 G3 B3 E4"),
        preset("seven_string_standard_bb", "A#/Bb Standard",                  7, STANDARD, "A#1 D#2 G#2 C#3 F#3 A#3 D#4"),
        preset("seven_string_standard_a",  "A Standard",                      7, STANDARD, "A1 D2 G2 C3 F3 A3 D4"),

        // ── 7-string Open ────────────────────────────────────────────────────────
        preset("seven_string_open_bm", "Open Bm",                             7, OPEN,     "B1 F#2 B2 D3 F#3 B3 D4"),
        preset("seven_string_open_b",  "Open B",                              7, OPEN,     "B1 F#2 B2 D#3 F#3 B3 D#4"),

        // ── 7-string Dropped ─────────────────────────────────────────────────────
        preset("seven_string_drop_a",  "Drop A (7-string)",                   7, DROPPED,  "A1 E2 A2 D3 G3 B3 E4"),
        preset("seven_string_drop_ab", "Drop G#/Ab",                          7, DROPPED,  "G#1 D#2 G#2 C#3 F#3 A#3 D#4"),
        preset("seven_string_drop_g",  "Drop G",                              7, DROPPED,  "G1 D2 G2 C3 F3 A3 D4"),
        preset("seven_string_drop_fs", "Drop F#",                             7, DROPPED,  "F#1 C#2 F#2 B2 E3 G#3 C#4"),

        // ── 8-string Standard ────────────────────────────────────────────────────
        preset("eight_string_standard_fs", "F# Standard",                     8, STANDARD, "F#1 B1 E2 A2 D3 G3 B3 E4"),
        preset("eight_string_standard_f",  "F Standard",                      8, STANDARD, "F1 A#1 D#2 G#2 C#3 F#3 A#3 D#4"),
        preset("eight_string_standard_e",  "E Standard (8-string)",           8, STANDARD, "E1 A1 D2 G2 C3 F3 A3 D4"),

        // ── 8-string Open ────────────────────────────────────────────────────────
        preset("eight_string_open_e", "Open E (8-string)",                    8, OPEN,     "E1 B1 E2 G#2 B2 E3 G#3 B3"),

        // ── 8-string Dropped ─────────────────────────────────────────────────────
        preset("eight_string_drop_e",  "Drop E (8-string)",                   8, DROPPED,  "E1 B1 E2 A2 D3 G3 B3 E4"),
        preset("eight_string_drop_eb", "Drop D#/Eb (8-string)",               8, DROPPED,  "D#1 A#1 D#2 G#2 C#3 F#3 A#3 D#4"),
        preset("eight_string_drop_d",  "Drop D (8-string)",                   8, DROPPED,  "D1 A1 D2 G2 C3 F3 A3 D4"),
        preset("eight_string_drop_cs", "Drop C# (8-string)",                  8, DROPPED,  "C#1 G#1 C#2 F#2 B2 E3 G#3 C#4"),
        preset("eight_string_drop_c",  "Drop C (8-string)",                   8, DROPPED,  "C1 G1 C2 F2 A#2 D#3 G3 C4"),
    )

    /** Catalog pre-grouped for the preset picker: outer key = string count, inner key = category. */
    val grouped: Map<Int, Map<TunerCategory, List<TunerPreset>>> =
        all.groupBy { it.stringCount }
            .mapValues { (_, list) -> list.groupBy { it.category } }

    // ── Private helpers ───────────────────────────────────────────────────────────

    /**
     * Builds a [TunerPreset] from a space-separated note-spec string (e.g. `"E2 A2 D3 G3 B3 E4"`).
     *
     * Crashes with [error] if any token fails to parse — these are compile-time constants and a
     * parse failure always means a typo in this file.
     */
    private fun preset(
        id: String,
        displayName: String,
        stringCount: Int,
        category: TunerCategory,
        notesSpec: String,
    ): TunerPreset {
        val notes = notesSpec.trim().split(" ").map { token ->
            Note.parse(token)
                ?: error("TunerPresets: failed to parse note token '$token' in preset '$id'")
        }
        require(notes.size == stringCount) {
            "TunerPresets: preset '$id' declares $stringCount strings but notes spec has ${notes.size}"
        }
        return TunerPreset(
            id = id,
            displayName = displayName,
            category = category,
            stringCount = stringCount,
            notes = notes,
        )
    }
}
