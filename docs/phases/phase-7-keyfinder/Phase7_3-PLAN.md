# Phase 7.3 — ViewModel & State

## Goal

Wire the engine (7.1) and the detector (7.2) into a `KeyFinderViewModel` that holds the note list, supports adding (dropdown or mic), removing, and marking/unmarking the root, and **re-ranks live** on every change with no action button. This is the presentation-logic layer — no composables yet.

By the end of 7.3 the ViewModel exposes a single `StateFlow<KeyFinderUiState>` that 7.4 renders directly.

## Scope

- `keyfinder/presentation/viewmodel/KeyFinderViewModel.kt`
- `keyfinder/presentation/viewmodel/KeyFinderUiState.kt`
- Note-list management, root marking, mic on/off, live recompute, ≥3-note gate.
- Exhaustive ViewModel unit tests (fakes for `NoteDetector` and the use case).

## Out of Scope

- Composables, chips, result cards, detail view, permission-denied screen → 7.4.
- The matching math (owned by 7.1) and audio detection (owned by 7.2) — consumed, not reimplemented.

## Reading Order Before Starting

1. `Phase7-PLAN.md` → "Scoring Model", "Ranking & Display Rules" (the gate + live-recompute rules)
2. `Phase7_1-PLAN.md` (the `MatchScalesUseCase` / model contracts) and `Phase7_2-PLAN.md` (`NoteDetector`)
3. `CLAUDE.md` §7 (`StateFlow` in ViewModels, sealed state) and §6 (100% ViewModel coverage)
4. This file

## Decisions To Lock At The Start Of 7.3

- [ ] **Note identity.** A user note is a `Note` (name + octave), but matching is by pitch class. Confirm: the list **stores `Note`s** (so the UI can show e.g. "E2" if played) but **de-duplicates by pitch class** for matching, and the visible chips are by pitch class. Recommended: keep the list keyed by pitch class (one chip per pitch class); the first-seen `Note` provides the chip's spelling. Record the choice.
- [ ] **Root removal behaviour.** If the marked-root note is removed, the root simply clears (no auto-reassignment). Confirm.
- [ ] **Adding a duplicate** (same pitch class via dropdown or mic) is a **no-op** (no error, no duplicate chip). Confirm.
- [ ] **Note cap.** A sane maximum (suggested 12 — one per pitch class; beyond that adds nothing). Confirm and make it a constant.
- [ ] **Mic recompute cadence.** Each confirmed note from `NoteDetector` is one add → one recompute. Confirm no extra debounce is needed beyond the detector's own (7.2).

## Implementation Details

### `KeyFinderUiState`

```kotlin
data class KeyFinderUiState(
    val notes: List<NoteChip>,            // ordered as added; de-duped by pitch class
    val rootPitchClass: Int?,             // the marked root, or null
    val isListening: Boolean,             // mic on/off
    val results: List<ScaleMatch>,        // empty when < 3 distinct notes
    val matchCount: Int,                  // results.size (for the "N MATCHES" header)
)

data class NoteChip(
    val pitchClass: Int,
    val displayName: String,              // spelling for the chip
    val isRoot: Boolean,
)
```

> `ScaleMatch` is the 7.1 domain type. The presentation layer derives each card's primary label, subtitle, and spelled notes from `match.candidate` via `ScaleType` resource keys + `ScaleSpeller` at render time (7.4) — `KeyFinderUiState` stays free of `Context`.

### `KeyFinderViewModel`

Constructor-injected with `MatchScalesUseCase` and `NoteDetector`. A single `MutableStateFlow<KeyFinderUiState>` exposed as `StateFlow`.

Intents (public functions called by the UI):

- `addNoteFromPicker(note: Note)` — add by pitch class (no-op if present); recompute.
- `removeNote(pitchClass: Int)` — remove; if it was the root, clear the root; recompute.
- `toggleRoot(pitchClass: Int)` — mark this note as the sole root, or unmark if it already is; recompute. Marking a new root moves the marker (only one root).
- `clearAll()` — empty the list and root; recompute (→ empty results).
- `startListening()` / `stopListening()` — flip `isListening`; start/stop the `NoteDetector`; while listening, collect `detectedNotes()` and feed each into the add path.

Recompute is a single private function: build `KeyFinderInput(pitchClasses, rootPitchClass)`, call the use case, update state (`results` + `matchCount`). The ≥3 gate lives in the use case, so below 3 notes `results` is naturally empty and the UI shows its idle state.

Threading: `MatchScalesUseCase` is a fast synchronous pure function; calling it on the main dispatcher is fine. If profiling ever shows jank, move it to `Dispatchers.Default` — but do not pre-optimise. The `NoteDetector` collection runs in `viewModelScope` on an appropriate dispatcher.

No logic in `init {}` (per `CLAUDE.md` §7). Listening starts only on explicit `startListening()`.

## Tests

`KeyFinderViewModelTest` with a **fake** `MatchScalesUseCase` (or the real one — it's pure, so the real one makes assertions concrete) and a **fake** `NoteDetector` whose `detectedNotes()` flow the test drives:

- Adding notes updates `notes` and, past 3 distinct, populates `results`; below 3, `results` is empty.
- Adding a duplicate pitch class is a no-op.
- Removing a note recomputes; removing the root clears `rootPitchClass`.
- `toggleRoot` sets, moves (only one root at a time), and unsets the root, each triggering a recompute and changing the resulting ranking/percentages as 7.1 dictates.
- `clearAll` empties everything.
- `startListening()` sets `isListening`, starts the detector, and a note emitted on the fake flow is added (and de-duped) exactly as a picker add would be.
- `stopListening()` clears `isListening` and stops the detector; subsequent flow emissions are ignored.
- `matchCount` equals `results.size`.
- (If using the real use case) the full-C-major + root-A case yields A Natural Minor #1 at 100% with the six siblings at 88% — an end-to-end-through-the-ViewModel assertion.

Use Turbine if `StateFlow` assertions get awkward (optional, per `IMPLEMENTATION_NOTES.md`).

## Steps

1. Lock the start-of-phase decisions; record in `DECISIONS.md`.
2. `KeyFinderUiState` + `NoteChip`.
3. `KeyFinderViewModel` with the intents and the single recompute path.
4. Wire `NoteDetector` collection under `startListening()`/`stopListening()`.
5. Tests (100% of ViewModel logic).
6. `DECISIONS.md` entries (note identity / pitch-class de-dup, root-removal, dedup no-op, note cap).
7. Hand off.

## Completion Criteria

See `Phase7_3-REQUIREMENTS.md`.
