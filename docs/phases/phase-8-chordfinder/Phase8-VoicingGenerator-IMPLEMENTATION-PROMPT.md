# Implementation Prompt — Chord Voicing Generator (Python, dev tool)

> A standalone, throwaway developer tool that produces the curated-JSON **input** for Phase 8.2. It is **not** part of the Android app, is never shipped, and has no Android/Kotlin dependencies. Run it on a workstation to emit candidate voicings; a human then prunes the output before it becomes the app asset.

---

You are writing a **Python 3 command-line script** that generates guitar **chord-voicing candidates** for Toniqo and writes them as developer-readable **JSON**, in the exact schema Phase 8.2's loader consumes. You write code; the user runs it. Do not assume any network or third-party packages — **standard library only** (`json`, `itertools`, `argparse`, `dataclasses`).

**Before writing code, read:**
1. `Phase8-PLAN.md` → "Stage 2 — Voicing Resolution" (scope, root-position rule, invariants).
2. `Phase8_2-PLAN.md` → the **JSON schema**, the `Voicing` invariants, and "Curated data input (from the Python generator)".
3. `FUTURE_PLANS.md` → FP-3 (this script is the first draft of the future runtime generator — keep tuning a parameter).

**What it must produce.** A single JSON file (default `voicings_standard_6.json`) matching exactly:
```json
{
  "tuningId": "standard_6",
  "version": 1,
  "chords": [
    { "rootPitchClass": 0, "quality": "MAJOR",
      "voicings": [
        { "frets": ["x",3,2,0,1,0], "fingers": [0,3,2,0,1,0], "barre": null },
        { "frets": [8,10,10,9,8,8], "fingers": [1,3,4,2,1,1], "barre": {"fret":8,"from":0,"to":5} }
      ] }
  ]
}
```
- `frets`: per string low→high — `"x"` muted, `0` (or `"o"`) open, integer = fretted.
- `fingers`: per string — `0` for open/muted, `1..4` for fretted.
- `barre`: `null`, or `{fret, from, to}` with **0-based** string indices low→high.
- Coverage: all **12 roots × {MAJOR, MINOR, DIMINISHED, AUGMENTED}**. Sort `chords` by `rootPitchClass` then a fixed quality order; pretty-print (`indent=2`) for hand-editing.

**Algorithm (keep it simple, deterministic, and tunable):**
- Standard tuning open pitch classes, low→high: `E A D G B E` = `[4, 9, 2, 7, 11, 4]` (C=0 … B=11). Keep the tuning as a **parameter** (list of open pitch classes) so the same code can target other tunings later; v1 only runs standard.
- Chord pitch classes from root + quality intervals: MAJOR `{0,4,7}`, MINOR `{0,3,7}`, DIMINISHED `{0,3,6}`, AUGMENTED `{0,4,8}` (mod 12, relative to the root pitch class).
- For each string, candidate actions = `muted` plus every fret in `0..MAX_FRET` whose resulting pitch class is in the chord. Enumerate the 6-string cartesian product.
- **Keep** a combination only if: the lowest **sounding** string's pitch class is the **root** (root-position); **all** chord tones appear across sounding strings; fret span (max−min over fretted, ignoring open) ≤ `MAX_SPAN` (4); at least `MIN_SOUNDED` (4) strings sound; and (recommended) no awkward interior single-string mutes between sounded strings beyond a small allowance — expose this as a flag so the human can relax it.
- **Finger assignment (heuristic, OK to be imperfect):** open/muted → 0; otherwise assign `1..4` by ascending fret then ascending string. If the lowest fretted fret is shared by ≥2 strings and notes exist on higher frets, emit a **barre** at that fret spanning the lowest→highest string that uses it, and give those strings finger 1.
- **Dedupe** identical `frets` tuples; **group/sort** voicings by base fret; keep at most `MAX_PER_CHORD` (e.g. 6) spread across the neck (prefer distinct positions).
- **Self-check before emitting:** assert each voicing satisfies notes-⊆-chord and root-in-bass, so the script never writes an invalid candidate (this complements 8.2's library validation test). Skip + count anything that fails rather than writing garbage.

**Quality of the script itself:**
- Module docstring stating it is a throwaway dev tool, not shipped, and the first draft of FP-3.
- Named constants for `MAX_FRET`, `MAX_SPAN`, `MIN_SOUNDED`, `MAX_PER_CHORD`, the open-string pitch classes, and the quality interval sets. No magic numbers inline.
- `dataclass`es for the internal voicing representation; pure functions; a `main()` with `argparse` (`--out`, `--max-fret`, `--max-per-chord`, `--allow-interior-mutes`).
- Comments explaining each heuristic so the developer can tweak before curating.
- Deterministic output (stable sorting) so re-runs diff cleanly.
- A short module-level `# How to curate:` note explaining that the developer removes unplayable/awkward voicings from the JSON by hand, and that 8.2's validation test will reject anything malformed.

**When done:**
- Provide the full script (e.g. `tools/voicing-generator/generate_voicings.py`) and a one-paragraph README on running it and feeding the output into Phase 8.2.
- Run nothing yourself; tell the user the exact command to produce the JSON.

Confirm you have read the schema in `Phase8_2-PLAN.md` and have no blocking questions, then write the script.
