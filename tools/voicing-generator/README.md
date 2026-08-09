# Voicing Generator

Throwaway dev tool that produces curated-JSON input for the app's chord voicing assets.
Requires Python 3.9+, no third-party packages.

Two driver scripts share one search engine (`voicing_core.py`):

| Driver | Reference tuning | Qualities | Output asset |
|---|---|---|---|
| `generate_voicings.py` | Standard 6-string (E2 A2 D3 G3 B3 E4) | MAJOR, MINOR, DIMINISHED, AUGMENTED | `voicings_standard_6.json` |
| `generate_voicings_drop_d.py` | Drop D (D2 A2 D3 G3 B3 E4) | MAJOR, MINOR, DIMINISHED, AUGMENTED, POWER | `voicings_drop_d_6.json` |

`voicing_core.py` is not run directly — it's the shared, tuning-agnostic algorithm (search,
invariant filters, canonicalization, dominance pruning, finger/barre assignment, spread
selection) both drivers import. A bug fix there (e.g. the barre-adjacency fix) fixes both
libraries at once; each driver owns only what's actually tuning/quality-specific (open pitch
classes, the quality→interval table, and the search-window constants).

Every other 6-string drop tuning (Drop C#, Drop C, Drop B, Drop Bb, Drop A, …) is a uniform
semitone offset of Drop D, so the app reaches `voicings_drop_d_6.json` for all of them via the
same fret-shifting tier that already serves Eb/D/C#/C standard from `voicings_standard_6.json`
— see `VoicingRepositoryImpl.kt`.

## Run

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

## Curate and commit

Open the generated file and delete any voicing that is awkward, unplayable (more than 4
distinct finger positions including barre), or duplicates a better shape already in the same
chord block. You may add hand-crafted shapes directly. When satisfied, copy the file to
`app/src/main/assets/chordfinder/<same filename>`; `VoicingLibraryValidationTest` (standard) /
an equivalent test (drop, once added) will reject any malformed entry on the next test run.

Until `voicings_drop_d_6.json` is curated and placed under `assets/chordfinder/`, the app's
`VoicingRepositoryImpl` treats every Drop-D-family tuning as "matched family, zero curated
voicings" rather than crashing — Drop D chords simply show an empty voicing list until the
asset ships.
