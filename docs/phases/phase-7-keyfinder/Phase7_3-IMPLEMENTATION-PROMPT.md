# Implementation Prompt — Phase 7.3 (ViewModel & State)

> Paste this to start a fresh implementation session for **Phase 7.3**. Phases 7.1 (engine) and 7.2 (audio + `NoteDetector`) must already be merged.

---

You are implementing **Phase 7.3 — ViewModel & State** of Toniqo, a native Android guitar toolkit. You write code; the user owns Android Studio, the build, the emulator/device, and Git. Do not run Gradle, launch emulators, or invoke git. Propose complete file contents the user can apply.

**Before writing any code, read, in this order:**
1. `PROJECT_PLAN.md`, `CLAUDE.md`, `IMPLEMENTATION_NOTES.md`, `DECISIONS.md` (always-read set; note the Phase 7.1 and 7.2 entries).
2. `Phase7-PLAN.md` → "Scoring Model" and "Ranking & Display Rules" (the ≥3 gate and live-recompute rules).
3. `Phase7_1-PLAN.md` (the `MatchScalesUseCase` and domain-model contracts) and `Phase7_2-PLAN.md` (the `NoteDetector` interface).
4. `Phase7_3-PLAN.md` and `Phase7_3-REQUIREMENTS.md` — your scope for this session.
5. `CLAUDE.md` §7 (`StateFlow` in ViewModels, sealed state, no logic in `init {}`) and §6 (100% ViewModel coverage).

**Hard constraints (from `CLAUDE.md` / `IMPLEMENTATION_NOTES.md`):**
- Kotlin, minSdk 31, Hilt (KSP), JUnit 4 + MockK. Feature-first; presentation logic in `keyfinder/presentation/`.
- SOLID strictly; constructor injection. No `!!`, no logic in `init {}`, sealed/`data class` types, `StateFlow` exposed (never `MutableStateFlow`).
- No magic numbers or strings (note cap is a named constant; no formatted UI text in the ViewModel — expose counts/flags, defer text to 7.4's `strings.xml`). KDoc on public API. Tests alongside the code.
- **Stop and ask** rather than guess if anything is ambiguous or a decision would cascade.

**Lock these decisions at the start of the session and record them in `DECISIONS.md` before coding:**
1. **Note identity** — the list de-dupes by **pitch class** (one chip per pitch class; the first-seen `Note` provides the chip's spelling). Confirm.
2. **Root removal** — removing the marked-root note simply **clears** the root (no auto-reassignment). Confirm.
3. **Duplicate add** — adding a pitch class already present is a **no-op** (no error, no duplicate). Confirm.
4. **Note cap** — a sane maximum (suggested 12, one per pitch class), as a named constant. Confirm.
5. **Mic cadence** — each confirmed note from `NoteDetector` is one add → one recompute; no extra debounce beyond the detector's own (7.2). Confirm.

**Your task this session (presentation logic only — no composables):**
1. `keyfinder/presentation/viewmodel/KeyFinderUiState.kt` — `KeyFinderUiState(notes: List<NoteChip>, rootPitchClass: Int?, isListening: Boolean, results: List<ScaleMatch>, matchCount: Int)` and `NoteChip(pitchClass: Int, displayName: String, isRoot: Boolean)`. State carries domain `ScaleMatch`es only — **no** `Context`, resources, or pre-rendered card strings.
2. `keyfinder/presentation/viewmodel/KeyFinderViewModel.kt` — constructor-injected `MatchScalesUseCase` + `NoteDetector`; a single `MutableStateFlow<KeyFinderUiState>` exposed as `StateFlow`. Intents:
   - `addNoteFromPicker(note: Note)` — add by pitch class (no-op if present); recompute.
   - `removeNote(pitchClass: Int)` — remove; clear root if it was the removed note; recompute.
   - `toggleRoot(pitchClass: Int)` — set / move / unset the single root; recompute.
   - `clearAll()` — empty notes + root; recompute.
   - `startListening()` / `stopListening()` — flip `isListening`, start/stop the `NoteDetector`, and while listening collect `detectedNotes()` and route each into the add path; ignore emissions when stopped.
   - One private recompute path builds `KeyFinderInput(pitchClasses, rootPitchClass)`, calls the use case, updates `results` + `matchCount`. The ≥3 gate lives in the use case, so below 3 notes `results` is naturally empty.
   - No logic in `init {}`; listening starts only on explicit `startListening()`. Run the `NoteDetector` collection in `viewModelScope`. Calling the (pure, synchronous) use case on the main dispatcher is fine — do not pre-optimise to `Dispatchers.Default`.

**Tests (`KeyFinderViewModelTest`, 100% of ViewModel logic):**
Prefer the **real** `MatchScalesUseCase` (it is pure, making assertions concrete) and a **fake** `NoteDetector` whose `detectedNotes()` flow the test drives. Cover: add/remove updates and the <3 → empty `results` gate; duplicate add is a no-op; removing the root clears `rootPitchClass`; `toggleRoot` sets/moves/unsets and changes ranking accordingly; `clearAll` empties everything; `startListening` sets the flag, starts the detector, and a flow emission is added (de-duped) like a picker add; `stopListening` clears the flag, stops the detector, and later emissions are ignored; `matchCount == results.size`; and one end-to-end assertion (full C major + root A → A Natural Minor #1 @ 100%, six siblings @ 88%). Use Turbine if `StateFlow` assertions get awkward (optional).

**When done:**
- Append the `DECISIONS.md` entries (pitch-class de-dup / note identity; root-removal-clears; duplicate-add no-op; note cap value), dated and append-only.
- Summary: files added/modified, and confirmation the ViewModel is ready for 7.4 to render with no further logic needed. Note whether app startup is affected (it should not be unless you stage 7.4-prep wiring — if so, flag it).
- Organise the proposal to map cleanly to commits (e.g. `feat: add Key Finder ViewModel and UI state with tests`). Do not commit yourself.

Confirm you have read the listed docs, have locked the start-of-phase decisions, and have no blocking questions — then proceed file by file with tests.
