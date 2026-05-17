# Phase 5 — Guitar Tuner Implementation

## Goal

Implement the Guitar Tuner module fully. This is the first real module to be built. It must be functional, tested, and production-quality by the end of this phase.

## Approach

Phase 5 is divided into sub-phases for incremental implementation and testing. Each sub-phase has its own `Phase5.N-PLAN.md` and `Phase5.N-REQUIREMENTS.md`, to be written at the start of that sub-phase. Sub-phases must be completed in order — each builds on the previous.

## Sub-Phases

| Sub-Phase | Name | Description |
|---|---|---|
| 5.1 | Tuning Data & Music Theory | Implement `MusicTheory`, the preset catalog, frequency math |
| 5.2 | Pitch Detection | Implement microphone capture and the YIN pitch detector |
| 5.3 | Tuner Logic & ViewModel | Implement `DetectTunedStringUseCase`, wire detector → ViewModel |
| 5.4 | Tuner UI | Implement the full `TunerScreen` matching `DESIGN.md` §8.1 |

> Sub-phase plans must be written before each sub-phase begins. Create `Phase5.1-PLAN.md`, `Phase5.1-REQUIREMENTS.md`, etc. at the repository root as work progresses.

## Reference Material

Before starting any sub-phase, read:
- `APP_SPECIFICATION.md` → "Module: Guitar Tuner"
- `IMPLEMENTATION_NOTES.md` → "Audio" and "Music Theory Primitives"
- `DESIGN.md` → §8.1 (Tuner) and §2.4 (semantic colour mappings)
- `DECISIONS.md` → all entries

## Decisions Already Resolved (carry forward from `APP_SPECIFICATION.md`, `DESIGN.md`, and `DECISIONS.md`)

These were settled during planning and need **not** be re-decided in Phase 5.1:

- ✅ **Preset storage:** Hardcoded Kotlin constants in `tuner/data/`. Not JSON, not Room.
- ✅ **Complete preset list:** Defined exhaustively in `APP_SPECIFICATION.md` (6/7/8-string × Standard/Open/Dropped).
- ✅ **Reference pitch:** A4 = 440 Hz default, with an in-app toggle for A4 = 432 Hz.
- ✅ **In-tune tolerance:** ±5 cents, sustained for 500 ms before auto-advancing to the next string.
- ✅ **Pitch detection algorithm:** YIN. (Autocorrelation was considered; YIN is more robust to harmonics on guitar fundamentals. Record the chosen YIN threshold parameter in `DECISIONS.md` at the start of Phase 5.2.)
- ✅ **Visual design:** Needle gauge readout per `DESIGN.md` §8.1. Idle state, success state, and string selector behaviour are fully specified there.
- ✅ **Cents → colour mapping:** `DESIGN.md` §2.4 is the single source of truth.

## Decisions Still Open (resolve at the start of the relevant sub-phase)

### Phase 5.1

- [ ] How are presets exposed to the UI? Flat list, or grouped by category and string count? (Recommended: grouped, matching `APP_SPECIFICATION.md` structure.)

### Phase 5.2

- [ ] Confirm audio capture parameters: **44100 Hz sample rate, mono, PCM 16-bit**, buffer size derived from `AudioRecord.getMinBufferSize()` with a minimum floor for stable detection (likely 2048 or 4096 frames). Lock in `DECISIONS.md`.
- [ ] YIN threshold parameter (typical: 0.10–0.15). Confirm via experimentation on representative guitar audio.

### Phase 5.3

- [ ] How is the "sustained 500 ms" requirement implemented? Suggested approach: a debounced state machine inside `DetectTunedStringUseCase` that tracks consecutive in-tolerance detections and emits an `IN_TUNE_STABLE` status only after the threshold is met.
- [ ] How is auto-advance behaviour made toggleable? Default: enabled. The user should be able to disable auto-advance and switch strings manually.

### Phase 5.4

- [ ] **A4 = 432 Hz toggle UI placement.** `DESIGN.md` §14 Q1 lists this as open. Resolve before building.
- [ ] **Microphone permission-denied screen.** `DESIGN.md` §14 Q2 lists this as open. Resolve before building.

## Dependencies

No new libraries are needed for the core tuner. If a third-party pitch-detection library is considered later, it must be documented in `DECISIONS.md` with rationale before being added.

## Completion Criteria

Phase 5 is complete when all sub-phases 5.1 through 5.4 have met their individual requirements, and the Guitar Tuner module works end-to-end on a real device: the user can select a tuning, play a string near the device microphone, and receive accurate visual tuning feedback that correctly detects "in tune" within ±5 cents for at least 500 ms before advancing. The screen matches `DESIGN.md` §8.1.
