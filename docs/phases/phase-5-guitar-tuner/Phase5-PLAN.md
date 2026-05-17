# Phase 5 — Guitar Tuner Implementation

## Goal

Implement the Guitar Tuner module fully. This is the first real module to be built. By the end of this phase the tuner must be functional, tested, and production-quality on a real device.

## Approach

Phase 5 is divided into sub-phases for incremental implementation and testing. Each sub-phase has its own `Phase5.N-PLAN.md` and `Phase5.N-REQUIREMENTS.md`, to be written at the start of that sub-phase. Sub-phases must be completed in order — each builds on the previous.

## Sub-Phases

| Sub-Phase | Name | Description |
|---|---|---|
| 5.1 | Tuning Data & Music Theory | Implement `MusicTheory` (note ↔ frequency, cents math), the preset catalog as hardcoded constants, and unit tests for both |
| 5.2 | Pitch Detection | Implement microphone capture via `AudioRecord` and the YIN pitch detector. Lock in audio capture parameters and YIN threshold. |
| 5.3 | Tuner Logic & ViewModel | Implement the use case layer, the sustained-500ms state machine, both operating modes (sequential and chromatic), and wire `PitchDetector` → `TunerViewModel` |
| 5.4 | Tuner UI | Implement the full `TunerScreen` matching `DESIGN.md` §8.1, including the needle gauge, string selector, preset picker, A4=432 Hz settings sheet, and microphone permission-denied state |

> Sub-phase plans must be written before each sub-phase begins. Create `Phase5.1-PLAN.md`, `Phase5.1-REQUIREMENTS.md`, etc. under `docs/phase-5-guitar-tuner/` as work progresses.

## Reference Material

Before starting any sub-phase, read:

- `APP_SPECIFICATION.md` → "Module: Guitar Tuner"
- `IMPLEMENTATION_NOTES.md` → "Audio" and "Music Theory Primitives"
- `DESIGN.md` → §8.1 (Tuner) and §2.4 (semantic colour mappings)
- `DECISIONS.md` → all entries

## How Pitch Comparison Actually Works

This section consolidates the technical model the implementation must follow. It is not a re-decision — it is the corrected version of what `APP_SPECIFICATION.md` describes, captured here in one place so sub-phase plans can reference it without ambiguity.

**Signal flow:**

1. **Audio capture.** `AudioRecord` (mono, PCM 16-bit, 44.1 kHz baseline) writes microphone samples into a buffer of `N` frames. `N` is determined in Phase 5.2 by `AudioRecord.getMinBufferSize()` floored at 2048; smaller buffers lower latency but increase pitch-detection noise.

2. **Pitch detection.** The buffer is passed to the YIN algorithm, which returns either a fundamental frequency `f_detected` in Hz or `null` if no pitch was detectable (silence, transient noise, multi-pitch, etc). YIN is chosen specifically because guitar strings produce strong harmonics — a naive FFT-based detector would often lock onto the second harmonic and report 2× the true frequency. YIN avoids this via cumulative-mean-normalized difference.

3. **Cents conversion.** The detected frequency is converted to a **cents offset** from the target frequency:

   ```
   cents = 1200 × log2(f_detected / f_target)
   ```

   Cents are used (not raw Hz) because the perceptual distance between two pitches is logarithmic. A ±5-cent tolerance feels equally tight at E2 (~82 Hz) and at E4 (~330 Hz); a "±X Hz" tolerance would be much stricter at low pitches than high ones.

4. **Target selection.** Which frequency is `f_target`?

   - **Sequential mode:** `f_target` is the frequency of the current string in the selected tuning. The current string starts at index 0 (lowest) and advances when the sustained-tolerance condition is met.
   - **Chromatic mode:** `f_target` is the frequency of whichever string in the current tuning is closest in pitch to `f_detected`. The app re-evaluates which string is closest on every detection cycle.

5. **Sustained-tolerance check.** A string is considered in tune when `abs(cents) ≤ 5` for at least 500 ms of continuous detection. This is a state machine, not an instantaneous check — see Phase 5.3 for the implementation pattern.

**Target-frequency computation.** Target frequencies are not stored. Each note in a preset (`E2`, `A#1`, etc.) is converted to a frequency at runtime via:

```
f = referencePitchHz × 2^((semitoneOffsetFromA4) / 12)
```

The `referencePitchHz` is 440.0 by default, or 432.0 if the user has toggled it. This means the A4 = 432 Hz feature requires no separate preset table — the same presets work, just at a different reference.

## Operating Modes

The tuner supports two operating modes within the same session. Both are implemented in Phase 5.3.

**Sequential mode** is the default when a tuning is first selected. The tuner targets the lowest string, advances on the sustained-tolerance condition, and on the final string runs the success animation from `DESIGN.md` §8.1 before transitioning automatically into chromatic mode.

**Chromatic mode** can also be entered explicitly via a UI toggle at any time. In this mode the app continuously identifies which string of the current tuning is closest to the played pitch and shows cents-off relative to that target. Tapping a specific string in the selector pins the target to that string regardless of mode.

The mode toggle is exposed in the UI; the exact placement is decided in Phase 5.4.

## Decisions Already Resolved

These decisions are settled and need **not** be re-litigated. The relevant `DECISIONS.md` entries are the source of truth.

- ✅ **Preset storage:** Hardcoded Kotlin constants in `tuner/data/`. (`DECISIONS.md`, 2026-05-17)
- ✅ **Complete preset list:** Defined exhaustively in `APP_SPECIFICATION.md` (6/7/8-string × Standard/Open/Dropped), including modern-metal additions. (`DECISIONS.md`, 2026-05-17 later — modern-metal additions)
- ✅ **Reference pitch:** A4 = 440 Hz default with in-app toggle for A4 = 432 Hz. (`DECISIONS.md`, 2026-05-17)
- ✅ **In-tune tolerance:** ±5 cents, sustained for 500 ms before auto-advancing. (`DECISIONS.md`, 2026-05-17)
- ✅ **Pitch detection algorithm:** YIN. YIN threshold parameter is tuned in Phase 5.2 and recorded then.
- ✅ **Visual design:** Needle gauge readout per `DESIGN.md` §8.1. Idle state, success state, and string selector behaviour fully specified there.
- ✅ **Cents → colour mapping:** `DESIGN.md` §2.4 is the single source of truth.
- ✅ **Sequential and chromatic modes both ship in Phase 5.** Chromatic mode is implemented in 5.3 alongside sequential. (`DECISIONS.md`, 2026-05-17 later)
- ✅ **A4 = 432 Hz toggle UI placement.** Sun-icon button (top-right of the Tuner screen) opens a small settings sheet containing the toggle. (`DECISIONS.md`, 2026-05-17 later)
- ✅ **Microphone permission-denied screen.** Single `ToniqoCard` with mic icon, explanation text, and primary "Grant access" button that opens system app settings. (`DECISIONS.md`, 2026-05-17 later)

## Decisions Still Open (resolve at the start of the relevant sub-phase)

### Phase 5.1

- [ ] How the preset catalog exposes itself to the UI: flat list, or grouped by string count and category? **Recommended: grouped, matching the structure in `APP_SPECIFICATION.md`.** Confirm and record in `DECISIONS.md` at the start of 5.1.

### Phase 5.2

- [ ] Audio capture parameters: confirm **44.1 kHz, mono, PCM 16-bit**, buffer size derived from `AudioRecord.getMinBufferSize()` with a floor of 2048 frames. Lock in `DECISIONS.md`.
- [ ] YIN threshold parameter — typical range 0.10–0.15. Pick a starting value, validate experimentally on representative guitar audio (including low-fundamental tunings like Drop C 8-string), and record the final value in `DECISIONS.md`.
- [ ] Behaviour on detection failure: how often `null` results are tolerated before the UI returns to `LISTENING`. Suggested: 200 ms of consecutive `null` results.

### Phase 5.3

- [ ] Implementation pattern for the sustained-500ms check. **Suggested approach:** a state machine inside `DetectTunedStringUseCase` (or a renamed coordinator class) that tracks consecutive in-tolerance detections and emits an `IN_TUNE_STABLE` status only after the threshold is met. Document the chosen pattern in `DECISIONS.md`.
- [ ] How auto-advance is made toggleable. Default: enabled in sequential mode. The user should be able to disable auto-advance and switch strings manually.

### Phase 5.4

- [ ] UI placement of the mode toggle (sequential vs. chromatic) and the auto-advance toggle. These are minor UI decisions that depend on how the rest of the screen lays out; resolve when building the screen.

## Dependencies

No new libraries are needed for the core tuner. YIN is implemented from scratch — it is a few dozen lines of straightforward math and giving it our own implementation removes a dependency surface.

If a third-party pitch-detection library is ever considered, it must be documented in `DECISIONS.md` with rationale before being added.

## Completion Criteria

Phase 5 is complete when all sub-phases 5.1 through 5.4 have met their individual requirements, **and** the Guitar Tuner module works end-to-end on a real device:

- The user can select a tuning from the categorized picker.
- In sequential mode, the user can play each string of the selected tuning in order and receive accurate visual feedback. The tuner correctly detects "in tune" within ±5 cents sustained for at least 500 ms and advances to the next string.
- After the final string, the success animation from `DESIGN.md` §8.1 plays and the tuner transitions to chromatic mode.
- In chromatic mode, the user can play any string in any order and the tuner correctly identifies the closest target and displays cents-off.
- The A4 = 432 Hz toggle in the settings sheet correctly retunes all target frequencies live.
- The microphone permission-denied state renders correctly and "Grant access" opens system app settings.
- The screen matches `DESIGN.md` §8.1 visually.
- All unit tests pass (user-verified in Android Studio).
