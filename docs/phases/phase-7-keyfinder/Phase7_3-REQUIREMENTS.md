# Phase 7.3 — Requirements & Acceptance Criteria

Phase 7.3 produces presentation logic, no screen. Verified by unit tests.

## Agent Responsibilities

### `KeyFinderUiState` + `NoteChip`
- [ ] `KeyFinderUiState(notes, rootPitchClass, isListening, results, matchCount)` as specified.
- [ ] `NoteChip(pitchClass, displayName, isRoot)`.
- [ ] State carries domain `ScaleMatch`es only — no `Context`, resources, or pre-rendered card strings.

### `KeyFinderViewModel`
- [ ] Constructor-injected `MatchScalesUseCase` + `NoteDetector`. Single `StateFlow<KeyFinderUiState>`.
- [ ] `addNoteFromPicker` adds by pitch class; duplicate pitch class is a no-op.
- [ ] `removeNote` removes and clears the root if it was the removed note.
- [ ] `toggleRoot` enforces a single root (sets / moves / unsets) and recomputes.
- [ ] `clearAll` empties notes + root.
- [ ] `startListening` / `stopListening` flip `isListening`, start/stop the detector, and route confirmed notes into the add path while listening; ignore emissions when stopped.
- [ ] One private recompute path builds `KeyFinderInput` and calls the use case on every mutation.
- [ ] No logic in `init {}`; listening is not auto-started.
- [ ] Note list de-dupes by pitch class; cap is a named constant.

### Tests (`KeyFinderViewModelTest`)
- [ ] Add/remove/duplicate/clear behaviours, gate (<3 → empty `results`), root set/move/unset, listening start/stop + flow routing, `matchCount == results.size`.
- [ ] At least one end-to-end assertion (full C major + root A → A Natural Minor #1 @ 100%, siblings @ 88%).
- [ ] 100% of ViewModel logic covered (`CLAUDE.md` §6).

### Documentation Updates
- [ ] `DECISIONS.md`: pitch-class de-dup / note identity; root-removal-clears; duplicate-add no-op; note cap value.

### Code Quality
- [ ] No `TODO()`. KDoc on public API. No magic numbers. Any user-facing strings (e.g. "MATCHES" header text) deferred to 7.4's `strings.xml` — the ViewModel exposes counts/flags, not formatted UI text.

### Handoff
- [ ] Summary lists files and confirms the ViewModel is ready for 7.4 to render with no further logic needed.

## User Responsibilities (Verification in Android Studio)
- [ ] Gradle sync + Build succeed.
- [ ] Run All Tests green.
- [ ] App still launches; no new runtime UI yet (placeholder Key Finder screen unaffected unless 7.4-prep wiring is staged — if so, flagged in handoff).

## Decision Log
- [ ] All four 7.3 decision entries recorded in `DECISIONS.md` before close.
