# Kickoff Prompt — Phase 6.1

You are starting **Phase 6.1 — Click Synthesizer & Audio Format Foundation** of the Toniqo Android app.

## What you are doing

Implementing the metronome's audio synthesis foundation: pure-Kotlin code that produces PCM click buffers, plus the small pure helpers and tempo-descriptor lookup that downstream phases depend on. **No Android runtime code, no `AudioTrack`, no UI, no ViewModel** — everything in this sub-phase is unit-testable on the JVM.

## Read first, in this order

1. `CLAUDE.md` — house rules, formatting, naming, the testing framework, and the "ask before introducing a new pattern" rule.
2. `APP_SPECIFICATION.md` → "Module: Metronome".
3. `DESIGN.md` → §8.2.
4. `DECISIONS.md` — all entries. Pay attention to anything about audio, file structure, or naming conventions.
5. `Phase6-Metronome-Decisions.md` — the planning decision log. Items 1, 3, 7, 8, 15, 21 are the relevant ones for this sub-phase.
6. **`Phase6_1-PLAN.md`** — the implementation plan. Authoritative for what to build.
7. **`Phase6_1-REQUIREMENTS.md`** — the acceptance checklist.

If anything in the plan conflicts with `DESIGN.md` or `APP_SPECIFICATION.md`, raise the conflict before guessing.

## How to work

- Follow the **Steps** section of `Phase6_1-PLAN.md` in order.
- Use the **Requirements** as your final checklist — every checkbox must be satisfiable when you're done.
- Tests are exhaustive, not token. See the "Tests" section of the plan.
- All numeric values become named constants. No magic numbers anywhere in the new code.
- New types are `internal` unless they extend an existing public surface (the `Subdivision` enum is public from Phase 2 and stays public; new audio types in `metronome/data/audio/` are internal).
- Append-only decision log: any non-trivial decision you make on the way (especially around the `Subdivision` enum signature change) gets a dated entry in `DECISIONS.md`. The plan already lists the required entries.

## Hand-off

When you're done:
1. Run `./gradlew test` mentally — confirm every test you wrote covers the requirements.
2. Write a hand-off summary listing **files added** and **files modified** (no files should be removed in 6.1).
3. Note that 6.1 produces no user-visible UI changes — the user's sanity check is "the app still launches; Gradle sync, build, and tests all pass; no Hilt binding errors."
4. Hand back to the user. **You do not run the build or the app yourself** — the user verifies in Android Studio.

When the user reports back with any failures, fix them and hand off again.
