# Voicing Generator

Throwaway dev tool that produces curated-JSON input for the app's chord voicing assets.
Requires Python 3.9+, no third-party packages.

Four driver scripts share one search engine (`voicing_core.py`):

| Driver | Reference tuning | Qualities | Reads | Output asset |
|---|---|---|---|---|
| `generate_voicings.py` | Standard 6-string (E2 A2 D3 G3 B3 E4) | MAJOR, MINOR, DIMINISHED, AUGMENTED | (fretboard search) | `voicings_standard_6.json` |
| `generate_voicings_drop_d.py` | Drop D (D2 A2 D3 G3 B3 E4) | MAJOR, MINOR, DIMINISHED, AUGMENTED, POWER | (fretboard search) | `voicings_drop_d_6.json` |
| `generate_seventh_voicings.py` | Standard 6-string | 7 `SeventhQuality` values (see below) | curated `voicings_standard_6.json` | `voicings_standard_6_seventh.json` |
| `generate_seventh_voicings_drop_d.py` | Drop D | 7 `SeventhQuality` values | curated `voicings_drop_d_6.json` | `voicings_drop_d_6_seventh.json` |

`voicing_core.py` is not run directly — it's the shared, tuning-agnostic algorithm (search,
invariant filters, canonicalization, dominance pruning, finger/barre assignment, spread
selection, and seventh-chord mutation) all four drivers import. A bug fix there (e.g. the
barre-adjacency fix) fixes every library at once; each driver owns only what's actually
tuning/quality-specific (open pitch classes, the quality→interval table, and the search-window
constants).

Every other 6-string drop tuning (Drop C#, Drop C, Drop B, Drop Bb, Drop A, …) is a uniform
semitone offset of Drop D, so the app reaches `voicings_drop_d_6.json` for all of them via the
same fret-shifting tier that already serves Eb/D/C#/C standard from `voicings_standard_6.json`
— see `VoicingRepositoryImpl.kt`.

## Run — triads (from-scratch fretboard search)

```bash
cd tools/voicing-generator
python3 generate_voicings.py --out voicings_standard_6.json
python3 generate_voicings_drop_d.py --out voicings_drop_d_6.json
```

Optional flags on both: `--max-fret N` (default 15), `--max-per-chord N` (default 5),
`--allow-interior-mutes` (widens the search to include muted strings between
sounding ones — more candidates, more curation needed).

The drop-D driver deliberately searches a tighter window than the standard one (fret span ≤3
vs ≤5, capped at 3-4 sounding strings for triads / 2-3 for power chords) — these are meant to
be compact, movable riffing shapes, not the standard library's fuller open-position voicings.
It also skips the inversion pass (see its docstring for why).

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
```

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
