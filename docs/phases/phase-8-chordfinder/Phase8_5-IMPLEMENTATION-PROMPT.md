# Implementation Prompt — Phase 8.5 (Chord Finder · Voicings Screen & FretboardDiagram)

> Paste this to start a fresh implementation session for Phase 8.5 — the final sub-phase. Phases 8.1–8.4 must be complete.

---

You are implementing **Phase 8.5 — Voicings Screen & FretboardDiagram** of Toniqo, completing the Chord Finder end-to-end. You write code; the user owns the build, device, and Git. Do not run Gradle or git.

**Before writing any code, read, in this order:**
1. `PROJECT_PLAN.md`, `CLAUDE.md`, `IMPLEMENTATION_NOTES.md`, `DECISIONS.md`.
2. `Phase8-PLAN.md` → "Display Rules" (voicings screen) and "Stage 2".
3. `DESIGN.md` → §6 (components — note it currently lacks a fretboard diagram), §2, §3.
4. `Phase8_5-PLAN.md` and `Phase8_5-REQUIREMENTS.md` — your scope.
5. `Chord_Finder___C_voicings.png` — the authoritative visual (build it **minus** the CAGED shape names).
6. `Phase8_2-PLAN.md` (the `Voicing` model) and `Phase8_3-PLAN.md` (`ChordVoicingsViewModel`).

**Hard constraints:** as before. Additionally: the `FretboardDiagram` is a **stateless** `Canvas` component driven by a render model (never the domain `Voicing`); all sizes via `DESIGN` tokens; the component must lay out correctly for **6, 7, and 8** strings; no animation/interactivity; stateless content split from the Hilt wrapper; previews for every state. **Stop and ask** on ambiguity.

**Your task this session (UI + navigation; completes the module):**
1. `ui/components/FretboardDiagram.kt` — `Canvas` diagram driven by `FretboardRenderModel` (strings, fret window, nut/`Nfr` label, finger dots with numerals, `o`/`x` markers, barre). Root dots use the mint token; others neutral. Variable string count.
2. The pure `Voicing.toRenderModel()` mapper — windows frets around `baseFret`, picks nut vs `Nfr`, maps marks/roots/barre. No Android Canvas; fully unit-testable.
3. `chordfinder/presentation/ui/ChordVoicingsScreen.kt` + voicing card — back arrow + `VOICINGS · {CHORD}`, title + note pills (root mint), `{n} SHAPES` + `ROOT` legend + **tuning indicator**, `LazyVerticalGrid(2)` of cards (index, diagram, `FR x–y`, `OPEN/BARRE/SHAPE` tag). Annotate the header on `UNIFORM_OFFSET`; show the "shown for standard tuning" note on `UNSUPPORTED` (still rendering standard voicings). **No CAGED names.**
4. Navigation: add the Chord Finder list + `chordVoicings/{rootPc}/{quality}/{name}` routes to the app `NavHost`; wire `ChordFinderScreen.onChordSelected` to navigate, and a back action; keep the bottom-nav `CHORD` tab intact.
5. `strings.xml` — VOICINGS kicker, `%d SHAPES`, ROOT legend, TUNING indicator, `OPEN/BARRE/SHAPE` tags, `FR %d–%d`, tier-3 note.
6. `DESIGN.md` §6 — a new **Fretboard Diagram** component entry derived from the mockup (anatomy, dot/marker/barre spec, variable string count, tokens; note the deliberate CAGED-name omission).

**The render-model mapper + correct diagram drawing are the heart of this.** Unit-test the mapper per `Phase8_5-PLAN.md` → "Tests" (nut vs `Nfr`, dot/open/muted conversion, root flags, barre, high-position windowing, 6/7/8-string `stringCount`). Provide previews: open chord, barre chord, high-position chord, a 7-string layout, and the screen in standard / uniform-offset / unsupported states.

**When done:**
- Append to `DECISIONS.md`: the `DESIGN.md` §6 `FretboardDiagram` addition and the tier-3 fallback presentation.
- Summary: files; state that **Phase 8 is complete end-to-end**, and give the user the full end-to-end verification checklist from `Phase8_5-REQUIREMENTS.md` (standard + E♭ + Drop D tunings; Key Finder seed on fresh install; manual change stays put).
- Map to commits (e.g. `feat: fretboard diagram component`, `feat: voicings screen + navigation`, `docs: design system fretboard component`). Do not commit.

Confirm you have read the docs and have no blocking questions, then proceed file by file with tests.
