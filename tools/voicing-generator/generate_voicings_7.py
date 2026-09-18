"""
generate_voicings_7.py — Standard-7 chord voicing candidate generator for Toniqo.

THROWAWAY DEV TOOL.  Not shipped with the Android app.  Not part of the
Android build graph.  Run once on a workstation, curate the output by hand,
commit the curated JSON as the (future) 7-string Chord Finder asset.

All tuning-agnostic search logic lives in voicing_core.py (shared with every other driver in
this directory). This file owns only what's specific to standard 7-string tuning: the open
pitch classes, the 4 triad qualities, the search-window constants, and the CLI/JSON-writing
wrapper -- the exact same split of responsibility as generate_voicings.py (standard 6-string),
just one string lower.

Reference tuning: B1 E2 A2 D3 G3 B3 E4 -- standard 6-string tuning (E A D G B E) with a low B
added a perfect fourth below the low E. This is the near-universal "standard 7-string" tuning
(Ibanez UV7, Meshuggah, Dream Theater, etc.), as distinct from a 7-string *drop* tuning (e.g.
Drop A, where that low string is detuned a further whole step) -- which would need its own
curated library the same way Drop D needs its own relative to standard 6-string, and is not
included here; see the module docstring in generate_voicings_drop_d.py for why that pairing
exists and add a generate_voicings_drop_a_7.py analogously if/when that's wanted.

Why this file exists separately from generate_voicings.py rather than a --strings flag on it:
the two libraries are keyed to different reference tunings entirely (see
app/.../common/model/GuitarTuning.kt) and, per the project's existing pattern, each reference
tuning gets its own driver so its tuning- and quality-specific constants stay easy to read and
change without touching the other library's tuned parameters.

Low-string guarantee: on a 7-string instrument, the entire point of the extra string is the
extended low range -- but the plain fretboard search's own preferences (lowest base fret first,
fewest awkward mutes) will happily satisfy every triad using only the top 6 strings, exactly
like the 6-string library, and never touch the low B at all. voicing_core.generate_voicings()'s
guarantee_string_sounds=0 parameter (index 0 = the low B, lowest string in the low→high frets
array) fixes that: for every chord, if none of the voicings the normal search would have picked
happens to sound the low B, the best already-playable candidate that does gets appended as one
extra shape. Nothing about playability is relaxed to make this happen -- the appended shape is
drawn from the exact same fully-filtered candidate pool as everything else, so if the low B
genuinely can't be used playably for a given chord, no extra shape is forced in. This is
enforced in the shared core (not duplicated here) because the mechanism itself -- "make sure a
specific string is represented, if a valid candidate exists" -- has nothing 7-string-specific
about it; only the choice to invoke it with string index 0 belongs to this driver.

How to curate: same process as generate_voicings.py (see its docstring) -- inspect for
unplayable finger counts, redundant near-identical fingerings, and accidental inversions, then
commit the result. One extra thing to check here specifically: for each chord, is the appended
low-B voicing (if present) actually a good one, or does it look tacked-on? The generator picks
the lowest-base-fret playable candidate that uses the string, not the "best-sounding" one by any
musical judgment -- that's exactly the kind of call curation exists for.
"""

from __future__ import annotations

import json
import argparse

import voicing_core

# ---------------------------------------------------------------------------
# Constants — every tunable value is named here; no magic numbers inline.
# ---------------------------------------------------------------------------

# Fretboard search window (matches the standard 6-string driver's own default).
MAX_FRET: int = 15

# Maximum spread across the fretted region: max_fretted_fret − min_fretted_fret. Open strings
# (fret 0) are excluded. Same value as the standard 6-string driver -- adding a 7th string
# doesn't change how far a hand can stretch across the strings it does fret.
MAX_SPAN: int = 5

# A voicing must have at least this many sounding strings, out of 7. Same floor as the 6-string
# driver's MIN_SOUNDED=4 (all 3 triad tones plus at least one doubling) -- unchanged by the extra
# string, which only adds another string that MAY sound, not a requirement that more do.
MIN_SOUNDED: int = 4

# Maximum voicings kept per (root, quality) pair after dedup + spread selection, before the
# low-string-guarantee shape (see module docstring) is appended on top.
MAX_PER_CHORD: int = 5

# Minimum fret gap between consecutive selected voicings when spreading across the neck.
SPREAD_MIN_SPACING: int = 3

# Standard 7-string open pitch classes, low B → high e. C = 0 … B = 11.
# Must match a future GuitarTuning.STANDARD_7 in common/model/GuitarTuning.kt exactly, the same
# way STANDARD_6_OPEN_PCS below it is required to match GuitarTuning.STANDARD_6 -- see that
# file's kdoc. Index 0 (the low B) is what GUARANTEE_STRING_INDEX below refers to.
STANDARD_7_OPEN_PCS: list[int] = [11, 4, 9, 2, 7, 11, 4]   # B  E  A  D  G  B  e
STANDARD_7_TUNING_ID: str = "standard_7"

# 0-based index (low→high) of the string every chord should try to represent at least once —
# see the module docstring's "Low-string guarantee" section. Index 0 is the low B, the string
# that doesn't exist on a 6-string guitar at all.
GUARANTEE_STRING_INDEX: int = 0

# Triad interval sets — semitones above root, as relative pitch classes. Same four qualities as
# the standard 6-string driver; a 7-string instrument doesn't change what a major/minor/
# diminished/augmented triad *is*, only how many places it can be voiced.
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
            "Generate standard-7-string guitar chord voicing candidates for Toniqo. Output is "
            "developer-readable; prune unplayable entries by hand before committing as an app "
            "asset (see README.md)."
        )
    )
    parser.add_argument(
        "--out",
        default="voicings_standard_7.json",
        metavar="FILE",
        help="Output file path (default: voicings_standard_7.json)",
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
        help=(
            "Max root-position/inversion voicings kept per (root, quality) pair, before the "
            f"low-string-guarantee shape (default: {MAX_PER_CHORD})"
        ),
    )
    parser.add_argument(
        "--allow-interior-mutes",
        action="store_true",
        help=(
            "Allow muted strings between the lowest and highest sounding string.  "
            "Default off (stricter, fewer but cleaner candidates)."
        ),
    )
    parser.add_argument(
        "--no-guarantee-low-string",
        action="store_true",
        help=(
            "Disable the low-B guarantee shape (see module docstring). Useful for comparing "
            "output with/without it, or if a future curation pass decides it's not wanted."
        ),
    )
    args = parser.parse_args()

    chord_entries: list[dict] = []
    total_skipped = 0
    total_guaranteed = 0

    for root_pc in range(12):
        for quality in QUALITY_ORDER:
            chord_pcs = {(root_pc + interval) % 12 for interval in QUALITY_INTERVALS[quality]}
            voicings, skipped = voicing_core.generate_voicings(
                root_pc=root_pc,
                chord_pcs=chord_pcs,
                open_pcs=STANDARD_7_OPEN_PCS,
                max_fret=args.max_fret,
                max_span=MAX_SPAN,
                min_sounded=MIN_SOUNDED,
                max_per_chord=args.max_per_chord,
                allow_interior_mutes=args.allow_interior_mutes,
                spread_min_spacing=SPREAD_MIN_SPACING,
                guarantee_string_sounds=(
                    None if args.no_guarantee_low_string else GUARANTEE_STRING_INDEX
                ),
            )
            total_skipped += skipped
            if voicings and voicings[-1].frets[GUARANTEE_STRING_INDEX] != "x" and not any(
                v.frets[GUARANTEE_STRING_INDEX] != "x" for v in voicings[:-1]
            ):
                total_guaranteed += 1
            chord_entries.append({
                "rootPitchClass": root_pc,
                "quality": quality,
                "voicings": [voicing_core.voicing_to_dict(v) for v in voicings],
            })

    output = {
        "tuningId": STANDARD_7_TUNING_ID,
        "version": 1,
        "chords": chord_entries,
    }

    with open(args.out, "w", encoding="utf-8") as fh:
        json.dump(output, fh, indent=2)
        fh.write("\n")  # trailing newline — cleaner diffs after hand-edits

    total_voicings = sum(len(e["voicings"]) for e in chord_entries)
    print(f"Wrote {args.out}")
    print(f"  {len(chord_entries)} chord entries  ({total_voicings} voicings total)")
    if not args.no_guarantee_low_string:
        print(f"  {total_guaranteed} chord(s) needed an appended low-B shape to represent string 0")
        missing = [
            e for e in chord_entries
            if not any(v["frets"][GUARANTEE_STRING_INDEX] != "x" for v in e["voicings"])
        ]
        if missing:
            print(
                f"  WARNING: {len(missing)} chord(s) have NO voicing sounding the low B at all "
                "(no playable candidate existed) — inspect and hand-craft one if desired:",
            )
            for e in missing:
                print(f"    root={e['rootPitchClass']} {e['quality']}")
    if total_skipped:
        print(
            f"  WARNING: {total_skipped} candidate(s) skipped by self-check — "
            "this is a generator bug, investigate before curating."
        )


if __name__ == "__main__":
    main()
