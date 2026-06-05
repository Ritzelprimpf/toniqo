# Implementation Prompt — Phase 8.1 (Chord Finder · Diatonic Chord Engine)

> Paste this to start a fresh implementation session for Phase 8.1.

---

You are implementing **Phase 8.1 — Diatonic Chord Engine** of Toniqo, a native Android guitar toolkit. You write code; the user owns Android Studio, the build, the emulator/device, and Git. Do not run Gradle, launch emulators, or invoke git. Propose complete file contents the user can apply.

**Before writing any code, read, in this order:**
1. `PROJECT_PLAN.md`, `CLAUDE.md`, `IMPLEMENTATION_NOTES.md`, `DECISIONS.md` (always-read set).
2. `APP_SPECIFICATION.md` → "Module: Chord Finder".
3. `Phase8-PLAN.md` — the whole module plan. Read "Stage 1 — The Chord Engine" closely, and the **conflict note**: this phase supersedes `APP_SPECIFICATION.md` on Chord Finder scope (14 `ScaleType`s, not 7 modes; guitar voicings added). Follow the plan, not the older spec.
4. `Phase8_1-PLAN.md` and `Phase8_1-REQUIREMENTS.md` — your scope for this session.
5. The Phase 7.1 `DECISIONS.md` entries and `common/util/ScaleSpeller.kt` / `common/model/ScaleType.kt` — you build directly on these.

**Hard constraints (from `CLAUDE.md` / `IMPLEMENTATION_NOTES.md`):**
- Kotlin, minSdk 31, Jetpack Compose (M3), Hilt (KSP), JUnit 4 + MockK. Feature-first packages with Clean Architecture inside; pure music theory in `common/`.
- SOLID strictly. Pure stateless utilities may be top-level `object`s (the documented exception); everything else constructor-injected.
- No `!!`, no logic in `init {}`, sealed types for state/results, `data class` for models.
- No magic numbers or strings; all user-visible text in `res/values/strings.xml`; **no hardcoded note-name strings inside algorithms** — work in pitch classes, convert to display only at the `ScaleSpeller` boundary.
- KDoc on all public types/functions. Tests alongside the code they cover.
- **Stop and ask** rather than guess if anything is ambiguous or a decision would cascade.

**Your task this session (8.1 — pure Kotlin; no Android, no voicings, no ViewModel, no UI):**
Implement, with exhaustive unit tests:
1. `common/model/ChordQuality.kt` — exactly `MAJOR, MINOR, DIMINISHED, AUGMENTED` (reconcile, don't duplicate, if one exists).
2. `chordfinder/domain/model/SeventhQuality.kt` — the seven seventh types, each exposing a **suffix string-resource key**.
3. `chordfinder/domain/ChordQualityResolver.kt` — pure interval→quality mapping (`triad`, `seventh`), throwing on any combination outside the valid sets.
4. `chordfinder/domain/model/` — `DegreeChord`, `ChordFinderInput` (carries `scaleType: ScaleType`), `ChordFinderResult`. **Supersede** the Phase 2 `Mode`-based stubs and **remove** the Phase 2 `ChordFinderService`/`Impl` stubs (logic lives in the use case, mirroring Key Finder's `MatchScalesUseCase`).
5. `chordfinder/domain/usecase/FindChordsUseCase.kt` — stack thirds on each of the 7 degrees, derive quality from intervals, spell via `ScaleSpeller`, build Roman numerals/symbols. Synchronous pure function, **not** `suspend`.
6. `strings.xml` — chord-symbol suffixes and quality abbreviations.

**The interval-derivation is the heart of this.** Reproduce every fixture in `Phase8_1-PLAN.md` → "Tests" as unit tests — at minimum: C Ionian triads + sevenths (matching the two list mockups), A Aeolian, **A Harmonic Minor triads and sevenths** (`Caug`, and `AmMaj7 / Cmaj7♯5 / G♯dim7`, with `G♯` spelled correctly), A Melodic Minor sevenths, and a **12 roots × 14 `ScaleType`s** sweep asserting exactly 7 chords, only the four triad qualities ever appear, names are 7-distinct-letter-correct, and the resolver never throws.

**When done:**
- Append the 8.1 decisions to `DECISIONS.md` (dated, append-only): 14-`ScaleType` target (**explicitly superseding** `APP_SPECIFICATION.md`); interval-derived quality + the enumerated seventh set; guitar-voicings-added scope note (**explicitly superseding** `APP_SPECIFICATION.md`, engine in 8.2); removal of the Phase 2 `ChordFinderService` stub in favour of the use case.
- Summary: files added/modified; note 8.1 is logic-only (no UI, no DI the app launches against; startup unaffected).
- Organise the proposal to map cleanly to commits (e.g. `feat: chord quality resolver`, `feat: diatonic chord engine with tests`). Do not commit yourself.

Confirm you have read the listed docs and have no blocking questions, then proceed file by file with tests.
