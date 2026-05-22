# Kickoff Prompt — Phase 6.3

You are starting **Phase 6.3 — Metronome ViewModel & State Management** of the Toniqo Android app.

## What you are doing

Wiring the player and preferences from 6.2 into a complete observable `MetronomeUiState`. The ViewModel exposes a single `StateFlow<MetronomeUiState>` plus a `SharedFlow<MetronomeEvent>` for one-shot errors. All BPM input methods (text dialog input, slider, +/− buttons, tap tempo) go through the ViewModel and are unified through `onBpmChanged`.

This sub-phase is **pure orchestration** — no Compose UI, no `AudioTrack`. All work is unit-testable on the JVM using the fakes from 6.2 plus a new `FakeMetronomePlayer`.

Phase 6.2 must be complete before starting 6.3. You consume `MetronomePlayer`, `MetronomePreferences`, `TapTempoCalculator`, and `MetronomeConfig.DEFAULT` / `BPM_MIN` / `BPM_MAX` / `SUPPORTED_SIGNATURES`.

## Read first, in this order

1. `CLAUDE.md` — house rules.
2. `APP_SPECIFICATION.md` → "Module: Metronome".
3. `DESIGN.md` → §8.2.
4. `DECISIONS.md` — all entries, especially anything added in 6.1 and 6.2.
5. `Phase6-Metronome-Decisions.md` — Items 4, 6, 9, 17, 19.
6. `Phase6_2-PLAN.md` and the actual 6.2 code — these are your dependencies.
7. `Phase5_3-PLAN.md` — the tuner's ViewModel is the closest reference for the ViewModel-with-preferences pattern, the use case shape, and how to handle one-shot events vs. state.
8. **`Phase6_3-PLAN.md`** — the implementation plan.
9. **`Phase6_3-REQUIREMENTS.md`** — the acceptance checklist.

## How to work

- Follow the **Steps** section of `Phase6_3-PLAN.md` in order.
- The Phase 2 `StopMetronomeUseCase` is **removed** in this sub-phase — stopping is now coroutine cancellation, not a function call. Record removal in `DECISIONS.md`.
- The Phase 2 `MetronomeUiState` is **extended** with `tempoDescriptor` and `isInitialLoadComplete`. Record in `DECISIONS.md`.
- One-shot events (errors) flow through a separate `SharedFlow<MetronomeEvent>` — **do not mix events into the state**. The state is for current values; events are for things that happen once.
- BPM persistence is **debounced 200ms** — the player gets updates immediately, but DataStore doesn't get hammered during slider drags. `PERSIST_DEBOUNCE_MS` is the only literal in the ViewModel.
- All BPM input methods (`onBpmChanged`, `onBpmIncrement`, `onBpmDecrement`, `onTapTempo`) go through a single internal path that clamps to `[BPM_MIN, BPM_MAX]`. Don't duplicate the clamp logic.
- `onTimeSignatureChanged(num, den)` validates against `SUPPORTED_SIGNATURES` and silently rejects unsupported pairs. No exception, no event — the UI prevents this from happening, but the ViewModel guards defensively.
- The interim Phase 4 placeholder is updated to consume the real ViewModel. **Replace the 6.2 debug harness** with a minimal ViewModel-observing layout. Keep the `// TODO(6.4): replace with real screen` marker.
- Tests use `runTest`, virtual time, `FakeMetronomePreferences`, `FakeMetronomePlayer`, and a `TapTempoCalculator` with a fake `Clock`. The fake player needs to be created in this phase under `app/src/test/.../metronome/fakes/`.
- No magic numbers. `PERSIST_DEBOUNCE_MS` is the only literal and it's named.

## Hand-off

When you're done:
1. Hand-off summary lists files added, modified, and **removed** — note `StopMetronomeUseCase.kt` is removed.
2. Explicitly call out: "the placeholder screen now talks to the real ViewModel, but it's still a placeholder — 6.4 replaces it."
3. List the smoke tests from `Phase6_3-REQUIREMENTS.md` → "User Responsibilities". The user can now exercise persistence, lifecycle handling, and live config changes through the placeholder UI.
4. **You do not run the build or the app yourself.**

When the user reports issues, fix them and hand off again.
