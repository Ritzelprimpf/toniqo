"""
generate_voicings.py — Chord voicing candidate generator for Toniqo.

THROWAWAY DEV TOOL.  Not shipped with the Android app.  Not part of the
Android build graph.  Run once on a workstation, curate the output by hand,
commit the curated JSON as the Phase 8.2 asset.

Relationship to FP-3 (runtime tuning-adaptive generator):
  This script is the first draft of the FP-3 runtime engine.  The parts
  that are KEPT when FP-3 is promoted:
    - candidate_frets_for_string()   — tuning-agnostic note search per string
    - the cartesian-product enumeration in generate_voicings()
    - the invariant filter (passes_filters + self_check_voicing)
  The parts that are THROWAWAY (replaced by a smarter runtime engine):
    - the offline CLI wrapper (main / argparse)
    - assign_fingers_and_barre()     — rough heuristic; FP-3 needs a proper
                                       playability scorer
    - select_voicings()              — hard cap + basic spacing; FP-3 needs
                                       ranked scoring by difficulty/aesthetics

# How to curate:
#
# 1. Run the script (see README.md) to produce voicings_standard_6.json.
# 2. Open the file; remove any voicing that is awkward or unplayable:
#      - Shapes that require more than 4 distinct finger positions (including barre)
#      - Finger collisions (two fingers at the same fret on adjacent strings
#        that can't both be pressed cleanly)
#      - Duplicate grips that are enharmonically the same as another entry
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
from dataclasses import dataclass
from itertools import product
from typing import Optional

# ---------------------------------------------------------------------------
# Constants — every tunable value is named here; no magic numbers inline.
# ---------------------------------------------------------------------------

# Fretboard search window (matches Phase 8.2's per-diagram display cap).
MAX_FRET: int = 15

# Maximum spread across the fretted region: max_fretted_fret − min_fretted_fret.
# Open strings (fret 0) are excluded from the span calculation.
MAX_SPAN: int = 6

# Maximum number of fingers a voicing may require.  Barre counts as 1 finger
# (index); each distinct fretted position above the barre needs one more.
# Voicings that would need finger 5+ are mechanically unplayable and dropped.
MAX_FINGERS: int = 4

# A voicing must have at least this many sounding strings.
# 4 guarantees all three triad tones are covered with at least one doubling.
MIN_SOUNDED: int = 4

# Maximum voicings kept per (root, quality) pair after dedup + spread selection.
MAX_PER_CHORD: int = 6

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
# Internal model
# ---------------------------------------------------------------------------

@dataclass(frozen=True)
class BarreSpec:
    """
    A barre: the index finger presses a contiguous string range at one fret.

    from_string / to_string are 0-based string indices, low string = 0.
    The barre spans from_string through to_string inclusive (the range where
    the index finger physically lies; other fingers cover higher frets).
    """
    fret: int
    from_string: int
    to_string: int


@dataclass(frozen=True)
class VoicingCandidate:
    """
    Immutable internal representation of one candidate voicing.

    frets     — one entry per string, low→high.
                "x" = muted; int 0 = open; int ≥ 1 = fretted.
    fingers   — parallel to frets.  0 = open/muted; 1–4 = fretting finger.
    barre     — barre specification, or None.
    base_fret — lowest occupied fret (min over fretted > 0).
                0 only when every sounding string is open (extremely rare).
    """
    frets: tuple          # ("x" | int, …)  len = string_count
    fingers: tuple        # (int, …)         len = string_count
    barre: Optional[BarreSpec]
    base_fret: int


# ---------------------------------------------------------------------------
# Sort key helper — needed because frets tuples mix str ("x") and int.
# ---------------------------------------------------------------------------

def _fret_sort_key(f: object) -> int:
    """Numeric sort key for a single fret value: 'x' sorts after all frets."""
    return 999 if f == "x" else int(f)  # type: ignore[arg-type]


def _voicing_sort_key(v: VoicingCandidate) -> tuple:
    """Primary: ascending base_fret.  Secondary: frets tuple for stable tie-break."""
    return (v.base_fret, tuple(_fret_sort_key(f) for f in v.frets))


# ---------------------------------------------------------------------------
# Core search — FP-3 kernel (keep these tuning-agnostic)
# ---------------------------------------------------------------------------

def candidate_frets_for_string(
    open_pc: int,
    chord_pcs: set[int],
    max_fret: int,
) -> list:
    """
    All candidate actions for one string: ["x"] (muted) plus every fret in
    0..max_fret whose resulting pitch class is in chord_pcs.

    Keeping the search per-string and tuning-parameterised means the same
    function works for any tuning (the FP-3 promise).
    """
    candidates: list = ["x"]
    for fret in range(0, max_fret + 1):
        if (open_pc + fret) % 12 in chord_pcs:
            candidates.append(fret)
    return candidates


def passes_filters(
    combo: tuple,
    open_pcs: list[int],
    chord_pcs: set[int],
    root_pc: int,
    min_sounded: int,
    max_span: int,
    allow_interior_mutes: bool,
) -> bool:
    """
    Returns True only when combo satisfies all five required invariants.

    1. At least min_sounded strings sound.
    2. Root-position: lowest sounding string carries root_pc.
    3. Coverage: all chord tones appear across sounding strings.
    4. Span: max_fretted − min_fretted (open strings excluded) ≤ max_span.
    5. No interior mutes between lowest and highest sounding string
       (unless allow_interior_mutes — relaxed mode for wider exploration).
    """
    sounded_indices = [i for i, f in enumerate(combo) if f != "x"]

    # 1 — minimum sounded strings
    if len(sounded_indices) < min_sounded:
        return False

    # 2 — root in bass (Phase 8.2 v1 invariant: root-position only)
    bass_idx = sounded_indices[0]
    if (open_pcs[bass_idx] + combo[bass_idx]) % 12 != root_pc:
        return False

    # 3 — all chord tones present
    sounded_pcs = {(open_pcs[i] + combo[i]) % 12 for i in sounded_indices}
    if not chord_pcs.issubset(sounded_pcs):
        return False

    # 4 — fret span (open strings at fret 0 don't constrain left-hand reach)
    fretted_values = [combo[i] for i in sounded_indices if combo[i] > 0]
    if len(fretted_values) >= 2:
        if max(fretted_values) - min(fretted_values) > max_span:
            return False

    # 5 — interior mutes: every string between lowest and highest sounded must sound.
    #     Most practical voicings have no interior mutes; this rejects e.g. x-3-x-0-1-0.
    #     Use --allow-interior-mutes to explore uncommon but technically valid shapes.
    if not allow_interior_mutes:
        lo, hi = sounded_indices[0], sounded_indices[-1]
        for i in range(lo + 1, hi):
            if combo[i] == "x":
                return False

    return True


def self_check_voicing(
    frets: tuple,
    open_pcs: list[int],
    chord_pcs: set[int],
    root_pc: int,
) -> bool:
    """
    Hard assertion executed before any voicing is written to output.

    This is a redundant check on top of passes_filters.  If it ever returns
    False the candidate is skipped and counted so the developer knows the
    generator has a bug — the script never silently emits an invalid voicing.
    """
    sounded = [i for i, f in enumerate(frets) if f != "x"]
    if not sounded:
        return False
    # Notes ⊆ chord
    sounded_pcs = {(open_pcs[i] + frets[i]) % 12 for i in sounded}
    if not chord_pcs.issubset(sounded_pcs):
        return False
    # Root in bass
    return (open_pcs[sounded[0]] + frets[sounded[0]]) % 12 == root_pc


# ---------------------------------------------------------------------------
# Finger / barre assignment (heuristic — intentionally imperfect)
# ---------------------------------------------------------------------------

def assign_fingers_and_barre(
    frets: tuple,
) -> tuple[list[int], Optional[BarreSpec]]:
    """
    Heuristic finger assignment.  Imperfect by design — a human curates output.

    Barre heuristic:
      If the lowest fretted fret is shared by ≥ 2 strings AND strings at
      higher frets also exist, treat it as a barre at that fret.
      The barre (finger 1) spans from the lowest to the highest string that
      sits at the barre fret.  Remaining fretted strings receive fingers 2–4
      in ascending (fret, string-index) order.

    No-barre case:
      Assign fingers 1–4 in ascending (fret, string-index) order.

    The returned finger list may contain values > MAX_FINGERS for shapes that
    are mechanically unplayable; the caller filters those out before keeping
    the candidate.
    """
    fretted = [(i, f) for i, f in enumerate(frets) if f != "x" and f > 0]
    fingers = [0] * len(frets)

    if not fretted:
        return fingers, None

    min_fret = min(f for _, f in fretted)
    at_min = [i for i, f in fretted if f == min_fret]
    above_min = [(i, f) for i, f in fretted if f > min_fret]

    barre: Optional[BarreSpec] = None

    if len(at_min) >= 2 and above_min:
        # Barre detected: index finger at min_fret across the required string span
        barre = BarreSpec(
            fret=min_fret,
            from_string=min(at_min),
            to_string=max(at_min),
        )
        for string_idx in at_min:
            fingers[string_idx] = 1     # barre = finger 1
        # Non-barre fretted strings: fingers 2, 3, 4 by ascending fret then string
        for finger_num, (string_idx, _) in enumerate(
            sorted(above_min, key=lambda x: (x[1], x[0])), start=2
        ):
            fingers[string_idx] = finger_num
    else:
        # No barre — assign 1, 2, 3, 4 by ascending fret then string
        for finger_num, (string_idx, _) in enumerate(
            sorted(fretted, key=lambda x: (x[1], x[0])), start=1
        ):
            fingers[string_idx] = finger_num

    return fingers, barre


# ---------------------------------------------------------------------------
# Dedup + spread selection
# ---------------------------------------------------------------------------

def select_voicings(
    candidates: list[VoicingCandidate],
    max_count: int,
    min_spacing: int,
) -> list[VoicingCandidate]:
    """
    Picks at most max_count voicings from a sorted (ascending base_fret),
    deduplicated candidate list, preferring distinct neck positions.

    Two-pass strategy:
      Pass 1 — greedy: select a voicing only when its base_fret is at least
               min_spacing frets ahead of the previously selected one.
               The first voicing is always selected regardless of spacing.
      Pass 2 — fill: if fewer than max_count were collected, add unused
               voicings in base_fret order until the cap is reached.

    Final output is re-sorted by base_fret for deterministic JSON diffs.
    """
    if not candidates:
        return []

    selected: list[VoicingCandidate] = []
    last_base = candidates[0].base_fret - min_spacing  # ensures first is always taken

    for v in candidates:
        if len(selected) >= max_count:
            break
        if v.base_fret - last_base >= min_spacing:
            selected.append(v)
            last_base = v.base_fret

    # Fill remaining slots without spacing constraint
    if len(selected) < max_count:
        taken_frets = {v.frets for v in selected}
        for v in candidates:
            if len(selected) >= max_count:
                break
            if v.frets not in taken_frets:
                selected.append(v)
                taken_frets.add(v.frets)

    selected.sort(key=_voicing_sort_key)
    return selected


# ---------------------------------------------------------------------------
# Top-level generation for one (root, quality) pair
# ---------------------------------------------------------------------------

def generate_voicings(
    root_pc: int,
    quality: str,
    open_pcs: list[int],
    max_fret: int,
    max_span: int,
    min_sounded: int,
    max_per_chord: int,
    allow_interior_mutes: bool,
) -> tuple[list[VoicingCandidate], int]:
    """
    Returns (voicings, skipped_count).

    skipped_count > 0 means self_check_voicing caught an invariant violation
    that passes_filters should have blocked — indicates a generator bug.
    """
    chord_pcs = {(root_pc + interval) % 12 for interval in QUALITY_INTERVALS[quality]}

    # Build per-string candidate lists; order mirrors the low→high JSON frets array.
    per_string = [
        candidate_frets_for_string(pc, chord_pcs, max_fret)
        for pc in open_pcs
    ]

    seen: set[tuple] = set()
    raw: list[VoicingCandidate] = []
    skipped = 0

    for combo in product(*per_string):
        if not passes_filters(
            combo, open_pcs, chord_pcs, root_pc,
            min_sounded, max_span, allow_interior_mutes,
        ):
            continue
        if combo in seen:
            continue
        seen.add(combo)

        # Self-check — should never trigger if passes_filters is correct
        if not self_check_voicing(combo, open_pcs, chord_pcs, root_pc):
            skipped += 1
            continue

        fingers_list, barre = assign_fingers_and_barre(combo)

        # Drop voicings that require more than MAX_FINGERS (mechanically unplayable).
        # max(fingers_list) is the highest finger number assigned; > MAX_FINGERS means
        # the shape needs a 5th finger or more.
        if max(fingers_list) > MAX_FINGERS:
            continue

        fretted_values = [f for f in combo if f != "x" and f > 0]
        base_fret = min(fretted_values) if fretted_values else 0

        raw.append(VoicingCandidate(
            frets=combo,
            fingers=tuple(fingers_list),
            barre=barre,
            base_fret=base_fret,
        ))

    raw.sort(key=_voicing_sort_key)
    selected = select_voicings(raw, max_per_chord, SPREAD_MIN_SPACING)
    return selected, skipped


# ---------------------------------------------------------------------------
# JSON serialisation
# ---------------------------------------------------------------------------

def voicing_to_dict(v: VoicingCandidate) -> dict:
    """
    Converts a VoicingCandidate to the exact JSON object Phase 8.2 expects.

    frets: "x" stays "x"; 0 stays 0 (open); positive int stays int.
    barre: None → null; BarreSpec → {"fret": int, "from": int, "to": int}.
    """
    barre_dict: Optional[dict] = None
    if v.barre is not None:
        barre_dict = {
            "fret": v.barre.fret,
            "from": v.barre.from_string,
            "to": v.barre.to_string,
        }
    return {
        "frets": list(v.frets),
        "fingers": list(v.fingers),
        "barre": barre_dict,
    }


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
            voicings, skipped = generate_voicings(
                root_pc=root_pc,
                quality=quality,
                open_pcs=STANDARD_6_OPEN_PCS,
                max_fret=args.max_fret,
                max_span=MAX_SPAN,
                min_sounded=MIN_SOUNDED,
                max_per_chord=args.max_per_chord,
                allow_interior_mutes=args.allow_interior_mutes,
            )
            total_skipped += skipped
            chord_entries.append({
                "rootPitchClass": root_pc,
                "quality": quality,
                "voicings": [voicing_to_dict(v) for v in voicings],
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
