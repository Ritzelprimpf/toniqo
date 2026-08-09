"""
generate_voicings.py — Standard-6 chord voicing candidate generator for Toniqo.

THROWAWAY DEV TOOL.  Not shipped with the Android app.  Not part of the
Android build graph.  Run once on a workstation, curate the output by hand,
commit the curated JSON as the Phase 8.2 asset.

All tuning-agnostic search logic lives in voicing_core.py (shared with
generate_voicings_drop_d.py). This file owns only what's specific to standard
6-string tuning: the open pitch classes, the 4 triad qualities, the search-window
constants, and the CLI/JSON-writing wrapper.

# How to curate:
#
# 1. Run the script (see README.md) to produce voicings_standard_6.json.
#    The generator already removes two kinds of automatic clutter before
#    you see the file:
#      - canonicalize_voicing(): if a string can validly ring open, it does
#        — so you will never see x32010 next to x32013 / x3201x / x32x13 as
#        separate entries; they collapse to the one fullest voicing.
#      - prune_dominated(): a voicing that is identical to another except it
#        mutes one or more strings the other sounds is dropped in favour of
#        the fuller one.
#    What's left after that is genuinely different fingerings — e.g. the
#    same chord fretted starting from a different string (a D-string vs a
#    B-string alternate) or a different neck position — which is exactly
#    the kind of choice curation is for.
# 2. Open the file; remove any voicing that is awkward or unplayable:
#      - Shapes that require more than 4 distinct finger positions (including barre)
#      - Finger collisions (two fingers at the same fret on adjacent strings
#        that can't both be pressed cleanly)
#      - Alternate fingerings you find musically redundant even though they
#        are not structurally dominated (e.g. two similarly-awkward mid-neck
#        options at the same position — keep whichever plays better)
#      - Inversions accidentally included (inspect: does the lowest X show
#        the right root?)
#      - Any shape you personally find unlikely in a real arrangement
# 3. You MAY add hand-crafted voicings the generator missed, e.g. partial-
#    barre shapes.  They will be validated on every test run.
# 4. Phase 8.2's VoicingLibraryValidationTest enforces:
#      - notes ⊆ chord pitch classes
#      - lowest sounding string is the root
#      - marks.size == 6
#      - frets in 0..MAX_FRET
#      - category / fretRange / rootStringIndices self-consistent
#    Malformed hand-edits are caught at that test, not at runtime.
"""

from __future__ import annotations

import json
import argparse

import voicing_core

# ---------------------------------------------------------------------------
# Constants — every tunable value is named here; no magic numbers inline.
# ---------------------------------------------------------------------------

# Fretboard search window (matches Phase 8.2's per-diagram display cap).
MAX_FRET: int = 15

# Maximum spread across the fretted region: max_fretted_fret − min_fretted_fret.
# Open strings (fret 0) are excluded from the span calculation.
# Must match Voicing.kt's MAX_FRET_SPAN (app/.../chordfinder/domain/model/Voicing.kt) exactly --
# that's the value actually enforced at validation time. Flat (not position-scaled) even though a
# 6-fret stretch is physically easier higher up the neck than down at fret 1 -- deliberately kept
# simple; revisit with a position-scaled limit if flat 6 still proves too strict up high or too
# loose down low.
MAX_SPAN: int = 5

# A voicing must have at least this many sounding strings.
# 4 guarantees all three triad tones are covered with at least one doubling.
MIN_SOUNDED: int = 4

# Maximum voicings kept per (root, quality) pair after dedup + spread selection.
# 5, not 4, deliberately leaves the curator a little slack: this is a floor of "at least 4 good
# voicings after hand-curation," and the pre-curation pool needs one spare in case one candidate
# turns out awkward on inspection.
MAX_PER_CHORD: int = 5

# Minimum fret gap between consecutive selected voicings when spreading across
# the neck.  Prevents emitting six shapes all at the same position.
SPREAD_MIN_SPACING: int = 3

# Standard 6-string open pitch classes, low-E → high-e.
# C = 0 … B = 11.  Keeping this as a named parameter (not a hardcoded list in
# the algorithm) makes the search loop tuning-agnostic for FP-3.
STANDARD_6_OPEN_PCS: list[int] = [4, 9, 2, 7, 11, 4]   # E  A  D  G  B  e
STANDARD_6_TUNING_ID: str = "standard_6"

# Triad interval sets — semitones above root, as relative pitch classes.
QUALITY_INTERVALS: dict[str, set[int]] = {
    "MAJOR":      {0, 4, 7},
    "MINOR":      {0, 3, 7},
    "DIMINISHED": {0, 3, 6},
    "AUGMENTED":  {0, 4, 8},
}

# Canonical quality order within each root group in the output JSON.
QUALITY_ORDER: list[str] = ["MAJOR", "MINOR", "DIMINISHED", "AUGMENTED"]


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

def main() -> None:
    parser = argparse.ArgumentParser(
        description=(
            "Generate guitar chord voicing candidates for Toniqo's Phase 8.2 "
            "JSON asset.  Output is developer-readable; prune unplayable entries "
            "by hand before committing as assets/chordfinder/voicings_standard_6.json."
        )
    )
    parser.add_argument(
        "--out",
        default="voicings_standard_6.json",
        metavar="FILE",
        help="Output file path (default: voicings_standard_6.json)",
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
            "Default off (stricter, fewer but cleaner candidates).  Enable to "
            "explore shapes like x-3-x-0-1-0 that are technically valid but rare."
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
                open_pcs=STANDARD_6_OPEN_PCS,
                max_fret=args.max_fret,
                max_span=MAX_SPAN,
                min_sounded=MIN_SOUNDED,
                max_per_chord=args.max_per_chord,
                allow_interior_mutes=args.allow_interior_mutes,
                spread_min_spacing=SPREAD_MIN_SPACING,
            )
            total_skipped += skipped
            chord_entries.append({
                "rootPitchClass": root_pc,
                "quality": quality,
                "voicings": [voicing_core.voicing_to_dict(v) for v in voicings],
            })

    output = {
        "tuningId": STANDARD_6_TUNING_ID,
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
