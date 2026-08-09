"""
generate_voicings_drop_d.py — Drop-D-family chord voicing candidate generator for Toniqo.

THROWAWAY DEV TOOL, same status as generate_voicings.py.  Not shipped with the Android app.
Run once on a workstation, curate the output by hand, commit the curated JSON as
assets/chordfinder/voicings_drop_d_6.json.

Separate file from generate_voicings.py by design (not just a CLI flag on it): Drop D is a
different reference tuning with a different intent -- compact, movable shapes for riffing,
including a chord quality (POWER) the standard library doesn't have at all -- and keeping it
in its own driver keeps that intent easy to read and change without touching the standard
library's tuned parameters. All tuning-agnostic search logic (the actual algorithm, including
the barre-adjacency fix and near-duplicate filtering) lives in voicing_core.py and is shared,
so a bug fixed there is fixed for both drivers at once.

Why this library looks different from the standard one:
  - Quality set adds POWER (root + fifth, no third -- see ChordQuality.kt). Power chords are
    the default drop-tuning riff shape; a triad's third is often deliberately omitted low on
    the neck because it makes the chord sound muddy through distortion.
  - MAX_SPAN is tighter (3, not the standard library's 5) and MIN/MAX_SOUNDED cap voicings at
    3-4 strings for triads / 2-3 for power chords: these are meant to be compact, movable grips
    playable low on a drop-tuned neck, not the standard library's fuller open-position shapes.
  - No inversion pass: a "power chord inversion" (fifth in the bass) is a niche shape the
    product didn't ask for here, and disabling it keeps this driver scoped to exactly what was
    requested -- 3-note chords and power chords.

Every 6-string drop tuning that keeps standard tuning above the low string (Drop C#, Drop C,
Drop B, Drop Bb, Drop A, ...) is a uniform semitone offset of Drop D, so this one curated
library covers all of them via the app's existing fret-shifting tier -- see
VoicingRepositoryImpl.kt.

How to curate: same process as generate_voicings.py (see its docstring) -- inspect for
unplayable finger counts, redundant near-identical fingerings, and accidental inversions, then
commit the result.
"""

from __future__ import annotations

import json
import argparse

import voicing_core

# ---------------------------------------------------------------------------
# Constants — every tunable value is named here; no magic numbers inline.
# ---------------------------------------------------------------------------

# Fretboard search window. Same ceiling as the standard library -- no reason a drop-tuning
# shape search should stop earlier than the app's own per-diagram display cap.
MAX_FRET: int = 15

# Maximum spread across the fretted region. Tighter than the standard library's 5: these are
# meant to be compact, movable grips, not the fuller open-position shapes searched for there.
MAX_SPAN: int = 3

# Per-quality sounded-string bounds. Triads: 3-4 strings (the 3 chord tones, optionally one
# doubled). POWER: 2-3 strings (root+fifth, optionally the root doubled an octave up -- the
# classic "beefed up" power chord).
QUALITY_MIN_SOUNDED: dict[str, int] = {
    "MAJOR": 3, "MINOR": 3, "DIMINISHED": 3, "AUGMENTED": 3, "POWER": 2,
}
QUALITY_MAX_SOUNDED: dict[str, int] = {
    "MAJOR": 4, "MINOR": 4, "DIMINISHED": 4, "AUGMENTED": 4, "POWER": 3,
}

# Maximum voicings kept per (root, quality) pair after dedup + spread selection.
MAX_PER_CHORD: int = 5

# Minimum fret gap between consecutive selected voicings when spreading across the neck.
# Tighter than the standard library's 3: MAX_SPAN=3 already keeps the search window narrow, so
# a 3-fret spacing requirement would starve the spread pass of candidates to pick from.
SPREAD_MIN_SPACING: int = 2

# Drop D 6-string open pitch classes, low→high. Must match GuitarTuning.DROP_D_6 in
# common/model/GuitarTuning.kt exactly -- that's the reference tuning this library is keyed to.
DROP_D_6_OPEN_PCS: list[int] = [2, 9, 2, 7, 11, 4]   # D  A  D  G  B  e
DROP_D_6_TUNING_ID: str = "drop_d_6"

# Chord-quality interval sets — semitones above root, as relative pitch classes.
QUALITY_INTERVALS: dict[str, set[int]] = {
    "MAJOR":      {0, 4, 7},
    "MINOR":      {0, 3, 7},
    "DIMINISHED": {0, 3, 6},
    "AUGMENTED":  {0, 4, 8},
    "POWER":      {0, 7},
}

# Canonical quality order within each root group in the output JSON.
QUALITY_ORDER: list[str] = ["MAJOR", "MINOR", "DIMINISHED", "AUGMENTED", "POWER"]


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

def main() -> None:
    parser = argparse.ArgumentParser(
        description=(
            "Generate drop-D-family chord voicing candidates for Toniqo. Output is "
            "developer-readable; prune unplayable entries by hand before committing as "
            "assets/chordfinder/voicings_drop_d_6.json."
        )
    )
    parser.add_argument(
        "--out",
        default="voicings_drop_d_6.json",
        metavar="FILE",
        help="Output file path (default: voicings_drop_d_6.json)",
    )
    parser.add_argument(
        "--max-fret",
        type=int,
        default=MAX_FRET,
        metavar="N",
        help=f"Highest fret searched per string (default: {MAX_FRET})",
    )
    parser.add_argument(
        "--max-per-chord",
        type=int,
        default=MAX_PER_CHORD,
        metavar="N",
        help=f"Max voicings kept per (root, quality) pair (default: {MAX_PER_CHORD})",
    )
    parser.add_argument(
        "--allow-interior-mutes",
        action="store_true",
        help=(
            "Allow muted strings between the lowest and highest sounding string.  "
            "Default off (stricter, fewer but cleaner candidates)."
        ),
    )
    args = parser.parse_args()

    chord_entries: list[dict] = []
    total_skipped = 0

    for root_pc in range(12):
        for quality in QUALITY_ORDER:
            chord_pcs = {(root_pc + interval) % 12 for interval in QUALITY_INTERVALS[quality]}
            voicings, skipped = voicing_core.generate_voicings(
                root_pc=root_pc,
                chord_pcs=chord_pcs,
                open_pcs=DROP_D_6_OPEN_PCS,
                max_fret=args.max_fret,
                max_span=MAX_SPAN,
                min_sounded=QUALITY_MIN_SOUNDED[quality],
                max_sounded=QUALITY_MAX_SOUNDED[quality],
                max_per_chord=args.max_per_chord,
                allow_interior_mutes=args.allow_interior_mutes,
                spread_min_spacing=SPREAD_MIN_SPACING,
                include_inversion=False,
            )
            total_skipped += skipped
            chord_entries.append({
                "rootPitchClass": root_pc,
                "quality": quality,
                "voicings": [voicing_core.voicing_to_dict(v) for v in voicings],
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
    print(f"Wrote {args.out}")
    print(f"  {len(chord_entries)} chord entries  ({total_voicings} voicings total)")
    if total_skipped:
        print(
            f"  WARNING: {total_skipped} candidate(s) skipped by self-check — "
            "this is a generator bug, investigate before curating."
        )


if __name__ == "__main__":
    main()
