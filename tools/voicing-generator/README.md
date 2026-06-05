# Voicing Generator

Throwaway dev tool that produces the curated-JSON input for Phase 8.2.
Requires Python 3.9+, no third-party packages.

## Run

```bash
cd tools/voicing-generator
python3 generate_voicings.py --out voicings_standard_6.json
```

Optional flags: `--max-fret N` (default 15), `--max-per-chord N` (default 6),
`--allow-interior-mutes` (widens the search to include muted strings between
sounding ones — more candidates, more curation needed).

## Curate and commit

Open `voicings_standard_6.json` and delete any voicing that is awkward, unplayable
(more than 4 distinct finger positions including barre), or duplicates a better
shape already in the same chord block.  You may add hand-crafted shapes directly.
When satisfied, copy the file to
`app/src/main/assets/chordfinder/voicings_standard_6.json`; Phase 8.2's
`VoicingLibraryValidationTest` will reject any malformed entry on the next test run.
