"""
generate_seventh_voicings_drop_d.py — Derives Drop-D-family seventh-chord voicings from the
curated triad library, for Toniqo.

THROWAWAY DEV TOOL, same status as generate_seventh_voicings.py. Not shipped with the Android
app. Run once on a workstation, curate the output by hand, commit the curated JSON as a new app
asset alongside voicings_drop_d_6.json.

Same mutation-based derivation as generate_seventh_voicings.py (see its docstring and
voicing_core.mutate_add_seventh() for the algorithm) — this file owns only what's
tuning-specific: the Drop D open pitch classes and its tighter MAX_SPAN, matching
generate_voicings_drop_d.py's own tuned parameters. POWER triads (root + fifth, no third) are
skipped — not seventh-chord-eligible, same as the standard driver.

## Run

    cd tools/voicing-generator
    python3 generate_seventh_voicings_drop_d.py --out voicings_drop_d_6_seventh.json

## Curate and commit

Same review process as generate_seventh_voicings.py's output. When satisfied, copy the file to
app/src/main/assets/chordfinder/voicings_drop_d_6_seventh.json.
"""

from __future__ import annotations

import argparse
import json

import voicing_core

# ---------------------------------------------------------------------------
# Constants — every tunable value is named here; no magic numbers inline.
# ---------------------------------------------------------------------------

DEFAULT_IN: str = "../../app/src/main/assets/chordfinder/voicings_drop_d_6.json"
DEFAULT_OUT: str = "voicings_drop_d_6_seventh.json"

# Fretboard search window for the mutated string. Matches generate_voicings_drop_d.py's default.
MAX_FRET: int = 15

# Must match generate_voicings_drop_d.py's own MAX_SPAN exactly — tighter than the standard
# library, same rationale: compact, movable grips, not fuller open-position shapes.
MAX_SPAN: int = 3

# Drop D 6-string open pitch classes, low→high. Must match generate_voicings_drop_d.py's own
# DROP_D_6_OPEN_PCS — the same reference tuning the triad library was authored for.
DROP_D_6_OPEN_PCS: list[int] = [2, 9, 2, 7, 11, 4]   # D  A  D  G  B  e
DROP_D_6_TUNING_ID: str = "drop_d_6"

# Triad interval sets — semitones above root. Must match generate_voicings_drop_d.py's own
# QUALITY_INTERVALS (minus POWER, which has no third and is not seventh-chord-eligible).
TRIAD_INTERVALS: dict[str, set[int]] = {
    "MAJOR":      {0, 4, 7},
    "MINOR":      {0, 3, 7},
    "DIMINISHED": {0, 3, 6},
    "AUGMENTED":  {0, 4, 8},
}

# (seventhQuality name, semitone interval above root) per triad quality — mirrors
# ChordQualityResolver.seventh() in the Kotlin app. See generate_seventh_voicings.py's docstring.
TRIAD_TO_SEVENTHS: dict[str, list[tuple[str, int]]] = {
    "MAJOR":      [("MAJOR_SEVENTH", 11), ("DOMINANT_SEVENTH", 10)],
    "MINOR":      [("MINOR_SEVENTH", 10), ("MINOR_MAJOR_SEVENTH", 11)],
    "DIMINISHED": [("HALF_DIMINISHED", 10), ("DIMINISHED_SEVENTH", 9)],
    "AUGMENTED":  [("AUGMENTED_MAJOR_SEVENTH", 11)],
}


def _parse_frets(raw: list) -> tuple:
    """Mirrors the app's VoicingJsonParser frets convention: "x" stays "x", everything else is int."""
    return tuple(f if isinstance(f, int) else "x" for f in raw)


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

def main() -> None:
    parser = argparse.ArgumentParser(
        description=(
            "Derive Drop-D-family seventh-chord voicing candidates from the curated triad "
            "library by mutating existing shapes. Output is developer-readable; curate before "
            "committing as an app asset."
        )
    )
    parser.add_argument(
        "--in", dest="in_path", default=DEFAULT_IN, metavar="FILE",
        help=f"Curated triad JSON to read (default: {DEFAULT_IN})",
    )
    parser.add_argument(
        "--out", default=DEFAULT_OUT, metavar="FILE",
        help=f"Output file path (default: {DEFAULT_OUT})",
    )
    parser.add_argument(
        "--max-fret", type=int, default=MAX_FRET, metavar="N",
        help=f"Highest fret searched on the mutated string (default: {MAX_FRET})",
    )
    args = parser.parse_args()

    with open(args.in_path, "r", encoding="utf-8") as fh:
        triad_data = json.load(fh)

    chord_entries: list[dict] = []
    skipped_shapes: list[tuple] = []
    total_generated = 0

    for entry in triad_data["chords"]:
        root_pc = entry["rootPitchClass"]
        quality = entry["quality"]
        seventh_specs = TRIAD_TO_SEVENTHS.get(quality)
        if seventh_specs is None:
            continue  # POWER -- no third, no sevenths

        triad_pcs = {(root_pc + iv) % 12 for iv in TRIAD_INTERVALS[quality]}

        for seventh_name, interval in seventh_specs:
            seventh_pc = (root_pc + interval) % 12
            chord_pcs = triad_pcs | {seventh_pc}

            mutated: list = []
            for shape_idx, voicing_dict in enumerate(entry["voicings"], start=1):
                frets = _parse_frets(voicing_dict["frets"])
                candidate = voicing_core.mutate_add_seventh(
                    frets=frets,
                    open_pcs=DROP_D_6_OPEN_PCS,
                    root_pc=root_pc,
                    chord_pcs_with_seventh=chord_pcs,
                    seventh_pc=seventh_pc,
                    max_fret=args.max_fret,
                    max_span=MAX_SPAN,
                )
                if candidate is None:
                    skipped_shapes.append((root_pc, quality, seventh_name, shape_idx, voicing_dict["frets"]))
                    continue
                mutated.append(candidate)

            deduped: list = []
            seen: set = set()
            for c in mutated:
                if c.frets in seen:
                    continue
                seen.add(c.frets)
                deduped.append(c)
            deduped = voicing_core.prune_dominated(deduped)
            deduped.sort(key=lambda v: v.base_fret)

            total_generated += len(deduped)
            chord_entries.append({
                "rootPitchClass": root_pc,
                "quality": quality,
                "seventhQuality": seventh_name,
                "voicings": [voicing_core.voicing_to_dict(v) for v in deduped],
            })

    output = {
        "tuningId": DROP_D_6_TUNING_ID,
        "version": 1,
        "chords": chord_entries,
    }

    with open(args.out, "w", encoding="utf-8") as fh:
        json.dump(output, fh, indent=2)
        fh.write("\n")  # trailing newline — cleaner diffs after hand-edits

    total_voicings = sum(len(e["voicings"]) for e in chord_entries)
    print(f"Read {len(triad_data['chords'])} triad chord entries from {args.in_path}")
    print(f"Wrote {args.out}")
    print(f"  {len(chord_entries)} chord entries  ({total_voicings} voicings total)")

    empties = [e for e in chord_entries if not e["voicings"]]
    if empties:
        print(f"  WARNING: {len(empties)} chord entrie(s) have ZERO derived voicings:")
        for e in empties:
            print(f"    root={e['rootPitchClass']} {e['quality']} -> {e['seventhQuality']}")

    if skipped_shapes:
        print(
            f"  {len(skipped_shapes)} source triad shape(s) had no eligible mutation "
            "(no doubled tone to sacrifice):",
        )
        for root_pc, quality, seventh_name, shape_idx, frets in skipped_shapes:
            print(f"    root={root_pc} {quality} -> {seventh_name}: shape #{shape_idx} {frets}")


if __name__ == "__main__":
    main()
