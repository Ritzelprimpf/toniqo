# Implementation Prompt — Phase 8.4 (Chord Finder · Chord List Screen)

> Paste this to start a fresh implementation session for Phase 8.4. Phases 8.1–8.3 must be complete.

---

You are implementing **Phase 8.4 — Chord List Screen** of Toniqo. You write code; the user owns the build, device, and Git. Do not run Gradle or git.

**Before writing any code, read, in this order:**
1. `PROJECT_PLAN.md`, `CLAUDE.md`, `IMPLEMENTATION_NOTES.md`, `DECISIONS.md`.
2. `Phase8-PLAN.md` → "Display Rules" (list screen).
3. `DESIGN.md` → §8.4 (Chord Finder list), §6 (dropdowns, chips, segmented control — reuse existing ones), §2.4 (degree colours), §2 (colour tokens), §3 (type).
4. `Phase8_4-PLAN.md` and `Phase8_4-REQUIREMENTS.md` — your scope.
5. `Chord_Finder___triads.png` and `Chord_Finder___7ths.png` — the authoritative visuals.
6. `Phase8_3-PLAN.md` — the `ChordFinderViewModel`/state you render.

**Hard constraints:** as before. Additionally: stateless content composable separated from the Hilt wrapper; use **only** `DESIGN.md` tokens (no raw hex/sp/dp where a token exists); reuse existing dropdown/segmented-control components rather than inventing new ones; min tap target 48dp; previews for every state. **Stop and ask** on ambiguity.

**Your task this session (UI for the list screen; no voicings screen, no nav graph):**
1. `chordfinder/presentation/ui/DegreeColor.kt` — exhaustive `ChordQuality → Tq` token mapping (mint/cyan/amber/violet), no `else`.
2. `chordfinder/presentation/ui/ChordDegreeRow.kt` — coloured Roman numeral + quality abbreviation, chord symbol, note pills, trailing chevron; whole row clickable.
3. `chordfinder/presentation/ui/ChordFinderScreen.kt` — header (kicker `CHORD FINDER · DIATONIC`, title `{Root} {ModeLabel}`, info affordance), Root dropdown (12, respelled for the active mode via `ScaleSpeller`), Mode dropdown (all 14 `ScaleType`s), `{n} CHORDS` + `TRIADS / 7THS` segmented toggle, `LazyColumn` of rows. Stateful wrapper forwards chord selection to an `onChordSelected: (ChordKey, String) -> Unit` callback (nav wired in 8.5).
4. `strings.xml` — kicker, dropdown/toggle labels, `%d CHORDS`, quality abbreviations.

**Fidelity to §8.4 and the mockups is the heart of this.** Match the dropdown widths (Mode `flex 1.4`, Root `flex 1`, 42dp), the row anatomy, the degree colours, and the spacing/type. Unit-test `DegreeColor` (exhaustive, correct tokens). Compose UI tests are optional per `CLAUDE.md`; if added, assert 7 rows, the toggle switching `C`↔`Cmaj7`, and the row-click callback.

**When done:**
- Add a `DECISIONS.md` entry only if you resolved a design ambiguity (e.g. info-sheet content); otherwise none.
- Summary: files; note navigation is stubbed via `onChordSelected` pending 8.5.
- Map to commits (e.g. `feat: degree colour mapping`, `feat: chord finder list screen`). Do not commit.

Confirm you have read the docs and have no blocking questions, then proceed.
