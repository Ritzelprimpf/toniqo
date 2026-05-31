# Implementation Prompt — Phase 7.2 (Shared Audio & Note Detection)

> Paste this to start a fresh implementation session for **Phase 7.2**. Phase 7.1 (the matching engine) must already be merged.

---

You are implementing **Phase 7.2 — Shared Audio & Note Detection** of Toniqo, a native Android guitar toolkit. You write code; the user owns Android Studio, the build, the emulator/device, and Git. Do not run Gradle, launch emulators, or invoke git. Propose complete file contents the user can apply.

**Before writing any code, read, in this order:**
1. `PROJECT_PLAN.md`, `CLAUDE.md`, `IMPLEMENTATION_NOTES.md`, `DECISIONS.md` (always-read set; note the Phase 7.1 entries and the Phase 5 audio-parameter / YIN-threshold entries).
2. `APP_SPECIFICATION.md` → "Module: Key Finder" and "Module: Tuner".
3. `Phase7-PLAN.md` → "Architecture & Package Notes".
4. `Phase7_2-PLAN.md` and `Phase7_2-REQUIREMENTS.md` — your scope for this session.
5. The **existing tuner audio code** (`tuner/data/` capture + `PitchDetector`/YIN) and the `Phase5_2` docs, plus `common/util/MusicTheory.kt` (`frequencyToNote`).

**Hard constraints (from `CLAUDE.md` / `IMPLEMENTATION_NOTES.md`):**
- Kotlin, minSdk 31, Jetpack Compose (Material 3), Hilt (KSP), JUnit 4 + MockK. Feature-first packages with Clean Architecture inside each; pure music theory in `common/`.
- SOLID strictly. Pure stateless utilities may be top-level `object`s; everything else constructor-injected.
- No `!!`, no logic in `init {}`, sealed types for state/results, `data class` for models.
- No magic numbers or strings (sample rate, buffer sizes, YIN threshold, confirmation windows all named). KDoc on all public APIs. Tests alongside the code.
- **Stop and ask** rather than guess if anything is ambiguous or a decision would cascade.

**Lock these three decisions at the very start of the session and record them in `DECISIONS.md` before coding:**
1. **Module location & name** — recommended: a new top-level `audio/` package (sibling to `common/` and `ui/`), not `common/audio/`. This is a deliberate deviation from the feature-first layout in `CLAUDE.md` §3; justify it (audio capture is an Android-dependent cross-feature concern that doesn't belong in pure-theory `common/`, and duplicating it would violate DRY).
2. **Move boundary** — what is *generic* and moves to `audio/` (`AudioCaptureSource` = the `AudioRecord` wrapper emitting frames; `PitchDetector` = YIN, buffer → Hz) versus what is *tuner-specific* and stays in `tuner/` (cents math, target/string selection, the 500 ms sustained-tune state machine).
3. **Detector confirmation thresholds** for Key Finder — the sustained window (suggested ~150–250 ms) and the debounce rule that makes one held note emit once.

**Your task this session:**
1. Create the top-level **`audio/`** package. **Move** (not rewrite) `AudioCaptureSource`/`AudioRecordCaptureSource` and `PitchDetector`/`YinPitchDetector` out of the tuner into it, with their existing tests relocated. Capture parameters and the YIN threshold move **unchanged**. Add an `audio/di/` Hilt module binding both interfaces; remove the tuner's old bindings for these.
2. **Re-point the tuner** to consume the `audio/` interfaces. This is a parity-preserving refactor: the tuner's existing unit-test suite is the regression gate and must pass unchanged. Do not "improve" the moved code. If a genuine behavioural change seems required, **stop and ask**.
3. Add `keyfinder/domain/repository/NoteDetector.kt`: `detectedNotes(): Flow<Int>` (pitch class 0..11), `suspend fun start()`, `suspend fun stop()`.
4. Implement `keyfinder/data/StableNoteDetectorImpl.kt` composing `AudioCaptureSource` + `PitchDetector` + `MusicTheory.frequencyToNote`: YIN → Hz → `Note` → pitch class, emitting a pitch class only after the confirmation window, debounced so a held note emits once, re-armed by silence/`null` or a pitch-class change.
5. Add the `keyfinder/di/` Hilt binding for `NoteDetector` → `StableNoteDetectorImpl`.
6. Reuse the tuner's runtime `RECORD_AUDIO` flow; if its permission helper is screen-local, lift it to a shared location (`ui/` or `audio/`) rather than duplicating — note what you did. (The Key Finder permission-denied **UI** is 7.4, not this session.)

**Tests:**
- Relocated `audio/` tests pass in their new package.
- `StableNoteDetectorImplTest` with a **fake** `AudioCaptureSource` + **fake/mock** `PitchDetector` driving scripted Hz/null sequences: a held note emits its pitch class **exactly once**; it does not re-emit until the pitch class changes or silence intervenes; a transient shorter than the window emits nothing; A → silence → A emits twice; slightly detuned input still maps to the nearest pitch class; `null` mid-confirmation resets.
- The **entire existing tuner test suite** runs green against the promoted `audio/` classes — this is an explicit acceptance item.

**When done:**
- Append the `DECISIONS.md` entries (shared `audio/` module + deviation justification; detector confirmation/debounce thresholds), dated and append-only.
- Give the user a summary that **explicitly flags the tuner refactor** and lists what they must re-verify: tuner builds, tuner tests green, tuner still tunes correctly on a real device, and no Hilt/DI errors at launch.
- Organise the proposal to map cleanly to commits (e.g. `refactor: promote audio capture and YIN to shared audio module`, `feat: add Key Finder stable-note detector with tests`). Do not commit yourself.

Confirm you have read the listed docs and the existing tuner audio code, have locked the three start-of-phase decisions, and have no blocking questions — then proceed file by file with tests.
