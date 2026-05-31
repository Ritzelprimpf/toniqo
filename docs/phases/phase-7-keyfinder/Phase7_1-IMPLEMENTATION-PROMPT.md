# Implementation Prompt — Phase 7 (Key Finder)

> Paste this to start a fresh implementation session. It is written for **Phase 7.1**. To run a later sub-phase, change every `7.1` / `Phase7_1` to the target sub-phase number — the structure is identical.

---

You are implementing **Phase 7.1 — Scale Model & Matching Engine** of Toniqo, a native Android guitar toolkit. You write code; the user owns Android Studio, the build, the emulator/device, and Git. Do not run Gradle, launch emulators, or invoke git. Propose complete file contents the user can apply.

**Before writing any code, read, in this order:**
1. `PROJECT_PLAN.md`, `CLAUDE.md`, `IMPLEMENTATION_NOTES.md`, `DECISIONS.md` (always-read set).
2. `APP_SPECIFICATION.md` → "Module: Key Finder".
3. `Phase7-PLAN.md` — the whole module plan. Pay special attention to "Scoring Model", "Scale Inventory", "Ranking & Display Rules", "Conventional Spelling Rule", and the **conflict note** (this plan supersedes `APP_SPECIFICATION.md` on Key Finder scoring and scale set — follow the plan, not the older spec).
4. `Phase7_1-PLAN.md` and `Phase7_1-REQUIREMENTS.md` — your scope for this session.

**Hard constraints (from `CLAUDE.md` / `IMPLEMENTATION_NOTES.md`):**
- Kotlin, minSdk 31, Jetpack Compose (Material 3), Hilt (KSP), JUnit 4 + MockK. Feature-first packages with Clean Architecture inside each (`keyfinder/data` → `domain` → `presentation`); pure music theory in `common/`.
- SOLID strictly. Pure stateless utilities may be top-level `object`s (the documented exception); everything else is constructor-injected.
- No `!!`, no logic in `init {}`, sealed types for state/results, `data class` for models.
- No magic numbers or strings; all user-visible text in `res/values/strings.xml`; no hardcoded note-name strings inside algorithms (work in pitch classes, convert to display only at the `ScaleSpeller` boundary).
- KDoc on all public types and functions. Tests alongside the code they cover, not at the end.
- **Stop and ask** rather than guess if anything is ambiguous or a decision would cascade.

**Your task this session (7.1 — pure Kotlin, no Android, no audio, no UI):**
Implement, with exhaustive unit tests:
1. `common/model/ScaleFamily.kt` and `common/model/ScaleType.kt` (the 14 scale types; the 7 diatonic patterns must equal the existing `Mode` enum — assert it, don't silently duplicate; expose label **resource keys**, not text).
2. `common/util/ScaleSpeller.kt` (conventional spelling: canonical root table + letter-per-degree accidentals).
3. `keyfinder/domain/model/` — `ScaleCandidate`, `KeyFinderInput`, `ScaleMatch` (no display strings on the domain model).
4. `keyfinder/domain/ScaleCatalog.kt` (exactly 168 candidates).
5. `keyfinder/domain/usecase/MatchScalesUseCase.kt` — the locked score formula (`points/maxPoints`, root +1, `maxPoints = max(7, n) (+1 if root)`, round half up), the ≥3 gate, exclude 0%, rank (percent desc → `rankOrder` asc → root pc asc), cap at 7. Synchronous pure function, **not** `suspend`.
6. `strings.xml` entries for the 14 labels + subtitles (with a `%s` root placeholder).

**The scoring is the heart of this.** Reproduce every canonical worked example from `Phase7-PLAN.md` as a unit test — at minimum: `{C,E,G}` → 43% (no root) and the 50%/38% split (root C); the seven modes of C major all 100% + `FULL` and ordered Major→Natural Minor→rest; root A lifting A Natural Minor to 100%/`TONIC`/`FULL`/#1 with its six siblings at 88%; the stray-B♭ case at 88% and **not** `FULL`; the top-7 cap; and `isFull` true at 43% for a 3-note containing scale. Also assert the spelling canon (e.g. `A Harmonic Minor` → `A B C D E F G♯`, `F♯ Major` → `F♯ G♯ A♯ B C♯ D♯ E♯`) and that every scale spells with 7 distinct letters.

**When done:**
- Append the 7.1 decisions to `DECISIONS.md` (dated, append-only): the scoring formula (explicitly superseding the `APP_SPECIFICATION.md` "% of input notes" rule), single root folded into the percentage, the 14-type inventory + exclusions, the ranking rule (explicitly superseding the separate-tonic-ranking step), the ≥3 gate, conventional spelling, and the `ScaleType` superset alongside the retained `Mode`.
- Give the user a summary: files added/modified, and a note that 7.1 is logic-only (no UI, no new DI the app launches against, so startup is unaffected).
- Organise the proposal so it maps cleanly to commits (e.g. `feat: add ScaleType and ScaleSpeller`, `feat: add Key Finder matching engine with tests`). Do not commit yourself.

Confirm you have read the listed docs and have no blocking questions, then proceed file by file with tests.
