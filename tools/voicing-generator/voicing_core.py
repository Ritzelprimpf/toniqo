"""
voicing_core.py — Tuning-agnostic chord voicing search engine shared by every driver script
in this directory (generate_voicings.py for standard tuning, generate_voicings_drop_d.py for
the drop-D family, and any future one).

THROWAWAY DEV TOOL, same status as its callers. Not shipped with the Android app.

Relationship to FP-3 (runtime tuning-adaptive generator):
  The parts that are KEPT when FP-3 is promoted:
    - candidate_frets_for_string()   — tuning-agnostic note search per string
    - the cartesian-product enumeration in _search_candidates()
    - the invariant filter (passes_filters + self_check_voicing)
    - canonicalize_voicing()         — collapses free-string open/mute noise;
                                       tuning-agnostic, keep as-is
    - is_dominated() / prune_dominated() — drops strict-subset voicings;
                                       tuning-agnostic, keep as-is
  The parts that are THROWAWAY (replaced by a smarter runtime engine):
    - assign_fingers_and_barre()     — rough heuristic; FP-3 needs a proper
                                       playability scorer
    - select_voicings()              — hard cap + basic spacing; FP-3 needs
                                       ranked scoring by difficulty/aesthetics

A driver script owns everything tuning- and quality-specific: the open-string pitch classes,
the chord-quality interval table, the search-window constants (MAX_FRET, MAX_SPAN, MIN_SOUNDED,
MAX_SOUNDED, MAX_PER_CHORD, SPREAD_MIN_SPACING), and the CLI/JSON-writing wrapper. This module
owns only the search algorithm itself, so a bug fix (e.g. the barre-adjacency fix) only ever
needs to happen in one place.
"""

from __future__ import annotations

from dataclasses import dataclass
from itertools import product
from typing import Optional

# Maximum number of fingers a voicing may require. Barre counts as 1 finger (index); each
# distinct fretted position above the barre needs one more. Voicings that would need finger 5+
# are mechanically unplayable and dropped. Fixed across every tuning -- a hand has 4 fingers
# regardless of what the strings are tuned to -- so this lives here, not in a driver script.
MAX_FINGERS: int = 4


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
    require_root_bass: bool = True,
    max_sounded: Optional[int] = None,
) -> bool:
    """
    Returns True only when combo satisfies all required invariants.

    1. At least min_sounded strings sound, and (when max_sounded is given) at most
       max_sounded strings sound. max_sounded defaults to None (no cap) so callers
       that never pass it keep the old, uncapped behaviour -- it exists for drivers
       that want deliberately compact, minimal-string shapes (e.g. movable drop-tuning
       power chords), not as a universal constraint.
    2. Root-position (only when require_root_bass=True): lowest sounding string carries
       root_pc. When False, the bass may be any chord tone (an inversion) -- per-string
       candidates are already restricted to chord tones only (see
       candidate_frets_for_string), so relaxing this never lets a non-chord-tone bass
       through; it only widens which *chord tone* may sit in the bass.
    3. Coverage: all chord tones appear across sounding strings.
    4. Span: max_fretted − min_fretted (open strings excluded) ≤ max_span.
    5. No interior mutes between lowest and highest sounding string
       (unless allow_interior_mutes — relaxed mode for wider exploration).
    """
    sounded_indices = [i for i, f in enumerate(combo) if f != "x"]

    # 1 — sounded-string count bounds
    if len(sounded_indices) < min_sounded:
        return False
    if max_sounded is not None and len(sounded_indices) > max_sounded:
        return False

    # 2 — root in bass (skipped when searching for inversions)
    if require_root_bass:
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
    require_root_bass: bool = True,
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
    # Root in bass (skipped when checking an inversion candidate)
    if not require_root_bass:
        return True
    return (open_pcs[sounded[0]] + frets[sounded[0]]) % 12 == root_pc


def canonicalize_voicing(
    combo: tuple,
    open_pcs: list,
    chord_pcs: set,
    root_pc: int,
    min_sounded: int,
    max_span: int,
    allow_interior_mutes: bool,
    require_root_bass: bool = True,
    max_sounded: Optional[int] = None,
) -> tuple:
    """
    Collapses combinatorial near-duplicates that differ only in whether an
    optional string rings open (e.g. x32010 / x32013 / x3201x are all the
    same grip; only the free top string differs).

    For every string not already open, try setting it to open (fret 0) --
    but only when its open note is a chord tone AND the resulting voicing
    still passes every invariant (passes_filters is the sole validity
    oracle, reused unchanged). This is a ONE-WAY simplification: a string
    is upgraded toward "open" when it's free to be; it is never muted or
    downgraded here. That direction matters -- muting an already-sounding
    string can silently strip a full, idiomatic voicing (classic open C,
    x32010) down to a sparser one, which is not the canonical form we want.
    Sparser derivatives that are still redundant are instead removed later,
    explicitly, by prune_dominated().
    """
    combo = list(combo)
    for i, value in enumerate(combo):
        if value == 0:
            continue  # already open, nothing to upgrade
        if (open_pcs[i] % 12) not in chord_pcs:
            continue  # open note isn't a chord tone on this string
        trial = combo.copy()
        trial[i] = 0
        if passes_filters(
            tuple(trial), open_pcs, chord_pcs, root_pc,
            min_sounded, max_span, allow_interior_mutes, require_root_bass, max_sounded,
        ):
            combo[i] = 0
    return tuple(combo)


def is_dominated(candidate: "VoicingCandidate", other: "VoicingCandidate") -> bool:
    """
    True if `candidate` is a strict, redundant subset of `other`: every
    string `candidate` sounds, `other` sounds at the identical fret, and
    `other` additionally sounds at least one string `candidate` mutes.

    A dominated voicing is literally the same grip with one or more strings
    deliberately left silent -- it adds no new shape, only a thinner strum
    of a shape already present -- so it is dropped in favour of the fuller
    voicing.
    """
    extra_sounding = False
    for c_fret, o_fret in zip(candidate.frets, other.frets):
        if c_fret == "x":
            if o_fret != "x":
                extra_sounding = True
            continue
        if c_fret != o_fret:
            return False
    return extra_sounding


def prune_dominated(candidates: list) -> list:
    """
    Removes any voicing that is a strict subset of another voicing in the
    same list (see is_dominated). O(n^2) over an already-deduplicated,
    typically small per-chord candidate list -- cheap in practice.
    """
    return [
        c for c in candidates
        if not any(is_dominated(c, other) for other in candidates if other is not c)
    ]


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
      higher frets also exist, a barre is a *candidate* at that fret, spanning
      from the lowest to the highest string that sits at the barre fret.  It's
      only accepted if every string in that span is physically compatible with
      a finger lying flat across it: each one must be either muted, at
      min_fret (barred), or fretted *higher* than min_fret (another finger
      presses harder on top of the barre -- e.g. the classic F shape
      1,3,3,2,1,1, where fingers 2-4 press strings 1-3 above the fret-1
      barre).  Only an *open* string inside the span is physically
      impossible -- the barre finger lying flat across it would necessarily
      fret it -- so that's the sole rejection case; when rejected, every
      fretted string instead gets its own finger (falls through to the
      no-barre case below).
      Accepted barre: barre strings get finger 1; remaining fretted strings
      receive fingers 2–4 in ascending (fret, string-index) order.

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
        lo, hi = min(at_min), max(at_min)
        # Physical validity check: every string between lo and hi (inclusive) must be
        # either muted, at min_fret (barred), or fretted higher (a second finger
        # pressing on top of the barre -- fine). Only an *open* string in that range
        # is impossible -- e.g. frets [8,7,5,0,5,0] has strings 2 and 4 both at fret
        # 5, but string 3 between them rings open, so there is no valid barre there.
        barre_span_is_playable = all(
            frets[i] == "x" or frets[i] >= min_fret
            for i in range(lo, hi + 1)
        )
        if barre_span_is_playable:
            barre = BarreSpec(fret=min_fret, from_string=lo, to_string=hi)
            for string_idx in at_min:
                fingers[string_idx] = 1     # barre = finger 1
            # Non-barre fretted strings: fingers 2, 3, 4 by ascending fret then string
            for finger_num, (string_idx, _) in enumerate(
                sorted(above_min, key=lambda x: (x[1], x[0])), start=2
            ):
                fingers[string_idx] = finger_num

    if barre is None:
        # No (valid) barre — assign 1, 2, 3, 4 by ascending fret then string
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
# Top-level generation for one (root, chord_pcs) pair
# ---------------------------------------------------------------------------

def bass_pc(frets: tuple, open_pcs: list[int]) -> int:
    """Pitch class of the lowest-index (bass) sounded string. Caller guarantees ≥1 sounds."""
    bass_idx = next(i for i, f in enumerate(frets) if f != "x")
    return (open_pcs[bass_idx] + frets[bass_idx]) % 12


def _search_candidates(
    root_pc: int,
    chord_pcs: set[int],
    open_pcs: list[int],
    per_string: list[list],
    min_sounded: int,
    max_span: int,
    allow_interior_mutes: bool,
    require_root_bass: bool,
    max_sounded: Optional[int] = None,
) -> tuple[list["VoicingCandidate"], int]:
    """
    Runs the full combinatorial search once, with root-in-bass either required or relaxed.
    Shared by generate_voicings()'s root-position pass and its inversion pass -- everything
    from raw enumeration through finger/barre assignment is identical between the two; only
    which bass pitch classes are acceptable differs.

    Returns (candidates, skipped_count) -- see generate_voicings() for skipped_count's meaning.
    """
    seen: set[tuple] = set()
    raw: list[VoicingCandidate] = []
    skipped = 0

    for combo in product(*per_string):
        if not passes_filters(
            combo, open_pcs, chord_pcs, root_pc,
            min_sounded, max_span, allow_interior_mutes, require_root_bass, max_sounded,
        ):
            continue

        # Collapse combinatorial near-duplicates before dedup: several raw
        # combos differing only in an optional string's open/mute/alt-fret
        # choice canonicalize to the same tuple.
        canonical = canonicalize_voicing(
            combo, open_pcs, chord_pcs, root_pc,
            min_sounded, max_span, allow_interior_mutes, require_root_bass, max_sounded,
        )
        if canonical in seen:
            continue
        seen.add(canonical)

        # Self-check — should never trigger if passes_filters/canonicalize are correct
        if not self_check_voicing(canonical, open_pcs, chord_pcs, root_pc, require_root_bass):
            skipped += 1
            continue

        fingers_list, barre = assign_fingers_and_barre(canonical)

        # Drop voicings that require more than MAX_FINGERS (mechanically unplayable).
        # max(fingers_list) is the highest finger number assigned; > MAX_FINGERS means
        # the shape needs a 5th finger or more.
        if max(fingers_list) > MAX_FINGERS:
            continue

        fretted_values = [f for f in canonical if f != "x" and f > 0]
        base_fret = min(fretted_values) if fretted_values else 0

        raw.append(VoicingCandidate(
            frets=canonical,
            fingers=tuple(fingers_list),
            barre=barre,
            base_fret=base_fret,
        ))

    raw.sort(key=_voicing_sort_key)
    # Cross-voicing pass: drop any voicing that is a strict, muted-down
    # subset of a fuller voicing already present (see prune_dominated).
    return prune_dominated(raw), skipped


def generate_voicings(
    root_pc: int,
    chord_pcs: set[int],
    open_pcs: list[int],
    max_fret: int,
    max_span: int,
    min_sounded: int,
    max_per_chord: int,
    allow_interior_mutes: bool,
    spread_min_spacing: int,
    max_sounded: Optional[int] = None,
    include_inversion: bool = True,
) -> tuple[list[VoicingCandidate], int]:
    """
    Returns (voicings, skipped_count).

    voicings is up to max_per_chord root-position shapes plus, appended at the end and only
    when include_inversion is True, one inversion (bass = third or fifth) when the search finds
    at least one -- see the "Inversion" section below. skipped_count > 0 means self_check_voicing
    caught an invariant violation that passes_filters should have blocked — indicates a generator
    bug.

    @param chord_pcs The chord's own pitch classes (root already included), e.g. {0, 4, 7} for a
        C major triad or {7, 2} for a G power chord. Callers own their own quality → interval
        table; this function is agnostic to what "quality" even means.
    """
    # Build per-string candidate lists; order mirrors the low→high JSON frets array.
    per_string = [
        candidate_frets_for_string(pc, chord_pcs, max_fret)
        for pc in open_pcs
    ]

    # Root-position search (the default, matching Phase 8.2 v1's original scope).
    root_position_raw, skipped_root = _search_candidates(
        root_pc, chord_pcs, open_pcs, per_string,
        min_sounded, max_span, allow_interior_mutes, require_root_bass=True, max_sounded=max_sounded,
    )
    selected = select_voicings(root_position_raw, max_per_chord, spread_min_spacing)

    if not include_inversion:
        return selected, skipped_root

    # Inversion search: bass = third or fifth. A second, separate pass (rather than just
    # relaxing the main search) keeps the two concerns independent -- the root-position
    # count/spread above is unaffected by whether an inversion happens to exist, and exactly
    # one inversion (not a variable number) is appended, per the product decision that every
    # chord should surface exactly one alternate-bass option alongside its root-position shapes.
    all_bass_raw, skipped_inversion = _search_candidates(
        root_pc, chord_pcs, open_pcs, per_string,
        min_sounded, max_span, allow_interior_mutes, require_root_bass=False, max_sounded=max_sounded,
    )
    inversions = sorted(
        (c for c in all_bass_raw if bass_pc(c.frets, open_pcs) != root_pc),
        key=_voicing_sort_key,
    )
    # Reject any inversion that is really just an already-selected root-position shape with one
    # extra optional string un-muted -- e.g. x32010's low E left ringing becomes 032010, which
    # flips the bass to an inversion but isn't a genuinely different shape, only the exact
    # near-duplicate pattern (same grip, only whether a redundant string rings differs) this
    # generator otherwise works hard to avoid. is_dominated(selected_shape, inversion_candidate)
    # is true precisely in that case: the selected shape is a strict, sparser subset of it.
    chosen_inversion = next(
        (inv for inv in inversions if not any(is_dominated(sel, inv) for sel in selected)),
        None,
    )
    if chosen_inversion is not None:
        selected = selected + [chosen_inversion]

    return selected, skipped_root + skipped_inversion


# ---------------------------------------------------------------------------
# Seventh-chord derivation by mutation (used by generate_seventh_voicings*.py)
# ---------------------------------------------------------------------------

def mutate_add_seventh(
    frets: tuple,
    open_pcs: list[int],
    root_pc: int,
    chord_pcs_with_seventh: set[int],
    seventh_pc: int,
    max_fret: int,
    max_span: int,
) -> Optional[VoicingCandidate]:
    """
    Derives a seventh-chord voicing from a curated triad voicing by mutating exactly one
    currently-sounded, non-bass string whose note is doubled elsewhere in the shape, changing
    its fret so it sounds the seventh instead. Everything else about the shape -- which strings
    sound, the bass note/degree, all other frets -- is left untouched.

    Only a string whose current note already appears on another sounded string is eligible: its
    occurrence is redundant (that chord tone survives via the other string), so repurposing it
    for the seventh never drops a required tone. The bass string is never mutated, so the
    voicing's bass degree (root-position, or an inversion with third/fifth in the bass) carries
    through unchanged into the seventh-chord version.

    Returns the mutation with the smallest fret change from the original (ties broken by lowest
    string index, then lowest fret), or None if no doubled, non-bass string can reach the
    seventh without breaking playability (max span, or needing a 5th finger).
    """
    sounded = [i for i, f in enumerate(frets) if f != "x"]
    if not sounded:
        return None
    bass_idx = sounded[0]

    sounded_pcs = [(open_pcs[i] + frets[i]) % 12 for i in sounded]
    pc_counts: dict[int, int] = {}
    for pc in sounded_pcs:
        pc_counts[pc] = pc_counts.get(pc, 0) + 1

    best: Optional[tuple] = None  # (delta, string_idx, new_fret, candidate)

    for pos, string_idx in enumerate(sounded):
        if string_idx == bass_idx:
            continue
        pc = sounded_pcs[pos]
        if pc_counts[pc] < 2:
            continue  # not a doubled tone -- mutating it would drop a required chord tone

        original_fret = frets[string_idx]
        for new_fret in range(0, max_fret + 1):
            if (open_pcs[string_idx] + new_fret) % 12 != seventh_pc:
                continue

            trial = list(frets)
            trial[string_idx] = new_fret
            trial_tuple = tuple(trial)

            trial_pcs = {(open_pcs[i] + trial[i]) % 12 for i in sounded}
            if trial_pcs != chord_pcs_with_seventh:
                continue  # should be unreachable given the doubled-tone precondition above

            fretted_values = [trial[i] for i in sounded if trial[i] > 0]
            if len(fretted_values) >= 2 and max(fretted_values) - min(fretted_values) > max_span:
                continue

            fingers, barre = assign_fingers_and_barre(trial_tuple)
            if max(fingers) > MAX_FINGERS:
                continue

            delta = abs(new_fret - original_fret)
            base_fret = min(fretted_values) if fretted_values else 0
            candidate = VoicingCandidate(
                frets=trial_tuple, fingers=tuple(fingers), barre=barre, base_fret=base_fret,
            )
            key = (delta, string_idx, new_fret)
            if best is None or key < best[:3]:
                best = (*key, candidate)

    return best[3] if best is not None else None


# ---------------------------------------------------------------------------
# JSON serialisation
# ---------------------------------------------------------------------------

def voicing_to_dict(v: VoicingCandidate) -> dict:
    """
    Converts a VoicingCandidate to the exact JSON object the app's VoicingJsonParser expects.

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
