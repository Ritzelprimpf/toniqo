# Kickoff Prompt — Phase 6.2

You are starting **Phase 6.2 — Metronome Player, Scheduler, Persistence** of the Toniqo Android app.

## What you are doing

Implementing the metronome's audio engine and the persistence layer. This is the first sub-phase where actual audio plays — `AudioTrack` is initialized, beats are scheduled with the anchor-based drift-corrected loop, audio focus is requested and respected, and BPM / time signature / subdivision survive across app launches via DataStore.

It is also the **heaviest** sub-phase: it touches the most real Android infrastructure. Expect to spend the bulk of Phase 6's effort here.

Phase 6.1 must be complete before starting 6.2. You consume `ClickSynthesizer`, `MetronomeAudioFormat`, `Subdivision.multiplier`, `clicksPerBar`, and `clickKindFor` — they should already exist and be tested.

## Read first, in this order

1. `CLAUDE.md` — house rules.
2. `APP_SPECIFICATION.md` → "Module: Metronome".
3. `DESIGN.md` → §8.2.
4. `DECISIONS.md` — all entries, especially anything added in 6.1.
5. `Phase6-Metronome-Decisions.md` — Items 2, 4, 5, 6, 14, 15, 16, 17, 19.
6. `Phase6_1-PLAN.md` and the actual 6.1 code — these are your dependencies.
7. `Phase5_2-PLAN.md` — the tuner's `MicrophoneAudioSource` is the closest reference pattern for the new flow-based `MetronomePlayer` API. Mimic its `callbackFlow` + `awaitClose` lifecycle structure.
8. `Phase5_3-PLAN.md` — the tuner's `TunerPreferences` is the closest reference pattern for `MetronomePreferences`. Mimic its interface/impl/fake structure.
9. **`Phase6_2-PLAN.md`** — the implementation plan.
10. **`Phase6_2-REQUIREMENTS.md`** — the acceptance checklist.

If anything conflicts with `DESIGN.md` or `APP_SPECIFICATION.md`, raise it before guessing.

## How to work

- Follow the **Steps** section of `Phase6_2-PLAN.md` in order.
- The Phase 2 `MetronomePlayer` interface (imperative `start`/`stop`/`updateConfig`/`currentBeat`) is **superseded** by the new flow-based `run(initialConfig, configFlow): Flow<PlayerEvent>`. Record this in `DECISIONS.md` before implementing.
- The Phase 2 `Subdivision` enum may have been updated in 6.1 to carry `multiplier`. Verify before assuming.
- Extract pure scheduling logic into testable helpers. `AudioTrack` itself is verified by user smoke test, not by unit tests.
- Inject `Clock` everywhere time-dependent code lives. `System.nanoTime()` is not virtualized by `kotlinx-coroutines-test`, so the abstraction is mandatory for testability.
- The plan requires you to **add a temporary debug harness** (a Play/Stop button) to the existing Phase 4 placeholder screen so the user can audibly verify audio works. Mark it with `// TODO(6.4): replace with real screen`. It's scaffolding — not polished UI.
- No magic numbers. `60_000_000_000L` (nanoseconds per minute) is named. Buffer sizes, timeouts, audio attributes are named.
- Resource lifetime is non-negotiable: `AudioTrack` must be released in `awaitClose`, audio focus abandoned, the listener unregistered. A leaked `AudioTrack` corrupts the whole audio stack on the device.
- Append-only decision log entries for: the `MetronomePlayer` API change, anchor-based scheduling, screen-lifecycle-only playback, the `metronome_preferences` DataStore file, whole-config replacement on invalid persisted data, tap-tempo algorithm parameters, audio attributes choice.

## Hand-off

When you're done:
1. Hand-off summary lists files added, modified, and removed.
2. Explicitly call out the debug harness as "6.2 scaffolding to be replaced in 6.4."
3. List the smoke tests the user is expected to perform — they're enumerated in `Phase6_2-REQUIREMENTS.md` under "User Responsibilities". The user verifies actual audio behaviour on a real device (emulator works but real hardware is better for audio quality assessment).
4. If the user reports a click sounds wrong (too loud / too soft / wrong pitch), this is **expected** for v1 starting values — adjust the relevant constant in `ClickParameters` (from 6.1) and record the change in `DECISIONS.md`. This is not a defect.
5. **You do not run the build or the app yourself.** The user verifies in Android Studio and on-device.

When the user reports issues, fix them and hand off again.
