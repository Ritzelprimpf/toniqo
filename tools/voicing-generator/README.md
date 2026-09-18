# Voicing Generator

Throwaway dev tool that produces curated-JSON input for the app's chord voicing assets.
Requires Python 3.9+, no third-party packages.

Six driver scripts share one search engine (`voicing_core.py`):

| Driver | Reference tuning | Qualities | Reads | Output asset |
|---|---|---|---|---|
| `generate_voicings.py` | Standard 6-string (E2 A2 D3 G3 B3 E4) | MAJOR, MINOR, DIMINISHED, AUGMENTED | (fretboard search) | `voicings_standard_6.json` |
| `generate_voicings_drop_d.py` | Drop D (D2 A2 D3 G3 B3 E4) | MAJOR, MINOR, DIMINISHED, AUGMENTED, POWER | (fretboard search) | `voicings_drop_d_6.json` |
| `generate_voicings_7.py` | Standard 7-string (B1 E2 A2 D3 G3 B3 E4) | MAJOR, MINOR, DIMINISHED, AUGMENTED | (fretboard search) | `voicings_standard_7.json` |
| `generate_seventh_voicings.py` | Standard 6-string | 7 `SeventhQuality` values (see below) | curated `voicings_standard_6.json` | `voicings_standard_6_seventh.json` |
| `generate_seventh_voicings_drop_d.py` | Drop D | 7 `SeventhQuality` values | curated `voicings_drop_d_6.json` | `voicings_drop_d_6_seventh.json` |
| `generate_seventh_voicings_7.py` | Standard 7-string | 7 `SeventhQuality` values | curated `voicings_standard_7.json` | `voicings_standard_7_seventh.json` |

`voicing_core.py` is not run directly — it's the shared, tuning-agnostic algorithm (search,
invariant filters, canonicalization, dominance pruning, finger/barre assignment, spread
selection, seventh-chord mutation, and the low-string guarantee described below) all six drivers
import. A bug fix there (e.g. the barre-adjacency fix) fixes every library at once; each driver
owns only what's actually tuning/quality-specific (open pitch classes, the quality→interval
table, and the search-window constants).

Every other 6-string drop tuning (Drop C#, Drop C, Drop B, Drop Bb, Drop A, …) is a uniform
semitone offset of Drop D, so the app reaches `voicings_drop_d_6.json` for all of them via the
same fret-shifting tier that already serves Eb/D/C#/C standard from `voicings_standard_6.json`
— see `VoicingRepositoryImpl.kt`. The same would apply to a future `generate_voicings_drop_a_7.py`
(the 7-string equivalent of Drop D — low string down a further whole step) relative to
`voicings_standard_7.json`, if that's ever wanted; it doesn't exist yet, only the standard-7
tuning does.

**7-string is dev-tool-only for now**, exactly like Drop D was when it first landed: `GuitarTuning`
in `common/model/GuitarTuning.kt` has no `STANDARD_7` entry yet, `VoicingRepositoryImpl`'s
`FAMILIES` list doesn't know about a 7-string family, and the Chord Finder UI has no
string-count/tuning picker at all — it's implicitly 6-string throughout. Generating and curating
`voicings_standard_7.json` is useful and safe to do independently of that wiring (this is the
same incremental order Drop D followed), but actually *seeing* 7-string chords in the app needs
that separate, larger follow-up.

## Low-string guarantee (7-string only)

A plain fretboard search has no reason to prefer using the extra low string — the same search
that produces the standard 6-string library's shapes will happily satisfy every 7-string triad
using only the top 6 strings and leave the low B muted throughout, since a shorter, higher-string
shape is just as valid a match. That defeats the point of generating a 7-string library at all.

`generate_voicings_7.py` calls `voicing_core.generate_voicings()` with
`guarantee_string_sounds=0` (string index 0 = the low B). For every chord, if none of the shapes
the normal search would have picked anyway happens to sound the low B, the single best
already-playable candidate that does gets appended as one extra voicing. Nothing about
playability is relaxed to do this — the appended shape is drawn from the exact same
fully-filtered, self-checked candidate pool as everything else the search produces, so "the
chords should still be playable" holds by construction. If no playable candidate can be found for
a given chord, nothing is forced in and the console output says so explicitly (see below) — a
soft, best-effort "if possible," not an unconditional promise.

Running `generate_voicings_7.py` as-is (48 chords: 12 roots × 4 triad qualities) found that 47 of
48 chords already had a low-B voicing among the normal top picks, and the guarantee mechanism
needed to append one for exactly 1 chord — every chord ends up with at least one shape using the
low B. Pass `--no-guarantee-low-string` to compare output with the mechanism disabled.

## Run — triads (from-scratch fretboard search)

```bash
cd tools/voicing-generator
python3 generate_voicings.py --out voicings_standard_6.json
python3 generate_voicings_drop_d.py --out voicings_drop_d_6.json
python3 generate_voicings_7.py --out voicings_standard_7.json
```

Optional flags on all three: `--max-fret N` (default 15), `--max-per-chord N` (default 5),
`--allow-interior-mutes` (widens the search to include muted strings between
sounding ones — more candidates, more curation needed). `generate_voicings_7.py` additionally
accepts `--no-guarantee-low-string` (see "Low-string guarantee" above).

The drop-D driver deliberately searches a tighter window than the standard one (fret span ≤3
vs ≤5, capped at 3-4 sounding strings for triads / 2-3 for power chords) — these are meant to
be compact, movable riffing shapes, not the standard library's fuller open-position voicings.
It also skips the inversion pass (see its docstring for why). The 7-string driver otherwise uses
the same search window as the 6-string standard driver (fret span ≤5, 4+ sounding strings,
inversion pass included) — adding a string doesn't change how far a hand can stretch, only how
many strings there are to choose from.

## Run — seventh chords (derived from your curated triads)

Unlike the triad drivers, the seventh-chord drivers never search the fretboard from scratch.
They read the **curated** triad asset under `app/src/main/assets/chordfinder/` and, for each
triad shape already approved there, try to derive a seventh-chord shape by mutating exactly one
already-sounded, doubled-tone string into the seventh — see `mutate_add_seventh()` in
`voicing_core.py`. Every seventh voicing is therefore anchored to a fingering already curated;
running this again after re-curating the triad file regenerates matching sevenths for free. A
triad shape with no doubled tone to sacrifice yields no derivative for that shape and is listed
in the console output at the end, not silently dropped.

```bash
cd tools/voicing-generator
python3 generate_seventh_voicings.py --out voicings_standard_6_seventh.json
python3 generate_seventh_voicings_drop_d.py --out voicings_drop_d_6_seventh.json
python3 generate_seventh_voicings_7.py --in voicings_standard_7.json --out voicings_standard_7_seventh.json
```

`mutate_add_seventh()` never touches the bass string, so a 7-string triad shape that sounds the
low B in the bass keeps sounding it there in the derived seventh-chord shape too — the low-string
guarantee survives seventh-chord derivation automatically, with no extra logic needed.

Both accept `--in FILE` to point at a different curated triad source (defaults to the real app
asset two directories up) and `--max-fret N` (default 15, applied to the mutated string only).

Which `SeventhQuality` values are generated per triad quality mirrors
`ChordQualityResolver.seventh()` in the Kotlin app exactly:

- MAJOR → `MAJOR_SEVENTH`, `DOMINANT_SEVENTH`
- MINOR → `MINOR_SEVENTH`, `MINOR_MAJOR_SEVENTH`
- DIMINISHED → `HALF_DIMINISHED`, `DIMINISHED_SEVENTH`
- AUGMENTED → `AUGMENTED_MAJOR_SEVENTH`
- POWER is skipped entirely (no third, not seventh-chord-eligible)

## Curate and commit

Open the generated file and delete any voicing that is awkward, unplayable (more than 4
distinct finger positions including barre), or duplicates a better shape already in the same
chord block. You may add hand-crafted shapes directly. When satisfied, copy the file to
`app/src/main/assets/chordfinder/<same filename>`; `VoicingLibraryValidationTest` (standard) /
an equivalent test (drop, once added) will reject any malformed entry on the next test run.

For the seventh-chord files specifically, also check the console output's warning sections —
chord entries with zero derived voicings, and source triad shapes that had no eligible mutation
— before deciding whether a gap needs a hand-crafted shape or is acceptable as-is.

Until `voicings_drop_d_6.json` is curated and placed under `assets/chordfinder/`, the app's
`VoicingRepositoryImpl` treats every Drop-D-family tuning as "matched family, zero curated
voicings" rather than crashing — Drop D chords simply show an empty voicing list until the
asset ships. The same applies independently to each `_seventh.json` asset: a chord whose
seventh-chord asset is missing or has no entry for that key just shows no seventh-chord
voicings, without affecting its plain-triad lookup.

**7-string is further behind than Drop D on the wiring side, but less than it might look**: even
once `voicings_standard_7.json` / `voicings_standard_7_seventh.json` are curated, there's
currently nowhere in the app for them to go — no `GuitarTuning.STANDARD_7`, no `FAMILIES` entry
in `VoicingRepositoryImpl`, and no way for the Chord Finder UI to select a 7-string instrument at
all. The good news: both `Voicing.kt` (domain model — `marks`/`fingers` length is derived from
`openNotes.size`, not hardcoded to 6, by design, per its kdoc) and
`ui/components/FretboardDiagram.kt` (the Canvas that draws the diagram — every measurement is
already `model.stringCount`-relative, not a fixed 6) are already string-count-agnostic. The
remaining wiring is narrower than a full rewrite: add the `GuitarTuning` constant, register the
family in `VoicingRepositoryImpl`, give the Chord Finder screen a way to pick a 7-string tuning,
and write a `VoicingLibraryValidationTest` analog for it (the existing one hardcodes
`GuitarTuning.STANDARD_6` and asserts `marks.size == 6` specifically, so it validates the
standard-6 asset only — a 7-string version needs its own copy with `stringCount == 7`, same
structure). That's still a separate task from curating this library and is not done here.
