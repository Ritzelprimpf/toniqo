# Toniqo — Decisions Log

This file is **append-only**. Every non-trivial architectural or product decision is recorded here, with the date, the choice, the alternatives considered, and the rationale. Never overwrite past entries — if a decision is superseded, add a new entry that explicitly supersedes the old one and link them.

The agent must read this file at the start of every phase and consult it before introducing new patterns. If a decision needs to be made that isn't covered here, the agent must stop and ask, then record the answer here.

---

## 2026-05-17 — Project layout: feature-first with Clean Architecture inside

**Decision.** The top-level package layout under `de.ritzelprimpf.toniqo` is feature-first: `tuner/`, `metronome/`, `keyfinder/`, `chordfinder/`, plus shared `common/` and `ui/`. Each feature module internally follows Clean Architecture layering (`data/`, `domain/`, `presentation/`).

**Alternatives considered.**
- *Strict layer-first* (top-level `data/`, `domain/`, `presentation/`). Cleaner academically but, for a 4-module utility app, results in widely scattered files for any single feature.
- *Per-feature decision.* Rejected — consistency matters more than module-local preference for an app this size.

**Rationale.** With four independent, equal-weight features and no shared business workflows, feature-first localizes change: working on the tuner touches `tuner/` and nothing else. Clean Architecture's discipline is preserved *within* each module, where it matters.

---

## 2026-05-17 — Testing framework: JUnit 4 + MockK

**Decision.** Use JUnit 4 for all unit tests. Use MockK for mocking. Prefer hand-written fakes over mocks at repository boundaries.

**Alternatives considered.**
- *JUnit 5 + `de.mannodermaus.android-junit5` plugin.* Modern, parameterized tests are nicer, but instrumented tests still run on JUnit 4 — mixing both is friction with little payoff.

**Rationale.** JUnit 4 is Android's default; one framework everywhere keeps test infrastructure trivial.

---

## 2026-05-17 — Tuning preset storage: hardcoded constants

**Decision.** Tuning presets ship as hardcoded Kotlin constants in `tuner/data/` (or `domain/model/`, to be confirmed in Phase 4.1). They are not loaded from a JSON asset and not stored in Room.

**Alternatives considered.**
- *JSON asset.* Slightly easier to edit without a recompile, but adds parsing complexity and a runtime failure mode for content that never changes between releases.
- *Room database.* Justified only if user-defined presets become a feature. They are not currently in scope.

**Rationale.** The preset list is fixed at build time and small enough to type by hand. Constants compile-check correctly and need no parser.

**Supersession trigger.** If user-defined tuning presets are added, revisit and likely move to Room.

---

## 2026-05-17 — Reference pitch: A4 = 440 Hz default, 432 Hz optional

**Decision.** Default reference pitch is A4 = 440 Hz. A toggle in tuner settings allows switching to A4 = 432 Hz. The full chromatic scale is derived from the chosen reference.

**Rationale.** 440 Hz is the international standard; 432 Hz is requested often enough by certain communities to justify the toggle but rare enough that it should not be the default.

---

## 2026-05-17 — In-tune tolerance: ±5 cents over 500 ms

**Decision.** The tuner considers a string "in tune" when the detected pitch is within ±5 cents of the target for at least 500 ms of continuous detection. Only then does the UI advance to the next string.

**Alternatives considered.**
- *±2 cents.* More accurate but unreliable given microphone noise and string transients.
- *Instant advance (no sustained-tone requirement).* Causes spurious advances on transients.

**Rationale.** ±5 cents is below the human threshold for noticeable pitch error in this context; the 500 ms hold filters out transients without feeling sluggish.

---

## 2026-05-17 — Minimum SDK: 24

> **⚠️ Superseded by the 2026-05-17 (later) entry below.** Original rationale preserved for history.

**Decision.** `minSdk = 24` (Android 7.0 Nougat). `targetSdk = 34` (or current stable at implementation time).

**Alternatives considered.**
- *Earlier minSdk.* Jetpack Compose requires minSdk 21 absolute floor; 24 gives reliable `AudioRecord`, modern coroutines, and stable Hilt behavior without supporting devices that are effectively unused by the target audience (intermediate/advanced guitarists in 2026).

**Rationale.** An earlier draft of `IMPLEMENTATION_NOTES.md` said minSdk 12 — that was a typo. API 12 is Android 3.1 (2011), incompatible with the entire chosen stack.

---

## 2026-05-17 (later) — Minimum SDK: 31 (supersedes minSdk 24)

**Decision.** `minSdk = 31` (Android 12). `targetSdk = 34` (or current stable). This supersedes the earlier minSdk 24 decision logged above.

**Alternatives considered.**
- *minSdk 24 (Android 7.0).* Maximum reach but covers many devices that are no longer realistic for the target audience.
- *minSdk 26 (Android 8.0).* Sweet spot between reach and modern API access. Unlocks `java.time` and `AudioRecord.Builder`.
- *minSdk 31 (Android 12).* Chosen. Reduces device support but unlocks Material You dynamic color, the native Splash Screen API, and the most predictable modern audio behavior.

**Rationale.** The original requirement was "Android version 12," initially misread as SDK level 12. The user clarified that Android 12 (API 31) is the intended floor. The target audience — intermediate-to-advanced guitarists buying a dedicated tuner/metronome app in 2026 — is not running pre-2021 hardware in meaningful numbers, so the trade is acceptable.

**Consequences.**
- Material You dynamic color may be considered for the theme in Phase 3 (optional, not required).
- The Splash Screen API can be used directly via `androidx.core.splashscreen` without backports.
- `AudioRecord` and `AudioTrack` paths simplify — no compatibility shims needed.

---

## 2026-05-17 (later) — Human/Agent split: user owns the IDE, build, runtime, and Git

**Decision.** The agent does not run Gradle, launch emulators, or invoke `git`. The user is the only party with hands on Android Studio. The agent produces code changes in chat; the user applies them, runs builds and tests in the IDE, and commits.

**Alternatives considered.**
- *Agent runs `./gradlew` locally to "verify" builds.* Rejected — Android Studio bundles its own JDK and toolchain; a separate Gradle invocation can pick a different JDK and produce misleading results, and it doesn't catch IDE-specific problems anyway.
- *Agent commits via Git.* Rejected — the user wants explicit control over what enters history, particularly for a side project where commit shape matters.

**Rationale.** The user is reviewing, testing, debugging, and sometimes implementing features themselves. The agent's role is collaborator, not autonomous builder. Splitting responsibilities cleanly — agent writes, user verifies — avoids the agent claiming "the build passes" based on a build that doesn't reflect the user's actual environment.

**Consequences.**
- Phase completion criteria split into an agent-side checklist (code is present and correct) and a user-side checklist (Gradle syncs, build succeeds, tests pass, app runs).
- Phase 1 is reframed from "create a project" to "adapt the existing Android Studio template." The template was the **Empty Activity (Compose)** template with package `de.ritzelprimpf.toniqo` and Kotlin DSL.
- The agent suggests commit messages but does not commit.

---

## 2026-05-17 (later) — Design system locked in via `DESIGN.md`

**Decision.** Visual design, including the full token system, type scale, component primitives, module-specific screen specs, motion, and accessibility behaviours, is locked in `DESIGN.md`. The design system is implemented in code during Phase 3 (Design Tokens) and consumed by every subsequent phase.

**Alternatives considered.**
- *Design as we build, screen by screen.* Rejected — every screen would re-litigate colour, type, and spacing decisions.
- *Defer design system until first real module.* Rejected — placeholder screens in the navigation shell would be built against Material defaults and rebuilt later.

**Rationale.** Tokens are the foundation; everything visual depends on them. Locking the system and implementing it once before any screen work avoids rework and ensures consistency across modules.

**Consequences.**
- `DESIGN.md` is authoritative for appearance. `APP_SPECIFICATION.md` is authoritative for behaviour. Conflicts must be raised, not improvised around.
- The `Tq` token object (`ui/theme/Tq.kt`) is the only place `Color(0xFF...)`, font sizes, spacings, and radii are defined. Every other file consumes the tokens.
- Two fonts (Space Grotesk, JetBrains Mono) are bundled locally; no Downloadable Fonts.
- Material You dynamic colour is off for v1 — static brand palette only.

---

## 2026-05-17 (later) — Phase renumbering: design tokens become Phase 3

**Decision.** The implementation phases are reordered so that the design system is implemented before any UI screens are built. The new order:
- Phase 1: Project Setup (unchanged)
- Phase 2: Backend Outline (unchanged)
- Phase 3: Design Tokens (new — was part of old Phase 3)
- Phase 4: Navigation Shell & Placeholders (was the rest of old Phase 3)
- Phase 5: Guitar Tuner (was Phase 4; internal sub-phases renumbered 4.1–4.4 → 5.1–5.4)

**Alternatives considered.**
- *Insert "Phase 3.5" between the old Phase 3 and Phase 4.* Rejected — the old Phase 3 already mixed navigation with theme work; splitting it is cleaner than wedging in a half-step.
- *Build tokens inside the old Phase 3 alongside placeholders.* Rejected — the user explicitly wanted tokens isolated as their own phase.

**Rationale.** Cleaner phase boundaries. Phase 3 produces a working theme that any screen can consume; Phase 4 produces a navigation shell that uses that theme; Phase 5 onward produces real screens. Each phase has a single, focused output.

**Consequences.**
- All later phase numbers shift by one. The remaining-modules plan (old Phase 5+) becomes Phase 6+.
- The sub-phase numbering inside the Guitar Tuner phase shifts: old 4.1–4.4 are now 5.1–5.4.
- `Phase4-PLAN.md` no longer covers the Guitar Tuner — that's now in `Phase5-PLAN.md`. The file's contents have been entirely replaced.

---

## 2026-05-17 (later) — Tuner supports both sequential and chromatic modes from Phase 5

**Decision.** The Guitar Tuner ships with two operating modes in its first release: a **sequential mode** (start at the lowest string, advance string-by-string as each is brought in tune) and a **chromatic mode** (also called "free mode" — the app continuously identifies which string of the current tuning is closest to the played note and shows cents-off relative to that target). Both modes are implemented in Phase 5 and switchable in the UI. After all strings are tuned in sequential mode, the tuner automatically transitions to chromatic mode so the user can fine-tune.

**Alternatives considered.**
- *Sequential mode only in v1, chromatic mode in a later phase.* Rejected — the underlying pitch-detection and cents-conversion machinery is identical for both modes; only the "which target to compare against" logic differs. Deferring chromatic mode would mean revisiting the use case and ViewModel later to retrofit it, which is more work than implementing both at once.
- *Chromatic mode only.* Rejected — sequential mode gives clear progress feedback for users tuning a guitar from scratch and is the more discoverable default.

**Rationale.** Chromatic mode is the standard fine-tuning workflow on every real tuner and a real user need, not a stretch feature. The marginal cost to add it in Phase 5 is small; the cost to retrofit later is non-trivial. Sequential mode remains the default entry point because it's more guided.

**Consequences.**
- Phase 5.3 implements both modes within `DetectTunedStringUseCase` (or a renamed equivalent), and `TunerUiState` carries a `TunerMode` field.
- The UI surface for switching modes is decided in Phase 5.4 (likely a small toggle near the string selector).
- The `ALL_STRINGS_TUNED` status from `Phase2-REQUIREMENTS.md` triggers the success animation and then transitions the mode automatically rather than being a terminal state.

---

## 2026-05-17 (later) — Reference-pitch toggle UI placement: sun-icon button → settings sheet

**Decision.** The A4 = 440 / 432 Hz toggle is exposed via a sun-icon button in the top-right of the Tuner screen (the icon already present but unbound in the mockup). Tapping the icon opens a small settings sheet from the bottom containing the toggle, plus any future tuner-scoped settings.

**Alternatives considered** (all from `DESIGN.md` §14 Q1):
- *(a) Long-press the `A4 = 440 HZ` kicker line itself.* Rejected — long-press is undiscoverable for a setting users actively look for.
- *(b) A small inline toggle button next to the preset chip.* Rejected — adds visual noise to the most-used area of the screen for a setting most users never change.
- *(c) Sun-icon button → settings sheet.* Chosen.

**Rationale.** The sun icon is already in the mockup and is the most discoverable affordance. A settings sheet scales: 432 Hz toggle today, auto-advance toggle tomorrow, anything else later — all in one place that doesn't clutter the main readout.

**Consequences.**
- Phase 5.4 implements the sun-icon button, the bottom sheet, and the toggle inside it.
- The reference pitch value is read by the ViewModel; changing it updates all target frequencies for the current tuning live.

---

## 2026-05-17 (later) — Microphone permission-denied state: standard single-card screen

**Decision.** When `RECORD_AUDIO` is denied, the Tuner screen displays a single `ToniqoCard` containing: a microphone icon (24dp), a short two-line explanation of why the permission is needed, and a primary-styled "Grant access" button that opens system app settings via `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`. The rest of the Tuner UI (preset chip, readout well, string selector) is not rendered.

**Alternatives considered** (all from `DESIGN.md` §14 Q2):
- *Inline banner above the readout.* Rejected — leaves a non-functional readout visible, which is confusing.
- *Modal dialog.* Rejected — feels like a system pop-up and lets the user dismiss it back into a non-functional screen.
- *Single-card screen.* Chosen.

**Rationale.** Without microphone access the tuner does nothing. The screen should communicate that clearly and give the user one obvious next action. A single card is consistent with the rest of the design system and doesn't require new component primitives.

**Consequences.**
- Phase 5.4 implements the permission-denied state.
- The button uses the existing `btn.primary` style from `DESIGN.md` §6.1 (without the 24dp glow — that's reserved for the metronome Start button per §10).
- The screen also handles the "permanently denied" case (where the system permission dialog can no longer be shown) by opening app settings rather than re-requesting.

---

## 2026-05-17 (later) — Tuner preset catalog: modern-metal additions

**Decision.** The preset catalog defined in `APP_SPECIFICATION.md` is extended with four modern metal tunings for Phase 5 coverage:
- 6-string: Drop A#/Bb (A#1, F2, A#2, D#3, G3, C4)
- 7-string: Drop F# (F#1, C#2, F#2, B2, E3, G#3, C#4)
- 8-string: Drop C# (C#1, G#1, C#2, F#2, B2, E3, G#3, C#4)
- 8-string: Drop C (C1, G1, C2, F2, A#2, D#3, G3, C4)

**Alternatives considered.**
- *Leave the catalog as-is.* Rejected — modern metal is part of the target audience and these tunings are not exotic outliers in their respective scenes.
- *Add more (e.g., Open B, Open F on 6-string).* Rejected — those tunings exist but are rare enough that they add scroll burden without proportional value.

**Rationale.** Four targeted additions extend coverage to genuinely-used modern metal tunings without ballooning the list. The total catalog stays at roughly 30 presets, which is the UX comfort ceiling.

**Consequences.**
- The total count of presets ships at ~30 — still tractable in a grouped picker.
- Drop C (8-string) tunes the lowest string to C1 (≈32.7 Hz), which is at the boundary of reliable pitch detection on a phone microphone. Phase 5.2 must validate detection on this pitch with real audio; if unreliable, document the limitation in the help section rather than removing the preset.

---

## 2026-05-19 — Phase 5.1: diatonic seventh-chord harmonization

**Decision.** Each scale degree gets its musically correct diatonic seventh quality. For C Ionian: `[Cmaj7, Dm7, Em7, Fmaj7, G7, Am7, Bm7♭5]`. Fully-diminished sevenths (`dim7`) are included in `ChordQuality` for completeness but do not appear in standard diatonic harmonization.

**Alternatives considered.**
- *Triads only (no sevenths in Phase 5.1).* Rejected — the Chord Finder spec includes a seventh-chord toggle; building the logic now avoids a future retrofit.
- *Context-aware seventh spelling (e.g. B° with a true diminished seventh A♭).* Rejected for Phase 5.1 — deferred to `FUTURE_IMPROVEMENTS.md` because it requires key-aware enharmonic resolution.

**Rationale.** Diatonic sevenths are the standard reference for the Chord Finder module. Landing them in 5.1 alongside the music-theory primitives means Phases 6+ can use `MusicTheory.buildSeventhChords` without modification.

---

## 2026-05-19 — Phase 5.1: frequencyToNote defaults to sharp spelling

**Decision.** `MusicTheory.frequencyToNote()` always returns accidentals in sharp form (e.g. `C#4` rather than `Db4`). The return type is `Note?` (nullable); `null` is returned for frequencies outside the valid range or that are non-musical inputs (≤ 0, NaN, Infinity, pitches outside C0–B9).

**Alternatives considered.**
- *Context-aware spelling* (choose flat or sharp based on the surrounding key). Rejected for Phase 5.1 — deferred to `FUTURE_IMPROVEMENTS.md`. Requires knowing the musical key at call time, which the pitch-detection path does not have.
- *Always flat.* No preference; sharp was chosen arbitrarily as the common default for chromatic displays.

**Rationale.** The tuner needs a consistent, key-independent spelling for its chromatic readout. Sharp is the more common convention on real-world tuners.

**Supersession trigger.** If the Key Finder or Chord Finder need enharmonic-correct display, revisit at that point.

---

## 2026-05-19 — Phase 5.1: centsBetween added to MusicTheory

**Decision.** `MusicTheory.centsBetween(referenceFrequencyHz, detectedFrequencyHz): Double` is added as a new method. Formula: `1200 × log₂(detected / reference)`. Positive = sharp, negative = flat.

**Rationale.** Phase 5.3 (Tuner ViewModel) needs this computation on every detection cycle. Landing it alongside the other frequency math in 5.1 keeps all music-theory arithmetic in one tested place and avoids Phase 5.3 having to duplicate it.

---

## 2026-05-19 — Phase 5.1: getPresetsGrouped added to TunerPresetRepository

**Decision.** `TunerPresetRepository` gains a third method: `suspend fun getPresetsGrouped(): Map<Int, Map<TunerCategory, List<TunerPreset>>>`. The outer key is string count (6, 7, 8); the inner key is `TunerCategory`.

**Rationale.** The preset picker UI (Phase 5.4) renders presets in a categorized list. Pre-grouping in the repository keeps the grouping logic co-located with the data and avoids repeating it in the ViewModel. The `suspend` keyword matches the other two methods for interface consistency.

---

## 2026-05-19 — Phase 5.1: Scale constructor changed — intervals field removed

**Decision.** `Scale` changes from `Scale(root: Note, intervals: List<Interval>)` to `Scale(root: Note, mode: Mode)`. The `notes: List<Note>` property is now derived from `root` and `mode.intervalsFromRoot` at construction time. The `intervals` field is gone.

**Alternatives considered.**
- *Keep intervals as a separate field alongside mode.* Rejected — intervals are fully determined by mode; carrying both is redundant and creates a consistency hazard.
- *Keep Scale taking an arbitrary interval list.* Rejected — the only legitimate source of interval patterns is `Mode`. Encoding this directly makes Scale safer.

**Rationale.** Mode already carries the canonical interval pattern. Passing the same data twice as separate arguments was a Phase 2 stub artefact. This change also unifies the derivation path: `Scale.notes` is always the result of applying `mode.intervalsFromRoot` to `root`.

**Consequences.** All Phase 2 call sites that used `Scale(root, intervals = Mode.X.intervalsFromRoot)` are updated to `Scale(root, Mode.X)`. Test files updated accordingly.

---

## 2026-05-19 — Phase 5.1: Chord constructor changed — explicit notes field removed

**Decision.** `Chord` changes from `Chord(root: Note, quality: ChordQuality, notes: List<Note>)` to `Chord(root: Note, quality: ChordQuality)`. The `notes: List<Note>` property is now derived from `root` and `quality.intervalsFromRoot` at construction time.

**Alternatives considered.**
- *Keep notes as a constructor parameter* to allow custom voicings (inversions, doublings). Rejected — voicing is not modelled in Phase 5 or 6; the extra parameter invited incorrect usage where callers could pass notes inconsistent with the quality.

**Rationale.** Chord quality fully determines which pitch classes appear in the chord. Deriving notes from the quality removes the possibility of constructing a logically inconsistent Chord, and simplifies all call sites.

**Consequences.** `MusicTheory.buildTriads` and `buildSeventhChords` now simply pass the root note and classified quality; note lists are derived automatically. All Phase 2 call sites updated. `Chord.displayName()` uses `root.name.sharpName` (not `root.displayName()`) so chord symbols omit the octave number (e.g. `"Cmaj7"`, not `"C4maj7"`).

---

## 2026-05-19 — Phase 5.2: Audio capture parameters locked in

**Decision.** Microphone capture uses **44 100 Hz sample rate, mono (CHANNEL_IN_MONO), PCM 16-bit (ENCODING_PCM_16BIT)**. The buffer size in frames is `max(4096, AudioRecord.getMinBufferSize() / 2)`, guaranteeing a minimum of 4096 frames regardless of hardware.

**Alternatives considered.**
- *48 000 Hz.* The Android preferred rate; slightly better frequency resolution at the cost of slightly higher CPU. Rejected — 44 100 Hz is standard for music, and the difference is negligible for guitar pitch detection.
- *Smaller buffer (2048 frames).* Lower latency but higher risk of detection noise on low-frequency strings. Rejected.

**Rationale.** 44 100 Hz gives sub-cent frequency resolution across the full guitar range. 4096 frames is large enough to hold roughly 3–4 periods of E2 (82 Hz), which is the minimum needed for reliable YIN detection.

---

## 2026-05-19 — Phase 5.2: Audio source preference — UNPROCESSED preferred, MIC fallback

**Decision.** `MicrophoneAudioSourceImpl` first attempts `MediaRecorder.AudioSource.UNPROCESSED`. If the recorder is not in `STATE_INITIALIZED` after that attempt, it falls back to `MediaRecorder.AudioSource.MIC`. The source actually opened is reported in `CaptureEvent.Listening` and logged at INFO level.

**Alternatives considered.**
- *MIC only.* Simpler, but MIC applies AGC and noise reduction on many devices, which can distort the waveform and degrade YIN accuracy.
- *VOICE_RECOGNITION.* Targets speech-optimized processing; not appropriate for musical pitch.

**Rationale.** UNPROCESSED delivers the raw microphone signal — exactly what YIN needs. On devices that do not support it (STATE_UNINITIALIZED), falling back to MIC is the correct graceful degradation.

---

## 2026-05-19 — Phase 5.2: YIN threshold — 0.15

**Decision.** The YIN cumulative-mean-normalized-difference threshold is **0.15**, the value recommended by de Cheveigné & Kawahara (2002). Stored as `YinConfig.DEFAULT_THRESHOLD`.

**Alternatives considered.**
- *0.10.* More strict — fewer spurious detections but misses quiet or slightly inharmonic notes. Rejected as too aggressive for the use case.
- *0.20.* More permissive — picks up more notes but risks false detections on noise. Rejected.

**Rationale.** 0.15 is the paper's own recommendation and the empirical baseline across YIN implementations. The frequency-range guard (`absoluteMinFrequencyHz = 30 Hz`, `absoluteMaxFrequencyHz = 2000 Hz`) provides an additional safety net against edge-case false positives.

**Supersession trigger.** End-to-end real-device testing in Phase 5.4. If detection is unreliable on specific strings or tunings, the threshold (or the range guards) may be tuned and a new entry added.

---

## 2026-05-19 — Phase 5.2: MicrophoneAudioSource API shape — Flow<CaptureEvent>, no start/stop

**Decision.** `MicrophoneAudioSource` exposes a single `fun samples(): Flow<CaptureEvent>` method. There are no `start()` / `stop()` methods and no mutable state on the interface or the implementation. The `AudioRecord` lifecycle is entirely contained within the `callbackFlow` body; it is created on collection and released on cancellation.

**Alternatives considered.**
- *start()/stop() lifecycle methods.* Stateful lifecycle management. Rejected — makes the source harder to test and introduces the possibility of calls out of order (start/start, stop before start, etc.).
- *SharedFlow with a shared AudioRecord.* Allows multiple collectors to share one recorder. Rejected — adds complexity and was not needed for Phase 5.

**Rationale.** A cold `Flow` with no external lifecycle is the simplest, most composable shape. The coroutine scope of the collector is the lifecycle; cancelling the scope releases audio resources with no extra API surface.

---

## 2026-05-19 — Phase 5.2: callbackFlow for AudioRecord lifetime safety

**Decision.** `MicrophoneAudioSourceImpl.samples()` is implemented using `callbackFlow { ... }.flowOn(Dispatchers.IO)`. The `AudioRecord` is wrapped in a `try/finally` block inside the flow body; `record.stop()` and `record.release()` are called unconditionally in `finally`, ensuring release even when the collector cancels or an exception is thrown.

**Alternatives considered.**
- *`flow { }` + `launch { }` + `Channel`.* More boilerplate for the same semantics; `callbackFlow` is the idiomatic Kotlin coroutines solution for callback/blocking-I/O sources.
- *`produce { }` coroutine builder.* Experimental API; `callbackFlow` is stable.

**Rationale.** `callbackFlow` was designed for exactly this pattern: wrapping a blocking or callback-based source as a coroutine flow, with `awaitClose` guaranteeing cleanup. The `try/finally` around the read loop adds an explicit inner safety net so cleanup is not solely dependent on `awaitClose` being reached.

---

## 2026-05-19 — Phase 5.2: AudioPermissionChecker abstraction in common/permission/

**Decision.** A `AudioPermissionChecker` interface is placed in `common/permission/`. The Android implementation (`AndroidAudioPermissionChecker`) uses `ContextCompat.checkSelfPermission` with the application context and is bound via `CommonModule`. The interface is used by `MicrophoneAudioSourceImpl` rather than calling `ContextCompat` directly.

**Alternatives considered.**
- *Call `ContextCompat.checkSelfPermission` directly inside `MicrophoneAudioSourceImpl`.* Simpler, but makes the permission-denied path impossible to test without a real Android context.
- *Put the interface in `tuner/` instead of `common/`.* Would be acceptable for a single-module use; `common/` was chosen because other modules may eventually need the same check (e.g., a future voice-input Key Finder).

**Rationale.** The abstraction is essential for the only testable path in `MicrophoneAudioSourceImpl` (the permission-denied branch). Placing it in `common/` keeps the door open for reuse.

---

## 2026-05-19 — Phase 5.2: PitchDetector interface moved to common/util/

**Decision.** The `PitchDetector` interface is moved from `tuner/domain/repository/PitchDetector.kt` to `common/util/PitchDetector.kt`. The Phase 2 stub `YinPitchDetector` in `tuner/data/` is deleted; the full implementation lives in `common/util/YinPitchDetector.kt`.

**Alternatives considered.**
- *Keep the interface in `tuner/domain/repository/` and only move the implementation.* Acceptable, but the interface and its sole concrete implementation would live in different top-level packages, which is more confusing than useful.

**Rationale.** The YIN implementation is pure Kotlin with no Android dependencies, making it naturally a `common/util/` resident. The interface belongs alongside its implementation. If a future module needs pitch detection (e.g., for a vocal tuner), it can depend on `common/util/PitchDetector` without coupling to the tuner module.

**Consequences.** All import sites updated: `TunerModule`, `DetectTunedStringUseCase`. The old `tuner/domain/repository/PitchDetector.kt` file is deleted.

---

## 2026-05-20 — Phase 5.3: TuningStatus gains PERMISSION_DENIED and CAPTURE_FAILED

**Decision.** The `TuningStatus` enum is extended with two new values: `PERMISSION_DENIED` (capture blocked by missing permission) and `CAPTURE_FAILED` (non-permission hardware failure). Both are surfaced from `DetectTunedStringUseCase` so the ViewModel can map them into observable UI state.

**Rationale.** Phase 2 defined `TuningStatus` with only the happy-path values. Phase 5.3 is the first sub-phase that wires the audio pipeline end-to-end; `MicrophoneAudioSource` can emit `PermissionDenied` and `Failed` events and the ViewModel must have corresponding status values to surface them in UI state. Keeping them in `TuningStatus` (rather than a separate error enum) keeps the state model simple — one field covers every screen state.

---

## 2026-05-20 — Phase 5.3: TunerUiState reshaped for the full pipeline

**Decision.** `TunerUiState` is significantly expanded from the Phase 2 placeholder. New fields: `mode: TunerMode`, `availablePresets: Map<Int, Map<TunerCategory, List<TunerPreset>>>`, `targetNote: Note?`, `targetFrequencyHz: Double?`, `detectedNote: Note?`, `centsOffTarget: Double?`, `tunedStringIndices: Set<Int>`, `referencePitchHz: Double`. The old `availablePresets: List<TunerPreset>` (flat list) is replaced by the grouped map; `centsOffTarget: Float?` becomes `Double?` to match the precision of `MusicTheory.centsBetween`.

**Rationale.** The Phase 2 placeholder held only the minimal fields for an idle state. The real ViewModel needs all detection results, mode tracking, and progress state in one observable snapshot to drive the UI in Phase 5.4 without additional plumbing.

---

## 2026-05-20 — Phase 5.3: Sustained-tone window — size 6, threshold 5, 1-glitch budget

**Decision.** The sustained-tone window is a sliding `ArrayDeque<Boolean>` of capacity 6. A string is considered "in tune" when **the window is full AND at least 5 of the 6 entries are `true`** (within ±5 cents). This gives a 1-glitch-per-window budget: a single missed or out-of-tolerance detection does not reset progress.

**Alternatives considered.**
- *Strict 6-of-6.* No glitch budget — one transient bad frame drops back to FLAT/SHARP. Too fragile for real microphone input.
- *5-of-5 (smaller window).* Less glitch tolerance; the window fills faster. Rejected in favour of a larger window with a 1-slot budget.

**Rationale.** Two consecutive out-of-tolerance or null detections leave at most 4/6 `true` entries, which cannot satisfy the ≥5 threshold. This "two nulls reset" behaviour falls naturally out of the arithmetic with no explicit reset logic. The 6-element window with a 5-of-6 threshold balances responsiveness with noise immunity.

**Constants.** `SUSTAINED_WINDOW_SIZE = 6`, `SUSTAINED_MIN_IN_TOLERANCE = 5`, `IN_TUNE_TOLERANCE_CENTS = 5.0` (unchanged from DECISIONS.md 2026-05-17).

---

## 2026-05-20 — Phase 5.3: Auto-advance hold durations — 200 ms and 1200 ms

**Decision.** Two hold durations are used:
- `STRING_LOCK_HOLD_MS = 200` ms between a string locking in-tune and advancing to the next string. Gives the user a moment to see the in-tune state before the screen changes.
- `ALL_TUNED_HOLD_MS = 1200` ms between all strings tuned and the chromatic-mode transition. Matches the success-state animation duration from `DESIGN.md` §8.1.

Both are implemented as `viewModelScope.launch { delay(…) }` and are fully cancelable: any user action (preset change, string tap) during a hold cancels the pending job immediately.

**Rationale.** 200 ms is below the threshold of "feels slow" and above the threshold of "blink and miss it." 1200 ms is dictated by the design system's success ring animation duration.

---

## 2026-05-20 — Phase 5.3: Default first-launch preset — six_string_standard_e

**Decision.** On first launch (no saved preset ID in DataStore), the tuner defaults to `six_string_standard_e` (E Standard, 6-string). If the saved ID no longer exists in the catalog (e.g. after a catalog update), the fallback is also `six_string_standard_e`.

**Rationale.** Standard E tuning is by far the most common guitar tuning. A new user who just installed the app will almost certainly start here.

---

## 2026-05-20 — Phase 5.3: DataStore dependency added

**Decision.** `androidx.datastore:datastore-preferences` version 1.1.1 is added as an implementation dependency. It persists `TunerPreferences` (currently one key: `last_used_preset_id`).

**Alternatives considered.**
- *SharedPreferences.* Synchronous and not coroutine-native. DataStore is the modern replacement.
- *Room.* Overkill for a single string value.

**Rationale.** DataStore is the recommended replacement for SharedPreferences on modern Android, it integrates cleanly with coroutines via `Flow`, and it avoids the ANR risk of synchronous disk I/O on the main thread.

---

## 2026-05-20 — Phase 5.3: Two-mode operation (TunerMode.PRESET / CHROMATIC)

**Decision.** The `TunerMode` enum has two values: `PRESET` and `CHROMATIC`. In `PRESET` mode the use case compares each detection against the current string's preset note. In `CHROMATIC` mode the use case resolves the target per-frame via `MusicTheory.frequencyToNote(detectedHz, referencePitchHz)`. The sustain window is maintained in both modes, but auto-advance only applies in `PRESET` mode.

(This formalises the two-mode decision from 2026-05-17 at the code level. See that entry for the product rationale.)

---

## 2026-05-20 — Phase 5.3: Re-entry rules from chromatic to preset mode

**Decision.** Two user actions re-arm `PRESET` mode:
1. **Preset tap** (including re-tapping the current preset): mode = PRESET, currentStringIndex = 0, tunedStringIndices = emptySet(). The auto-advance sweep restarts from the first string.
2. **String pill tap**: mode = PRESET, currentStringIndex = tapped index, tunedStringIndices = emptySet(). Auto-advance proceeds from the tapped string onward.

In both cases `tunedStringIndices` is cleared — the user is starting a new sweep, not continuing a previous one. Previously-tuned strings are not preserved.

**Rationale.** String selection implies the user wants to re-tune from a specific point; carrying over a stale `tunedStringIndices` set would produce incorrect check marks. Resetting is the clearest semantic.

---

## 2026-05-20 — Phase 5.4: A4 = 432 Hz toggle UI placement

**Decision.** The A4 = 440 / 432 Hz toggle is exposed via a `settings`-icon button in the top-right corner of the Tuner screen. Tapping it opens a small settings sheet containing the toggle alongside any future tuner-scoped settings.

**Alternatives considered.**
- *(a) Long-press the `A4 = 440 HZ` kicker line itself.* Rejected — long-press is undiscoverable.
- *(b) A small inline toggle button next to the preset chip.* Rejected — adds noise to the most-used area for a rarely-changed setting.

**Rationale.** The settings button is already present in the mockup (previously unbound). A settings sheet scales gracefully — the 432 Hz toggle today, anything else later — without cluttering the main readout.

**Consequences.** Phase 5.4 implements the settings-icon button, the bottom sheet, and the segmented control inside it. Resolves `DESIGN.md` §14 Q1.

---

## 2026-05-20 — Phase 5.4: Microphone permission-denied screen design

**Decision.** When `RECORD_AUDIO` is denied, the readout-well area is replaced with a single `ToniqoCard` containing: a 28dp `mic` icon with a diagonal slash overlay, a short two-line explanation, and a primary-styled "Grant access" button. The preset chip and string selector remain visible.

**Alternatives considered.**
- *Inline banner above the readout.* Rejected — leaves a non-functional readout visible.
- *Modal dialog.* Rejected — lets the user dismiss into a non-functional screen.

**Rationale.** Without microphone access the tuner does nothing; the screen should communicate that clearly with one obvious next action. A single card is consistent with the design system and avoids new component primitives. Resolves `DESIGN.md` §14 Q2.

**Consequences.** The "Grant access" button requests permission on first tap; after permanent denial (detected via `shouldShowRequestPermissionRationale` + `hasRequestedAudioPermission` flag), it opens `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`.

---

## 2026-05-20 — Phase 5.4: Mode toggle UI placement

**Decision.** A `DropdownMenu` anchored to the preset chip's chevron (`▾`) provides two items — "Preset" and "Chromatic" — with a check mark on the current mode. The preset picker opens from the chip's label area (left side). Both tap targets are ≥ 44 × 44 dp via invisible padding.

**Alternatives considered.**
- *Top-bar segmented control.* Adds a permanent row of chrome for a feature many users never change.
- *Settings sheet item.* Buried; switching modes mid-session would require opening a sheet.

**Rationale.** Gives the mockup chevron an explicit meaning, keeps the chip's two affordances in one visual element, and avoids adding a persistent row to the screen layout.

---

## 2026-05-20 — Phase 5.4: Chromatic re-entry policy

**Decision.** When the user explicitly enters chromatic mode via the popover, the ViewModel captures `currentStringIndex` into a private field `previousPresetStringIndex: Int?`. When the user taps "Preset" in the popover from chromatic mode, the ViewModel restores that string index. `tunedStringIndices` is not restored. The snapshot is cleared on: auto-success transition, `onPresetSelected`, and `onStringSelected`.

**Alternatives considered.**
- *Always restore including check marks.* Rejected — makes chromatic mode a saved-state mechanism, which is not the intended use.
- *Always restart at string 0.* Rejected — loses the user's position when they "peek" at chromatic mode.

**Rationale.** The "I'm peeking" intent: restore position but not progress. Minimal snapshot surface area.

---

## 2026-05-20 — Phase 5.4: Auto-advance toggle persistence

**Decision.** The auto-advance toggle lives in the settings sheet and is persisted via `TunerPreferences.autoAdvanceEnabled` (default `true`). When disabled, `TunerEvent.StringTuned` still fires (haptic + check mark), but `currentStringIndex` is not incremented.

**Alternatives considered.**
- *Session-only toggle.* Rejected — users who prefer manual advance don't want to re-set it every launch.
- *On-screen toggle (not in sheet).* Rejected — would add a permanent element to the main readout area.

**Rationale.** Auto-advance preference is sticky; persisting it matches user expectation and adds a natural second tenant to the settings sheet.

---

## 2026-05-20 — Phase 5.4: Reference pitch persistence

**Decision.** Reference pitch is persisted via `TunerPreferences.referencePitchHz` (default `440.0`). Changing it in the settings sheet retunes all targets live (the ViewModel re-emits the active `tunerInput` immediately).

**Alternatives considered.**
- *Session-only.* Rejected — 432 Hz users expect it to stick.

**Rationale.** Same motivation as auto-advance: a sticky preference that belongs in `TunerPreferences`.

---

## 2026-05-20 — Phase 5.4: Preset picker surface

**Decision.** A Material 3 `ModalBottomSheet` with a segmented control (6-string / 7-string / 8-string) and grouped category sections (STANDARD / OPEN / DROPPED). Each row shows the display name and a note-list summary. The selected preset has a mint indicator dot. Tapping a row dismisses the sheet and calls `onPresetSelected`.

**Alternatives considered.**
- *Full-screen route.* Adds navigation complexity and breaks the "one active module" feel.
- *Inline carousel.* Scales poorly to 30+ presets.

**Rationale.** Standard Material 3 affordance that scales to the catalog size without navigation cost and matches the dark surface system.

---

## 2026-05-20 — Phase 5.4: SharedFlow event collection pattern

**Decision.** Events are collected in the screen via:
```kotlin
LaunchedEffect(viewModel.events, lifecycleOwner) {
    lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.events.collect { event -> ... }
    }
}
```

**Alternatives considered.**
- `LaunchedEffect(Unit)` — restarts on composition, potentially replaying after rotation.
- `collectAsStateWithLifecycle` — wrong shape for one-shot events.

**Rationale.** `repeatOnLifecycle(STARTED)` pauses collection on `STOPPED` and resumes on `STARTED` without re-running the outer `LaunchedEffect`, preventing duplicate haptics on rotation and invisible event consumption while backgrounded.

---

## 2026-05-20 — Phase 5.4: Needle gauge implementation

**Decision.** Pure Compose `Canvas`, all elements drawn per-frame (sweep arc, ticks, sweet-spot arc, needle, pivot cap). The needle glow uses `drawIntoCanvas { canvas.nativeCanvas }` with `android.graphics.Paint` and `BlurMaskFilter(6.dp.toPx(), NORMAL)`.

**Alternatives considered.**
- *Pre-rendered `GraphicsLayer` for static elements.* Profile-driven optimisation; not pre-emptive.
- *SVG asset.* Static; cannot animate the needle.

**Rationale.** Simplest correct implementation. Performance at tuner redraw rates (~10–30 fps) is not a concern; optimisation is deferred to profiling.

---

## 2026-05-20 — Phase 5.4: DESIGN.md §8.1 prose corrections

**Decision.** The detected-note hero letter has no glow. The mockups are authoritative — they show a flat white letter. The needle's 6dp drop-shadow glow (already specified in §8.1) is the only glow on the tuner screen.

**Alternatives considered.**
- *Add a text-glow exception to §10.* Rejected — mockups don't show it; no exception needed.

**Rationale.** Mockups override prose when they conflict. The §10 flat-design rule is preserved.

---

## 2026-05-20 — Phase 5.4: Sun-icon substitution

**Decision.** The `settings` glyph from the §7 icon set is used for the tuner's top-right button. No one-off `sun` SVG is introduced.

**Alternatives considered.**
- *Add a `sun` icon to the set.* Would require a new asset outside the scope of Phase 5.4.

**Rationale.** Stays within §7 icon-set discipline. If a dedicated sun is desired, add it as a new §14 question.

---

## 2026-05-20 — Phase 5.4: Compose UI test infrastructure

**Decision.** Compose UI tests use `androidTest` with `createComposeRule()` and `androidx.compose.ui:ui-test-junit4`. A `TunerScreenViewModel` interface is extracted so tests can inject a `FakeTunerScreenViewModel` without Hilt.

**Alternatives considered.**
- *Robolectric.* Introduces a parallel runtime that complicates Hilt + Compose interactions.

**Rationale.** `androidTest` with `createComposeRule()` is the standard Android Compose test pattern. The interface extraction is the smallest surface needed for fake injection. Cost: tests require a connected emulator or device.

---

## 2026-05-20 — Phase 5.4: hasRequestedAudioPermission preference

**Decision.** A `Boolean` field `hasRequestedAudioPermission` (default `false`) is added to `TunerPreferences` and tracked in `TunerUiState`. It is set to `true` the first time the `RequestPermission` launcher returns a result. The "permanently denied" condition requires `hasRequestedAudioPermission && !canShowRationale && !hasPermission`.

**Alternatives considered.**
- *SDK-version-specific API.* No reliable cross-version API exists.
- *Synthesise the state from `shouldShowRequestPermissionRationale` alone.* Fails on first launch where `shouldShowRationale = false` and `hasPermission = false` both hold, incorrectly routing to app settings.

**Rationale.** A single stored bit is the simplest, most robust approach across Android versions.

---

## 2026-05-21 — Metronome clicks synthesized in code, not bundled as audio assets

**Decision.** Metronome click sounds are synthesized at runtime from named constants in
`ClickSynthesizer`. No audio asset files (WAV/OGG) are bundled.

**Alternatives considered.**
- *Bundle pre-rendered WAV/OGG assets.* Rejected — no sound designer is available; binary files
  are opaque and break the "pure Kotlin, version-controlled parameters" convention established by
  the rest of the project.
- *Hybrid: generate assets at build time and commit them.* Rejected — adds a build step and a
  content-management concern for what is fundamentally a small set of numeric parameters.

**Rationale.** Synthesis keeps all audio behavior visible and testable in source code. Every
synthesis parameter is a named constant — adjusting a click's frequency or amplitude is a one-line
change. The extensibility benefit (future user-selectable timbres) aligns with the project's
clean, parametric approach.

**Supersession trigger.** If user-selectable click sounds are added and require professionally
designed samples that cannot be reasonably synthesized, revisit bundled assets at that time.

---

## 2026-05-21 — Metronome scheduler: anchor-based drift correction over plain delay loop

**Decision.** Beat timing uses an anchor-based drift-corrected `delay()` loop. Each beat target is
computed from a fixed start anchor:
`targetNs = startTimeNs + (beatIndex * 60_000_000_000L / bpm)`.
Nanosecond precision avoids integer-division rounding across many beats.

**Alternatives considered.**
- *Plain `delay(60_000 / bpm)` per beat.* Drift accumulates audibly over a session; integer
  division loses BPM precision. Rejected.
- *Sample-accurate scheduling via direct `AudioTrack` buffer writes.* Excellent timing, but
  substantially higher complexity (effectively a mini audio mixer). Residual jitter of the
  anchor approach (1–10 ms typical) is within human perception thresholds for "evenly spaced."
  Rejected for v1; can be revisited if jitter is audible in practice.

**Rationale.** Anchor-based correction gives no accumulated drift, simple testable code, and
jitter that matches what commercial metronome apps deliver — a well-understood sweet spot.

**Supersession trigger.** If users report audible jitter at extreme tempos or subdivisions,
revisit sample-accurate scheduling as a Polish Phase.

---

## 2026-05-21 — Metronome lifecycle: strict screen-scope binding, no foreground service

**Decision.** The metronome player runs only while the metronome screen is `STARTED`. Any
lifecycle exit (tab change, app backgrounded, audio focus loss) stops playback. Settings are
preserved; the player never auto-resumes.

**Alternatives considered.**
- *Keep playing on tab change.* Rejected — playing a click over the tuner or key finder makes
  no sense in real practice.
- *Foreground service for background playback.* Rejected — substantial scope (Service,
  notification channel, audio focus management, Android 14+ foreground-service types) for a
  marginal use case. Practice flows always involve an explicit restart anyway.

**Rationale.** The one-rule design (screen STARTED ↔ player running) is easy to reason about,
test, and impossible to violate accidentally. Lifecycle-driven mutual exclusion with the tuner
comes for free with no coordination code.

**Supersession trigger.** If "phone-in-pocket metronome" becomes a frequently requested feature,
revisit a foreground service as a separate Phase with its own design and notification spec.

---

## 2026-05-21 — Metronome: no per-beat accent customization in v1

**Decision.** Beat 1 of every bar is always ACCENTED; all other main beats are STANDARD. The
click-kind mapping is fixed — users cannot toggle per-beat accent levels in Phase 6.

**Alternatives considered.**
- *Level 1 (binary toggle per beat).* Would require making the beat indicator interactive, adding
  a reset affordance, extending `MetronomeConfig` with an accent pattern, and substantially
  growing the test surface. Rejected for v1.
- *Level 2 (three-state: silent / standard / accent).* Even larger scope. Rejected.

**Rationale.** The accent-on-beat-1 default covers the great majority of practice scenarios. The
known limitation (6/8 felt-in-2 requires accent customization) is explicitly documented. The
forward-compatible path is to add an optional `accentPattern: List<AccentLevel>?` field to
`MetronomeConfig` with `null` meaning "use the v1 default"; existing persisted configs remain valid.

**Supersession trigger.** If felt-in-2 or 7/8 accent grouping becomes a top user request,
implement Level 1 binary toggle first.

---

## 2026-05-21 — Phase 6.1: Subdivision enum gains `multiplier: Int` property

**Decision.** The Phase 2 `Subdivision` enum is extended with a constructor argument
`val multiplier: Int`. Values: `NONE(1)`, `EIGHTHS(2)`, `SIXTEENTHS(4)`, `TRIPLETS(3)`.

**Rationale.** The scheduler (Phase 6.2) and the `clicksPerBar` / `clickKindFor` helpers (Phase
6.1) both need the subdivision's multiplier as a value. Attaching it to the enum eliminates all
magic numbers from the scheduler's click-interval formula and from the beat-pattern helpers.
Making it a constructor argument rather than a `when` block in a companion method is idiomatic
Kotlin and makes the multiplier part of the enum's self-describing contract.

**Consequences.** All existing `Subdivision.X` references continue to compile without changes.
The only call sites affected are those that now can read `.multiplier` instead of reimplementing
the lookup.

---

## 2026-05-21 — Phase 6.1: Click synthesis parameters are v1 starting values, explicitly tunable

**Decision.** The numeric synthesis parameters in `ClickParameters` (frequencies, amplitudes,
envelope duration, decay rate) are documented as **v1 starting values**. After Phase 6.2's first
manual listening pass, any constant can be adjusted by changing a single named value — no
architectural review required.

**Alternatives considered.**
- *Lock parameters permanently.* Rejected — without a hardware setup for A/B testing during
  design, "correct" values for frequency spacing and amplitude hierarchy require iterative
  adjustment. Treating them as permanent before a first listen would invite unnecessary git
  ceremony on what is really a calibration step.
- *Make parameters configurable at runtime.* Rejected — the synthesizer is used as a pure,
  deterministic function; runtime configurability adds unnecessary complexity.

**Rationale.** The parameters chosen (1500/1000/800 Hz, 0.70/0.50/0.25 amplitudes, 30 ms / 160.0
decay) reflect best-practice starting points for a "clean digital tick." Encoding them as named
constants in `ClickParameters` makes adjustment a one-line diff, keeps the contract stable, and
allows tests to continue asserting the correct hierarchy and magnitude without re-engineering.

**Supersession trigger.** After Phase 6.2's manual smoke test. Any adjusted values are committed
alongside a note in `DECISIONS.md`.

---

## 2026-05-21 — Phase 6.1: ClickKind placed in domain/model, not data/audio

**Decision.** `ClickKind` is placed in `metronome/domain/model/ClickKind.kt` rather than
`metronome/data/audio/` as originally noted in the Phase 6.1 plan.

**Alternatives considered.**
- *`data/audio/` as the plan originally suggested.* Would require `BeatPattern.kt` in
  `domain/model/` to import from `data/audio/` — a domain → data dependency, which violates
  Clean Architecture (domain must not depend on data).

**Rationale.** `ClickKind` is a pure domain concept: "what kind of beat is this?" The synthesizer
(`data/audio/ClickSynthesizer`) consumes it as input — a data-depends-on-domain relationship,
which is the correct direction. Placing `ClickKind` in domain allows `BeatPattern.kt` and the
synthesizer to both reference it without any layering violation.

---

## 2026-05-22 — Phase 6.2: MetronomePlayer API refactored from imperative to flow-based

**Decision.** The Phase 2 imperative `MetronomePlayer` interface (`start(config)`, `stop()`,
`updateConfig(config)`, `currentBeat: Flow<Int>`) is **replaced** by a single method:
`fun run(initialConfig: MetronomeConfig, configFlow: Flow<MetronomeConfig>): Flow<PlayerEvent>`.
Collecting the returned flow starts playback; cancelling the collector stops it. The ViewModel owns
the collector's lifetime.

**Alternatives considered.**
- *Keep the imperative API, wire it to a `callbackFlow` internally.* Would preserve the old
  interface but require storing mutable audio state as fields on `AudioTrackMetronomePlayer` —
  making concurrent calls to `start`/`stop` a race condition.
- *Retain `currentBeat: Flow<Int>` as a separate shared flow.* Requires the player to own a
  `MutableSharedFlow` and manage its lifecycle separately from the AudioTrack. The events flow
  unifies the two concerns.

**Rationale.** The flow-based model makes `AudioTrack` resource lifetime unmistakably tied to the
coroutine scope of the collector — the same pattern used successfully in `MicrophoneAudioSourceImpl`
(Phase 5.2). `PlayerEvent` (with `Started`, `BeatTick`, `Failed` subtypes) cleanly replaces the
`currentBeat` flow and adds error signaling with no extra interface surface.

**Consequences.** `PlayerEvent` is placed in `domain/model/` so that `MetronomePlayer` (in
`domain/repository/`) can return `Flow<PlayerEvent>` without creating a domain → data dependency.
`StopMetronomeUseCase` becomes a no-op semantic stub; stopping is handled by ViewModel job
cancellation. `MetronomeStubsTest` (tested the old imperative stubs) is deleted.

---

## 2026-05-22 — Phase 6.2: Clock abstraction for testable timing

**Decision.** A `Clock` interface (`fun nanoTime(): Long`) is placed in `common/util/`. The
production implementation wraps `System.nanoTime()`. A `FakeClock` in the test source set returns a
manually controlled value.

**Alternatives considered.**
- *Call `System.nanoTime()` directly.* Not virtualized by `kotlinx-coroutines-test`; makes timing
  logic impossible to test without real-time delays.
- *`kotlinx.coroutines.test.TestCoroutineScheduler`.* Virtualizes `delay()` but not
  `System.nanoTime()`. Insufficient for the anchor-based scheduler which uses nanosecond wall
  time, not coroutine time.

**Rationale.** Injecting `Clock` is the minimal change that makes the entire scheduling and tap-tempo
math testable on the JVM at zero cost (no sleep, no device). The interface is placed in `common/`
because it is inherently domain-agnostic and may be reused by other timed features.

---

## 2026-05-22 — Phase 6.2: BeatScheduler extracted from AudioTrackMetronomePlayer

**Decision.** The beat-scheduling state machine is extracted into a separate `internal class
BeatScheduler(clock: Clock, initialConfig: MetronomeConfig)` with no `AudioTrack` dependency. All
scheduling math and re-anchor logic live there. `AudioTrackMetronomePlayer` holds a `BeatScheduler`
instance and calls its methods.

**Alternatives considered.**
- *Keep scheduling logic inline in `AudioTrackMetronomePlayer`.* Correct, but the player requires a
  real `AudioTrack` and cannot be JVM-unit-tested. The embedded math would have zero test coverage
  on the JVM.

**Rationale.** Extracting `BeatScheduler` as a pure, injected-clock state machine makes 100% of the
scheduling logic JVM-testable (`AudioTrackMetronomePlayerTest.kt`). The player becomes a thin shell
responsible only for audio I/O concerns (AudioTrack, audio focus, buffer writes).

---

## 2026-05-22 — Phase 6.2: Metronome persists to a dedicated DataStore file

**Decision.** `MetronomePreferencesImpl` uses `preferencesDataStore(name = "metronome_preferences")` —
a file separate from the tuner's `tuner_preferences`.

**Alternatives considered.**
- *Share the tuner's DataStore.* Rejected — the modules are independent; sharing a file creates an
  invisible coupling and makes it possible to accidentally corrupt one module's state while writing
  another's.

**Rationale.** Separate files enforce the module boundary at the persistence level. Future deletion
of either module leaves no orphaned keys in a shared file. The cost is negligible (one extra file on
disk).

---

## 2026-05-22 — Phase 6.2: Whole-config replacement strategy for invalid persisted data

**Decision.** When any single field in the persisted `metronome_preferences` DataStore is invalid
(null, out of range, unrecognized signature, or unrecognized subdivision name), the **entire
configuration** is replaced with `MetronomeConfig.DEFAULT`. Partial repair is not performed.

**Alternatives considered.**
- *Field-by-field fallback: repair only the invalid field, keep valid fields.* Rejected — a
  partially-corrupt config (e.g., invalid BPM with valid signature) may still produce an invalid
  combination after per-field repair, and the logic to detect it is more complex than a full reset.
- *Silently ignore the invalid field and keep the previous value.* Rejected — this is silent data
  corruption and inconsistent state.

**Rationale.** Whole-config replacement on any validation failure is simple, predictable, and safe.
The user loses no user-facing data they would notice (the next change they make persists correctly).
`RawMetronomeConfig.requiresRepair()` returns `false` for the all-null case (first launch) so
unnecessary write-back is suppressed.

---

## 2026-05-22 — Phase 6.2: Tap tempo — rolling 5-tap window, 2-second reset

**Decision.** The tap tempo algorithm (implemented in `TapTempoCalculator`) uses:
- A rolling window of the most recent **5 tap timestamps** (giving 4 usable intervals).
- The estimated BPM is the simple mean of those intervals, converted to BPM and rounded to the
  nearest integer.
- A gap of **> 2 000 ms** between consecutive taps clears the window and begins a new session.
- `null` is returned on the first tap of any session (no interval yet).
- The result is clamped to `[MetronomeConfig.BPM_MIN, MetronomeConfig.BPM_MAX]`.

**Alternatives considered.**
- *Larger window (8–10 taps).* More accurate for steady tempos but sluggish to respond to deliberate
  tempo changes. Rejected.
- *Weighted average (recent taps weighted more heavily).* Marginally better responsiveness at the
  cost of more complex, harder-to-test math. Rejected for v1.

**Rationale.** The 5-tap window is the industry-standard choice (used by most hardware and software
metronomes). The 2-second timeout matches the intuitive "I paused — this is a new session" feeling
without triggering spuriously on brief hesitations.

**Supersession trigger.** If users report the BPM display being slow to respond to deliberate tempo
changes, reduce the window to 4 taps. If it jumps erratically, consider a weighted average.

---

## 2026-05-22 — Phase 6.2: Metronome audio attributes — USAGE_MEDIA + CONTENT_TYPE_SONIFICATION

**Decision.** `AudioTrackMetronomePlayer` configures its `AudioTrack` with
`AudioAttributes.USAGE_MEDIA` and `AudioAttributes.CONTENT_TYPE_SONIFICATION`.

**Alternatives considered.**
- *`USAGE_ALARM`.* Keeps playing over DND and bypasses volume controls in some Android versions.
  Too aggressive for a practice tool.
- *`USAGE_ASSISTANCE_SONIFICATION`.* Intended for UI feedback sounds, not music-aligned content.
- *`USAGE_MUSIC`.* Semantically correct but `CONTENT_TYPE_SONIFICATION` better describes discrete
  click events vs. a continuous audio stream.

**Rationale.** `USAGE_MEDIA` places the metronome in the media volume channel (the channel users
expect to control for practice audio), and `CONTENT_TYPE_SONIFICATION` correctly classifies the
content. The combination is the same as standard metronome apps on Android.

---

## 2026-05-22 — Phase 6.2: Audio focus — AUDIOFOCUS_GAIN, any loss closes the flow

**Decision.** `AudioTrackMetronomePlayer` requests `AudioManager.AUDIOFOCUS_GAIN`. Any focus-loss
event (transient or permanent, with or without duck permission) closes the `callbackFlow` via a
`PlayerEvent.Failed(AUDIO_FOCUS_DENIED)` emission followed by channel close. Playback is not
auto-resumed when focus returns.

**Alternatives considered.**
- *Pause on transient loss, resume on regain.* Requires storing enough state to resume mid-bar.
  The "play only while screen is STARTED" lifecycle decision (2026-05-21) means transient losses
  (phone call, alarm) during active practice are uncommon. Auto-resume adds complexity for a
  marginal case.
- *Duck on `AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK`.* Metronome clicks at reduced volume are
  arguably worse than silence — the user may lose track of tempo. Rejected.

**Rationale.** The simplest, safest contract: if focus is lost for any reason, the metronome stops.
The user restarts intentionally. Consistent with the lifecycle decision.

---

## 2026-05-22 — Phase 6.3: StopMetronomeUseCase removed — stopping is coroutine cancellation

**Decision.** `StopMetronomeUseCase` is **deleted**. Stopping playback is handled exclusively by
cancelling the `playerJob` coroutine inside `MetronomeViewModel`. The use case is not replaced.

**Alternatives considered.**
- *Keep `StopMetronomeUseCase` as a semantic façade that cancels the job via a callback or
  shared cancellation token.* Adds indirection with no testability benefit: the ViewModel still
  has to hold the job reference anyway. Any logic worth testing lives in the ViewModel, not the
  use case.
- *Give `MetronomePlayer` an explicit `stop()` method.* Contradicts the flow-based API decision
  from Phase 6.2, where stopping is defined as "cancel the collector."

**Rationale.** In the flow-based model, starting = collecting, stopping = cancelling. There is no
domain logic in "cancel a coroutine job" that warrants encapsulation in a use case class. Keeping
the empty stub would violate SRP by manufacturing a responsibility where none exists.

---

## 2026-05-22 — Phase 6.3: MetronomeUiState extended with tempoDescriptor and isInitialLoadComplete

**Decision.** Two fields are added to `MetronomeUiState`:
- `tempoDescriptor: TempoDescriptor` — derived from `config.bpm` via `tempoDescriptorFor()`;
  displayed as a human-readable label (ADAGIO / ANDANTE / MODERATO / ALLEGRO / PRESTO).
- `isInitialLoadComplete: Boolean` — `false` until the first DataStore emission arrives so the
  screen can suppress content until persisted values are known.

**Alternatives considered.**
- *Compute `tempoDescriptor` in the Composable from `config.bpm`.* Puts display logic in the UI
  layer and makes it harder to test without Compose. Keeping it in state means the ViewModel
  produces a fully ready-to-render snapshot.
- *Model "loading" as a nullable `config`.* Forces null-checks throughout the UI for a transient
  state that only matters for the first frame. A Boolean flag is explicit and zero-cost.

**Rationale.** Both additions belong in the state object: `tempoDescriptor` is a derived display
value (single source of truth in the ViewModel), and `isInitialLoadComplete` is observable screen
state, not a one-shot event.

---

## 2026-05-22 — Phase 6.3: One-shot errors via SharedFlow<MetronomeEvent>, separate from state

**Decision.** Audio-unavailable errors are emitted through a `MutableSharedFlow<MetronomeEvent>`
(`replay = 0`, `extraBufferCapacity = 1`) exposed as `events: SharedFlow<MetronomeEvent>`. They
are **not** embedded in `MetronomeUiState`.

**Alternatives considered.**
- *`errorMessage: String?` field in `MetronomeUiState`.* Requires explicit "event consumed"
  acknowledgement so the snackbar does not re-appear on re-composition. Managing that flag is more
  complex than a dedicated event channel.
- *`Channel<MetronomeEvent>` exposed directly.* `SharedFlow` is the idiomatic Compose-friendly
  choice for one-shot events; it integrates cleanly with `LaunchedEffect` and
  `repeatOnLifecycle(STARTED)` (the pattern established in Phase 5.4 for `TunerViewModel`).

**Rationale.** Mixing ephemeral events into durable state is a known anti-pattern in MVI/MVVM —
the state machine would need extra logic to distinguish "event pending" from "event consumed."
A separate `SharedFlow` cleanly models "fire and forget" semantics. `replay = 0` guarantees no
stale errors are re-delivered after rotation.

---

## 2026-05-22 — Phase 6.3: BPM persistence debounced 200ms; player updated immediately

**Decision.** When the user changes BPM (slider drag, +/− tap, tap tempo):
1. `configFlow.value` and `_uiState` are updated **immediately** (synchronous, no coroutine).
2. DataStore persistence (`preferences.setConfig()`) is **debounced 200 ms** via a cancellable
   `persistJob` in `viewModelScope`.

The named constant is `PERSIST_DEBOUNCE_MS = 200L` in the ViewModel's companion object.

**Alternatives considered.**
- *Persist on every change.* During a slider drag, a 120-BPM sweep can produce 60+ write calls
  per second, hammering DataStore unnecessarily and causing noticeable I/O contention.
- *Persist only on stop or lifecycle exit.* The player and UI are always up-to-date, but if the
  app is force-killed mid-session, the last BPM is lost. 200 ms ensures any deliberate tap is
  persisted before the next UI interaction.
- *Debounce both the player and DataStore.* The player must respond immediately so the audible
  beat grid snaps to the new tempo without a perceptible lag. Only the storage write is debounced.

**Rationale.** The two-path design gives the user instant audio feedback (critical for tap tempo
and live adjustments) while protecting DataStore from write storms. 200 ms is long enough to
coalesce a rapid slider drag into a single write, and short enough that any intentional tap
is persisted before the screen is typically navigated away from.

---

## 2026-05-22 — Phase 6.4: Start/Stop button revised to icon + text after mockup review (Item 18)

**Decision.** The Start/Stop pill button shows an icon **and** a text label: ▶ + "Start" when
stopped, ⏸ + "Stop" when running. The original spec described the button as icon-only.

**Alternatives considered.**
- *Icon-only (original spec).* Saves horizontal space but reduces scannability at a glance.
  TalkBack must announce the icon's content description, which is redundant with the text label.
- *Text-only.* Loses the universal "play/pause" affordance that is immediately recognisable
  across cultures and age groups.

**Rationale.** The approved mockup review (Item 18) confirmed icon + text as the final design.
The text label sits to the right of the icon inside the 60dp pill, using `body.strong` style.
The icon's contentDescription is `null` because the text label serves accessibility.

---

## 2026-05-22 — Phase 6.4: UI structure additions from mockup review (Item 23)

**Decision.** Four UI elements visible in the approved mockups are formalised and documented:

**23a — Page status kicker.** A `mono.micro` kicker line above the H1 title shows
`METRONOME · RUNNING` (with a leading mint pulsing dot) or `METRONOME · STOPPED` (no dot).
The pulsing dot uses a ~1 s alpha animation (not gated on reduced-motion — gentle alpha pulse
is not a vestibular trigger per DESIGN.md §9).

**23b — Beat indicator header.** A `mono.micro` row above the beat segments shows:
- Left: `BEAT · X / N` (1-indexed current beat, time-signature numerator).
- Right: `QUARTER NOTES` for /4 signatures, `EIGHTH NOTES` for /8 signatures.

**23c — SUBDIVIDE kicker label.** The kicker above the subdivision dropdown reads "SUBDIVIDE"
(verb form), not "SUBDIVISION". The dropdown values use noun form ("None", "Eighth notes", etc.).

**23d — Tempo card as a grouped container.** The BPM display, descriptor, slider, and ±1 buttons
are enclosed in a single visually-bounded container (readout-well style: `bg.inset`, `r.xl`),
not laid out as loose siblings.

**Rationale.** These structural choices were visible in the approved mockups but not yet
captured in DESIGN.md §8.2. Formalising them here closes the gap between the spec and the
approved visual design.

---

## 2026-05-22 — Phase 6.4: Beat indicator 80ms animation overrides reduced-motion intentionally

**Decision.** The beat indicator's 80ms colour-flash animation is always active, even when the
system's reduced-motion setting is on.

**Rationale.** The beat indicator is the *primary temporal indicator* — without it, the user
cannot tell which beat the metronome is on. Disabling the flash would break core functionality,
not just aesthetics. This is the narrowly scoped exception to the reduced-motion rule documented
in DESIGN.md §8.2.

---

## 2026-05-22 — Phase 6.4: Screen-on lifecycle strictly bound to isPlaying + screen presence

**Decision.** `FLAG_KEEP_SCREEN_ON` is managed via `DisposableEffect(isPlaying)` in the
`KeepScreenOnWhilePlaying` composable. The `onDispose` block always clears the flag so it is
removed both on `isPlaying = false` transitions and when the screen leaves the composition.

**Alternatives considered.**
- *Foreground service with ongoing notification.* Provides background playback but is out of
  Phase 6.4 scope (Phase6-Metronome-Decisions.md Item 5, deferred). A foreground service would
  replace this composable in a future phase.
- *Clear flag only when stopped (not on navigate-away).* The flag would persist if the user
  navigated away while playing. `DisposableEffect` firing on composition exit is the correct hook.

**Rationale.** The `DisposableEffect` keyed on `isPlaying` re-fires on every state change and
on composable disposal, covering all lifecycle exit paths cleanly.

---

## 2026-05-22 — Phase 6.4: Glow rendered as layered semi-transparent boxes (hardware-accelerated)

**Decision.** The Start/Stop button 24dp glow and the beat-1 segment 12dp glow are both rendered
using the concentric-box approach (multiple semi-transparent rounded shapes at increasing radii
and decreasing alpha). No `BlurMaskFilter` is used for these glows.

**Alternatives considered.**
- *`BlurMaskFilter` via `drawIntoCanvas`.* Produces a smooth Gaussian blur but requires a
  software render layer (`Modifier.graphicsLayer { renderEffect = ... }` approach is API 31+
  and behaves differently from BlurMaskFilter). Hardware-layer compositing limitations make this
  fragile. The NeedleGauge uses it for the needle glow because the needle is a Canvas draw op;
  pill/rect shapes can use the box approach instead.
- *`Modifier.shadow`.* Shadows are always dark; mint glow must be a custom colour.

**Rationale.** The layered-box pattern is already established in the codebase for the nav
indicator glow (AppNavigationBar.kt). Consistent use of one hardware-accelerated approach across
all two permitted glows simplifies maintenance.

---

---

## 2026-05-30 — Phase 7.1: Key Finder match-scoring formula (supersedes APP_SPECIFICATION.md)

**Decision.** The matching engine uses the following formula for every candidate scale `S`
against input `I`:

```
n          = |I.pitchClasses|               (distinct input pitch classes)
covered    = |I.pitchClasses ∩ S.pitchClasses|
rootBonus  = 1  if (root marked AND S.root == markedRoot)  else 0
points     = covered + rootBonus
maxPoints  = max(7, n) + (1 if root marked else 0)
percent    = round_half_up(points / maxPoints × 100)
```

Candidates with `covered == 0` are excluded from results. `isFull` = `covered == n`.

**Supersedes.** `APP_SPECIFICATION.md` "Module: Key Finder → Matching Logic", which described
the score as "percentage of input notes that belong to the mode." That formula did not account
for stray notes lowering the score, did not define `maxPoints`, and treated the tonic as a
separate ranking step rather than a point contribution. This entry explicitly supersedes both
the score definition and the tonic-handling description in the spec.

**Alternatives considered.**
- *Raw covered / n:* gives 100% for any 3 notes that fit, regardless of scale size. Stray notes
  don't lower the score. Rejected — produces misleading results when the user adds notes that
  are outside the scale.
- *covered / 7 always:* correct denominator for a full 7-note set, but wrong for small inputs
  (3-note input gives max 43%, making partial results look deceptively poor). The `max(7, n)`
  floor balances both.

**Rationale.** The `max(7, n)` denominator penalises stray notes (n > 7 inputs lower the score)
while preserving meaningful percentages for small, well-fitting inputs (n ≤ 7). Folding the root
into the formula (rather than post-sorting) gives a single comparable number per result and
allows the root to break ties without a separate ranking pass.

---

## 2026-05-30 — Phase 7.1: Single root note folded into the percentage

**Decision.** The user may mark exactly one note as the root. The root contributes `+1` to
`points` and `+1` to `maxPoints` when marked, changing the score for scales that match the
root relative to those that don't. There is no separate post-sort step for root-matching scales.

**Supersedes.** `APP_SPECIFICATION.md` "Module: Key Finder → Output": "If a tonic is provided,
modes whose root is the tonic are ranked higher (ties broken by match score, then alphabetical
mode name)." The plan's single-formula approach replaces that two-step process.

**Alternatives considered.**
- *Post-sort boost:* sort first by score, then promote tonic matches. Produces
  discontinuities — a 60% tonic match jumping above a 95% non-tonic match feels wrong.
- *Separate tonic tier:* show tonic results in a distinct section. Adds UI complexity for a
  single bit of information; the percentage already encodes it.

**Rationale.** The formula gives the root a mathematically consistent weight (1 out of at most
8 points). A scale that perfectly matches the root note earns the bonus; all other scales are
naturally ranked below it at the same note content. The tie-breaking emerges from arithmetic,
not a special rule.

---

## 2026-05-30 — Phase 7.1: Scale inventory — 14 types (supersedes APP_SPECIFICATION.md diatonic-only set)

**Decision.** The Key Finder catalog contains **14 scale types** across three families:

| Family | Types |
|---|---|
| Diatonic (7) | Ionian, Dorian, Phrygian, Lydian, Mixolydian, Aeolian, Locrian |
| Harmonic minor (3) | Harmonic Minor, Phrygian Dominant, Locrian ♮6 |
| Melodic minor (4) | Melodic Minor, Lydian Dominant, Altered, Dorian ♭2 |

Total catalog: 14 types × 12 roots = **168 candidates**.

**Explicitly excluded:** pentatonic scales, blues scales, the remaining modes of harmonic and
melodic minor not listed above, double-harmonic / Byzantine parents. Exclusions are recorded to
prevent accidental re-addition without discussion.

**Supersedes.** `APP_SPECIFICATION.md` "Modes to Support": "Ionian … Locrian. Pentatonic,
harmonic/melodic minor, and other scale types may be added in a future phase." This entry
implements the "future phase" expansion and locks the inventory for Phase 7.

**Rationale.** The 7 additional types are the scales guitarists most commonly encounter beyond
the diatonic modes (harmonic minor for metal/classical, Phrygian Dominant for flamenco/metal,
Melodic Minor for jazz, Lydian Dominant for jazz fusion, Altered for jazz, Dorian ♭2 as a
secondary jazz colour). All are 7-note scales, which preserves the uniform `max(7, n)`
denominator. Pentatonics are excluded because their 5-note structure would require a
special-cased denominator floor.

---

## 2026-05-30 — Phase 7.1: Ranking rule (supersedes APP_SPECIFICATION.md separate-tonic step)

**Decision.** After scoring, results are ranked by:

1. `percent` descending.
2. `type.rankOrder` ascending ("common-first" order: Major → Natural Minor → Dorian → Phrygian
   → Lydian → Mixolydian → Locrian → Harmonic Minor → Phrygian Dominant → Locrian ♮6 →
   Melodic Minor → Lydian Dominant → Altered → Dorian ♭2).
3. Root pitch class ascending (C=0, C♯/D♭=1, … B=11).

The top 7 results are returned. Scales with `covered == 0` are excluded before ranking.

**Supersedes.** `APP_SPECIFICATION.md` "Module: Key Finder → Output": "If a tonic is provided,
modes whose root is the tonic are ranked higher." The tonic's effect is already expressed through
the percentage (see the root-bonus decision above), so no additional sort pass is needed. The
"alphabetical mode name" tie-break in the spec is replaced by the common-first ordinal, which
also imposes a deterministic, musically-meaningful order.

**Alternatives considered.**
- *Alphabetical tie-break:* deterministic but musically arbitrary (B Locrian before C Major).
- *Random / stable sort:* non-deterministic; bad for reproducibility and testing.

**Rationale.** Common-first order surfaces the most familiar results at the top of ties, which
reduces cognitive load for users who see Major before Locrian.

---

## 2026-05-30 — Phase 7.1: Match gate — ≥ 3 distinct pitch classes

**Decision.** `MatchScalesUseCase` returns an empty list when the input has fewer than 3
distinct pitch classes. With 0–2 notes, too many scales match at high percentages to be
meaningful; the threshold is an explicitly-tested constant (`MIN_NOTES_TO_MATCH = 3`).

**Rationale.** Three notes is the minimum for a meaningful harmonic context (a triad). With
fewer, the signal-to-noise ratio is too low. The gate value was established in `Phase7-PLAN.md`
based on UX reasoning: below 3 notes, the results list would show too many high-percentage
matches to be actionable.

**Supersession trigger.** If user research shows 2 notes is useful (e.g. for interval
identification), lower the constant.

---

## 2026-05-30 — Phase 7.1: Conventional enharmonic spelling for Key Finder results

**Decision.** `ScaleSpeller` (a pure `object` in `common/util/`) produces conventionally-spelled
note names using two rules:

1. **Root spelling:** a fixed canonical table maps each pitch class to its standard root name
   (`C, D♭, D, E♭, E, F, F♯, G, A♭, A, B♭, B`).
2. **Degree spelling:** starting from the root's letter, each of the 7 degrees is assigned the
   next letter cyclically (A–G, wrapping G→A). The accidental is the signed semitone difference
   between the degree's actual pitch class and the natural pitch class of its assigned letter.

This guarantees exactly one of each letter A–G per scale and correct accidentals for all 168
candidates, including exotic cases (e.g. G Altered → G A♭ B♭ C♭ D♭ E♭ F).

**Rationale.** The letter-per-degree rule is the standard musicological spelling convention.
Computing accidentals from the natural pitch classes of the assigned letters is
algorithm-derivable, testable, and correct across all 14 scale types without special cases or
lookup tables for individual scales.

---

## 2026-05-30 — Phase 7.1: ScaleType superset added alongside retained Mode enum

**Decision.** `common/model/ScaleType` is added as a new 14-entry enum. The existing
`common/model/Mode` enum is **retained unchanged**. The seven diatonic `ScaleType` entries
carry the same `intervalsFromRoot` arrays as the corresponding `Mode` entries; a unit test
asserts equality so the data has a single conceptual source of truth (maintained by `Mode`,
verified by the test).

**Rationale.** `Mode` is consumed by the tuner (note display), Chord Finder (diatonic
harmonization), and existing `Scale` / `MusicTheory` logic. Replacing `Mode` with `ScaleType`
would require changes across all those consumers for no benefit in Phase 7. Adding `ScaleType`
as a superset keeps `Mode` stable for its existing callers while giving the Key Finder its
14-type catalog.

**Consequences.** If a future phase merges the two (e.g. Chord Finder needs harmonic minor
chords), replace `Mode` with `ScaleType` at that point and delete `Mode`. Until then, both
coexist, with the test as the bridge.

---

---

## 2026-05-31 — Phase 7.2: Shared `audio/` module created (feature-first deviation)

**Decision.** A new top-level `audio/` package (`de.ritzelprimpf.toniqo.audio`) is created as a
sibling to `common/` and `ui/`. It contains `AudioCaptureSource` + `AudioRecordCaptureSource`
(promoted from `tuner/data/MicrophoneAudioSource*`), `CaptureEvent`, `AudioSourceKind`, and
`PitchDetector` + `YinPitchDetector` + `YinConfig` (promoted from `common/util/`). A new
`audio/di/AudioModule` provides all Hilt bindings; the bindings previously in `TunerModule` for
these classes are removed. This is a deliberate deviation from the feature-first layout in
`CLAUDE.md` §3, which names only `common/` and `ui/` as shared.

**Alternatives considered.**
- *Keep everything in `tuner/data/` and add forwarding wrappers in `common/`.* Would create a
  module dependency where `keyfinder/data/` depends on `tuner/data/` — violating the
  `IMPLEMENTATION_NOTES.md` module isolation rule ("a module must not reference another module's
  package").
- *Put the capture source in `common/audio/`.* `common/` is reserved for pure, platform-free
  music-theory primitives; `AudioRecord` has Android dependencies and must not enter it.
- *Duplicate the capture source and YIN detector in each feature module.* Violates DRY; any
  future fix or parameter change would need to be applied in two places.

**Rationale.** Audio capture is a cross-feature Android concern that belongs in a shared but
platform-honest location. `audio/` is that location: it is shared (Tuner + Key Finder) but
explicitly Android-dependent, keeping `common/` pure. The deviation from feature-first layout is
the minimum necessary to satisfy both module isolation and DRY simultaneously.

**Consequences.**
- `PitchDetector` and `YinPitchDetector` move from `common/util/` to `audio/`. This supersedes
  the Phase 5.2 decision that placed them in `common/util/`. They are part of the audio pipeline,
  not pure music theory, so `audio/` is the more coherent home.
- `TunerModule` loses the `PitchDetector`, `YinConfig`, and `MicrophoneAudioSource` bindings;
  these live in `AudioModule`.
- The tuner regression test suite is the acceptance gate for this refactor.

---

## 2026-05-31 — Phase 7.2: Key Finder stable-note detector confirmation and debounce thresholds

**Decision.** `StableNoteDetectorImpl` uses the following rules:

- **Confirmation window:** `CONFIRMATION_BUFFER_COUNT = 2` — the same pitch class must be
  detected in **2 consecutive buffers** before it is emitted.
- **Debounce:** once a pitch class is emitted, it is not re-emitted until either (a) a `null`
  (silence / no clear pitch) frame is received, or (b) a different pitch class is detected.

At the Phase 5.2 locked parameters (44 100 Hz sample rate, 4 096 frames per buffer), one buffer
takes ≈ 92.9 ms to fill; two buffers ≈ **186 ms**. This is within the target range of 150–250 ms.

**Alternatives considered.**
- *1 buffer (~93 ms).* Fires too easily on transients; a single chance detection would emit.
- *3 buffers (~279 ms).* Exceeds the 250 ms upper bound; feels sluggish for fast scalar playing.
- *Silence-only re-arm (ignoring pitch-class change).* Would require the user to silence between
  every note, which is impractical for chromatic scales or chord fragments played step by step.

**Rationale.** Two buffers provides transient immunity for sub-93 ms events (e.g. pick noise,
transients at string attack) while being responsive enough for deliberate quarter-note playing at
tempos up to ~160 BPM (a quarter note at 160 BPM ≈ 375 ms). The pitch-class-change re-arm is
critical for legato playing where the user slides from one note to the next without silence.

---

## 2026-05-31 — Phase 7.3: Note identity — pitch-class de-dup, first-seen spelling

**Decision.** The Key Finder note list is keyed by **pitch class** (0–11, octave-agnostic). At
most one chip is displayed per pitch class. The **first-seen `Note`** determines the chip's display
spelling; subsequent adds of the same pitch class are silent no-ops. The list is maintained as a
`LinkedHashMap<Int, String>` (pitchClass → displayName) to preserve insertion order.

**Alternatives considered.**
- *Store full `Note`s (with octave), dedupe in the use case.* Would require the ViewModel to
  project to pitch classes for every recompute and could result in two chips for e.g. "E2" and
  "E4" appearing alongside each other — confusing for a chromatic key-finding tool.
- *Always re-spell using the ScaleSpeller canonical table.* Rejected — the chip's spelling comes
  from how the user selected the note (picker or mic); overriding it silently would be surprising.

**Rationale.** The matching engine works on pitch classes. Exposing chips by pitch class makes the
UI consistent with the engine, avoids duplicate chips for the same audible note, and keeps the
`NoteChip` model simple.

---

## 2026-05-31 — Phase 7.3: Root removal clears root (no auto-reassignment)

**Decision.** When the user removes the note that is currently marked as the root, `rootPitchClass`
is cleared to `null`. No note is automatically promoted to root.

**Alternatives considered.**
- *Auto-assign the root to the lowest-pitch-class remaining note.* Implicit state mutation;
  the user did not request a root change. Would produce a confusing result recompute as a
  side-effect of a remove action.

**Rationale.** The simplest, most predictable semantic: removing a note removes it and nothing
else. If the root disappears, the root slot is empty. The user chooses a new root explicitly.

---

## 2026-05-31 — Phase 7.3: Duplicate add is a silent no-op

**Decision.** Adding a note whose pitch class is already in the list — whether from the picker
or from the mic — is a **silent no-op**: no error, no toast, no state change. The existing chip
and its spelling are preserved.

**Alternatives considered.**
- *Show an error or highlight the duplicate chip.* Rejected — the deduplication is intentional
  product behaviour, not an error condition. The mic path in particular will frequently attempt
  to re-add a held note; surfacing that as an error would be noisy.

**Rationale.** Idempotent add matches user expectation: "this note is already there" is the right
outcome with no need for feedback.

---

## 2026-05-31 — Phase 7.3: Note cap — MAX_NOTE_COUNT = 12

**Decision.** The ViewModel enforces a maximum of **12 distinct notes** (pitch classes) in the
list at any time. This cap is a named constant `MAX_NOTE_COUNT = 12` in `KeyFinderViewModel`.

**Alternatives considered.**
- *No cap.* Unnecessary — 12 is the chromatic maximum; a 13th distinct pitch class cannot
  exist. The cap documents and enforces the logical maximum explicitly rather than relying on
  the implicit dedup-as-cap behaviour.
- *A smaller cap (e.g. 7 or 8).* Rejected — the matching engine handles up to 12 pitch classes
  (n > 7 inputs lower stray notes' scores via the `max(7,n)` denominator). Limiting the input
  would prevent users from exploring chromatic inputs deliberately.

**Rationale.** 12 is the exact chromatic ceiling; naming it as a constant makes the intent
explicit and prevents a future caller from assuming the list is unbounded.

---

## 2026-05-31 — Phase 7.4: Add-note picker form — compact bottom sheet with 4×3 chromatic grid

**Decision.** The note-add picker is a `ModalBottomSheet` containing a title "ADD NOTE", a subtitle, and a 4×3 grid of all 12 pitch classes using `ScaleSpeller.ROOT_DISPLAY_NAMES` spellings (`C D♭ D E♭ / E F F♯ G / A♭ A B♭ B`). Already-present pitch classes are shown in a disabled visual state. Tapping a cell calls `addNoteFromPicker` and closes the sheet.

**Alternatives considered.**
- *Inline expandable grid in the note rail.* Clutters the rail for a transient action.
- *DropdownMenu anchored to the `+` button.* Limited layout control, hard to dismiss elegantly.

**Rationale.** Mirrors the tuner's preset picker pattern (also `ModalBottomSheet`). Keeps the main screen clean and gives the user a focused, discoverable affordance. The canonical spellings match the detail view, so the user sees the same note names everywhere.

---

## 2026-05-31 — Phase 7.4: Mic affordance placement — sub-header row (right of NOTES · n / TONIC · x)

**Decision.** The mic toggle (`mic` / `mic_off` icon, 20dp) is an `IconButton` at the right end of the "NOTES · n / TONIC · x" sub-header row. When `isListening = true`, a `PulsingDot` + "MIC LIVE" label appears in the same row immediately left of the toggle, mirroring the tuner's §8.1 mic-live indicator language.

**Alternatives considered.**
- *In the screen header row (next to the info button).* Requires extra row or crowd the header with two icons of different semantic weight.
- *As a dedicated floating pill below the note rail.* Adds layout complexity and breaks the row-based information hierarchy.

**Rationale.** The sub-header row already carries "NOTES · n" context. Placing the mic toggle adjacent to it keeps all note-input affordances in one visual region and keeps the primary title row uncluttered.

---

## 2026-05-31 — Phase 7.4: Mark-root gesture — tap chip body; remove gesture — tap × button

**Decision.**
- **Tap chip body** → `toggleRoot(pitchClass)`: marks the note as the root, or unmarks it if it already is. The root chip gains the 22% mint-mixed background and "· TONIC" suffix.
- **Tap × icon** (trailing in the chip) → `removeNote(pitchClass)`.

These two actions are visually separated (body vs trailing button), so they don't collide.

**Alternatives considered.**
- *Long-press to mark root, tap to remove.* Long-press is undiscoverable; users often don't try it without a visual hint. The × button makes remove obvious.
- *Tap to cycle role (normal → root → removed).* A three-state cycle on a single tap is surprising; remove should be an explicit action.

**Rationale.** The × pattern is standard for chips in Material Design. Assigning the chip body's tap to toggle-root gives that action a large touch target and keeps it discoverable ("tapping the note changes its role"). No gesture collision is possible since the × and the chip body don't overlap.

---

## 2026-05-31 — Phase 7.4: Detail view form — bottom sheet

**Decision.** Tapping a result row opens a `ModalBottomSheet` showing: the primary label (`H2`), subtitle (`MonoMicro`), percent + badges, a divider, and the 7 conventionally-spelled scale notes with their degree labels. No navigation to Chord Finder (cross-module navigation deferred).

**Alternatives considered.**
- *Full-screen route.* Adds navigation stack entry and breaks the "glance at the list → inspect one result → return" mental model. The list remains visible behind the sheet.

**Rationale.** The bottom sheet is lighter than a full route and keeps the result-list context visible in the scrim area, making it easy for the user to dismiss and examine the next result.

---

## 2026-05-31 — Phase 7.4: Result row → self-contained detail view, no Chord Finder navigation

**Decision.** Tapping a Key Finder result card opens a self-contained bottom sheet (detail view). The sheet does not contain a "Go to Chord Finder" link or navigation action. Cross-module navigation from Key Finder to Chord Finder is explicitly deferred.

**Rationale.** Chord Finder (Phase 8) is not yet implemented; wiring a nav action to it now would produce dead UI. The detail view's purpose — showing the scale's notes and degrees — is complete without it. The nav link can be added in Phase 8 as part of the Chord Finder implementation.

---

## 2026-05-31 — Phase 7.4: Canonical spelling for all chip display names

**Decision.** Both the picker path (`addNoteFromPicker`) and the mic path (`addNoteByPitchClass`) now use `ScaleSpeller.ROOT_DISPLAY_NAMES[pitchClass]` as the chip display name, replacing the previous `NoteName.sharpName` (ASCII "C#") used in the mic path. This ensures all chips show the same conventional spellings (unicode "D♭", "F♯") regardless of how the note was added.

**Alternatives considered.**
- *Keep sharpName for mic, ROOT_DISPLAY_NAMES for picker.* Creates an inconsistency: the picker shows "D♭" but the chip then shows "C#" if the same note is added via mic next session. Confusing.

**Rationale.** Consistency matters more than preserving "how the note arrived." The ScaleSpeller canonical table already defines the musically standard spelling for each pitch class; using it everywhere is simpler and more correct.

---

## 2026-05-31 — Phase 7.4 (supersedes mark-root gesture): tap = remove, long-press = tonic

**Decision.** Chips use `combinedClickable`: **tap = remove** the note, **long-press = toggle tonic**. No separate × button. A `mono.micro` hint line "TAP TO REMOVE · HOLD TO SET AS TONIC" is shown in `fg.quaternary` below the rail whenever notes are present but no tonic is marked; it disappears once a root is set.

**Supersedes.** The Phase 7.4 "Mark-root gesture" entry (tap chip body = toggle tonic, × button = remove). User testing found that for short note names ("C", "E") the × button occupied more than half the chip width, causing accidental removes when the user tapped what they thought was the chip label area.

**Alternatives considered.**
- *Keep × button, make it smaller.* Smaller than 16dp is below any practical tap target; still causes the layout crowding issue on short names.
- *Keep tap = tonic, add hint only.* The layout problem (× too large) persists regardless of discoverability text.

**Rationale.** Removing the × gives the entire chip to the primary remove gesture (matching Material `InputChip` convention) and eliminates the layout conflict. Long-press for the secondary tonic action is a standard Android pattern. The hint text makes both gestures explicit so neither requires discovery.

---

## 2026-06-05 — Phase 8.1: Chord Finder targets 14 ScaleTypes, not 7 diatonic modes (supersedes APP_SPECIFICATION.md)

**Decision.** The Chord Finder module harmonises all 14 `ScaleType`s from the Phase 7 Key Finder catalog — the 7 diatonic modes plus the harmonic-minor family (Harmonic Minor, Phrygian Dominant, Locrian ♮6) and the melodic-minor family (Melodic Minor, Lydian Dominant, Altered, Dorian ♭2). `ChordFinderInput` carries `scaleType: ScaleType` (0–11 root pitch class plus the enum) rather than a `Note` root and a `Mode`.

**Supersedes.** `APP_SPECIFICATION.md` "Module: Chord Finder — Modes supported: the same 7 diatonic modes as Key Finder." That scope was written before the Key Finder was expanded to 14 types in Phase 7.1. This entry explicitly supersedes it for Phase 8 onward.

**Alternatives considered.**
- *Stay at 7 modes.* Would require the user to switch to a different app for any harmonic/melodic-minor key. Rejected — the 14-type catalog is already implemented in Key Finder and costs nothing extra in the chord engine.

**Rationale.** Since the chord engine derives quality purely from intervals (no major-scale assumption), supporting all 14 types costs exactly zero extra logic. Limiting the UI to 7 types while the domain model already handles 14 would be an artificial constraint.

---

## 2026-06-05 — Phase 8.1: Interval-derived quality; SeventhQuality enum introduced; ChordQuality reduced to 4 triad values

**Decision.** Every chord quality is derived from the actual semitone intervals between stacked scale degrees — no hard-coded major-scale quality pattern. `ChordQuality` is reduced to exactly 4 triad values (`MAJOR`, `MINOR`, `DIMINISHED`, `AUGMENTED`), removing the five seventh-chord variants that were added in Phase 5.1. Seventh-chord types are modelled in a new `chordfinder/domain/model/SeventhQuality` enum with 7 entries: `MAJOR_SEVENTH`, `MINOR_SEVENTH`, `DOMINANT_SEVENTH`, `HALF_DIMINISHED`, `DIMINISHED_SEVENTH`, `MINOR_MAJOR_SEVENTH`, `AUGMENTED_MAJOR_SEVENTH`. `ChordQualityResolver` (a pure `object`) maps `(thirdInterval, fifthInterval)` → `ChordQuality` and `(ChordQuality, seventhInterval)` → `SeventhQuality`, throwing on any out-of-set input.

**Consequence — `MusicTheory.buildSeventhChords()` removed.** With seventh-chord types no longer in `ChordQuality`, `buildSeventhChords()` could not compile. It is removed because it is superseded by `FindChordsUseCase`, which handles all 14 scale types. `MusicTheory.buildTriads()` is retained; it only uses the 4 triad values.

**Alternatives considered.**
- *Keep `ChordQuality` at 9 values.* Would require a new parallel `SeventhQuality` enum that duplicated 5 of the 9 values. Rejected as duplication.
- *Put all quality logic in `MusicTheory`.* `MusicTheory` operates on `Mode`-based `Scale` objects; the new engine works on pitch classes + `ScaleType`. Mixing both paradigms in one object violates SRP.

**Rationale.** The chord engine needs only triad qualities as a stepping stone to seventh-quality resolution. Separating the two concepts into `ChordQuality` (triad) and `SeventhQuality` (seventh extension) reflects the two-step derivation process and makes the resolver's contract precise.

---

## 2026-06-05 — Phase 8.1: Guitar voicings added to Chord Finder module (supersedes APP_SPECIFICATION.md); engine follows in Phase 8.2

**Decision.** The Chord Finder module gains a guitar-voicings screen: tapping any chord on the list opens a screen showing playable fretboard diagrams for that chord, respecting the user's current tuning (standard 6-string + uniform-offset transpositions in v1). This supersedes `APP_SPECIFICATION.md` "Module: Chord Finder — Scope: Initial implementation: triads, with the seventh-chord toggle as part of the first release. No voicings." The voicing data model and runtime loader are built in Phase 8.2; this decision is recorded in 8.1 as required by `Phase8-PLAN.md`.

**Supersedes.** `APP_SPECIFICATION.md` "Module: Chord Finder — Scope" (no voicings). Chord Finder v1 ships triad voicings for standard tuning and uniform-offset transpositions.

**Rationale.** Guitar voicings are the natural completion of a chord-finder tool for guitarists. See `Phase8-PLAN.md` "Stage 2 — Voicing Resolution" for the full scope rationale, the CAGED-rejected rationale, and the tier-1/tier-2/tier-3 tuning model.

---

## 2026-06-05 — Phase 8.1: Phase 2 ChordFinderService stub removed; chord logic lives in FindChordsUseCase

**Decision.** The `ChordFinderService` interface, `ChordFinderServiceImpl` stub, and `ChordFinderModule` Hilt binding are deleted. `FindChordsUseCase` is now a self-contained pure class (`@Inject constructor()` with no service dependency) that implements the full chord-engine logic directly — mirroring how `MatchScalesUseCase` owns the Key Finder matching logic with no intermediate service layer. The old service name was "named Service (rather than Repository) because it performs computation, not persistence" — the same reasoning now applies directly to the use case itself.

**Alternatives considered.**
- *Keep the Service interface, implement it.* Would add an indirection layer with no testability benefit: `FindChordsUseCase` is already the test boundary, and the "service" would be a stateless delegate with identical inputs and outputs. An empty interface and its binding satisfy SRP only if they enable substitution — here they don't.

**Rationale.** Consistent with Key Finder (Phase 7.1 `MatchScalesUseCase`). The use case is the domain entry point; the service layer added in Phase 2 was a placeholder for the eventual implementation, not a long-term design requirement.

---

## 2026-06-05 — Phase 8.2: Data-driven chord-keyed voicings; CAGED runtime engine rejected

**Decision.** Guitar voicings are **data, not runtime-computed** in v1. The library is **keyed by chord identity** (`rootPitchClass + ChordQuality`) — never by key or mode — so each shape is curated once and reused across all modes that contain it. The shipped data asset (`assets/chordfinder/voicings_standard_6.json`) is produced by a throwaway offline Python generator + human curation; the runtime loader is deterministic and entirely guarded by the library validation test.

**Why not CAGED.** An earlier draft used a CAGED shape-transposition engine. CAGED shapes are defined by standard tuning's open-string intervals; the planned FP-3 runtime generator (tuning-adaptive voicings) cannot use them as a foundation. Building CAGED now would be thrown away rather than extended. Data-driven voicings key naturally to chord identity, letting the tier-2 fret-shift transform and the FP-3 generator both populate the same `Voicing` model without a schema change.

**Alternatives considered.**
- *CAGED runtime engine.* Rejected — dead end for the tuning-adaptive future described in FP-3.
- *kotlinx.serialization for the parser.* Would be cleaner, but requires an explicit new dependency (`CLAUDE.md` §8). Deferred unless the user approves. Android's built-in `org.json` parses the schema without modification.

**Rationale.** Static data + a tested loader gives the same runtime output as a generator, with the correctness advantage of human review and the performance advantage of no runtime search.

---

## 2026-06-05 — Phase 8.2: JSON parser — `org.json` (no new dependency)

**Decision.** `VoicingJsonParser` uses Android's built-in `org.json.JSONObject` / `JSONArray`. No new library dependency is introduced.

**Alternatives considered.**
- *kotlinx.serialization.* Cleaner, less boilerplate, but requires a new Gradle dependency and explicit user approval per `CLAUDE.md` §8. Not approved for Phase 8.2.

**Supersession trigger.** If `kotlinx.serialization` is approved for another feature, migrate the parser at that time.

---

## 2026-06-05 — Phase 8.2: Tier-2 uniform-offset transform (preserve sounding pitch, omit open, filter off-neck)

**Decision.** For tier-2 (uniform-offset) tunings: each standard voicing is shifted up by `abs(offset)` frets so it sounds the same pitch on the detuned instrument. Three filter rules apply:
1. **Open voicings are excluded** — an open string's pitch is fixed by the tuning and cannot be shifted.
2. **Off-neck voicings are excluded** — any shifted fret > `MAX_FRET` (15) is dropped.
3. **Preserved fields** — `fingers` and `rootStringIndices` are unchanged; the same grip on the same strings sounds the correct chord.

**Alternatives considered.**
- *Preserve grip (not sounding pitch) — omit open, keep fret positions.* Would sound a different chord than selected. Rejected as musically incorrect.
- *Include open voicings, annotate them as standard-only.* Complicates the UI without benefit; the user selected a transposed chord.

**Rationale.** The user chose a chord by name and expects to hear that chord. Shifting to preserve sounding pitch is the only musically meaningful transform for a uniformly-detuned guitar.

---

## 2026-06-05 — Phase 8.2: Neutral position-based voicing labels (diverges from mockup)

**Decision.** Voicings use a **1-based integer index** (`labelKey`) as their label, displayed as a zero-padded number (e.g. `01`, `02`). The mockup's `Open / A-shape / D-shape / E-shape` labels are **not used**.

**Supersedes (partial).** `Chord_Finder___C_voicings.png` shows CAGED shape names. This entry explicitly overrides those names with position-based labels.

**Rationale.** CAGED shape names are meaningful only in standard tuning. Under uniform-offset tier-2 or the future FP-3 generator, the same grip may not correspond to any CAGED shape name. Neutral labels work for all tunings without special-casing.

---

## 2026-06-05 — Phase 8.2: Variable string count baked in from day one

**Decision.** `Voicing.marks` and `Voicing.fingers` are `List<FretMark>` and `List<Int>` of length equal to the tuning's string count. No hardcoded `STRING_COUNT = 6`. The `validated()` factory takes `openNotes: List<Int>` whose size sets the required length. `FretboardDiagram` (Phase 8.5) is specified to render any string count.

**Rationale.** FP-3 adds non-6-string tunings. Baking the variable count in now means FP-3 adds data without a model change. The current v1 JSON ships 6-string data only; the model accepts 7/8.

---

## 2026-06-05 — Phase 8.2: `Voicing.bassDegree` seam for FP-1 (always ROOT in v1)

**Decision.** `Voicing` carries `bassDegree: ChordToneRole` (ROOT / THIRD / FIFTH / OTHER). All v1 curated voicings are root-position so the value is always `ROOT`. The field exists so FP-1 (inversions / slash chords) can populate the same model without a schema change.

**Rationale.** The cost of carrying one extra field is zero. Without it, adding FP-1 would require a model migration.

---

## 2026-06-07 — Phase 8.3: Two app-scoped in-memory stores for cross-module state

**Decision.** `LatestKeyResultStore` and `SelectedTuningStore` are `@Singleton` classes with `@Inject constructor()`, backed by `MutableStateFlow`. They are not injected via `AppModule` or `ChordFinderModule`; Hilt auto-provides any `@Singleton` with an `@Inject constructor`. Writers call `publish(…)` directly; readers hold the `StateFlow` reference.

**Alternatives considered.** A shared `AppStateHolder` data class containing both states; a `@Singleton` application-scope `ViewModel`; a reactive event bus.

**Rationale.** One class per concern satisfies SRP and ISP. `StateFlow` with `.value` for synchronous one-shot reads (seed algorithm) or `.collect` for reactive consumers (voicings re-lookup) covers both access patterns. No shared mediator is needed.

**Supersession trigger.** If more than ~5 cross-module stores accumulate, consolidating into a typed application state container may reduce boilerplate.

---

## 2026-06-07 — Phase 8.3: SelectedTuningStore carries TuningWithLabel (tuning + display name)

**Decision.** `SelectedTuningStore.publish(tuning, label)` and `selection: StateFlow<TuningWithLabel>` carry both the `GuitarTuning` and the human-readable `displayName` from `TunerPreset`. The `TunerViewModel` passes `preset.displayName` at each publish site.

**Alternatives considered.** Store only `GuitarTuning` and derive the label in `ChordVoicingsViewModel` (e.g. from the tuning id). Store separately in a second store.

**Rationale.** The tuning label is a UI concern derivable only from the `TunerPreset` at the write site. Re-deriving it from `GuitarTuning.id` in the ViewModel would couple the Chord Finder to tuner ID naming conventions. Carrying it once at publish is simpler and more correct. A second store for the label alone would create unnecessary coupling.

---

## 2026-06-07 — Phase 8.3: Seed algorithm reads LatestKeyResultStore.topResult.value once at init

**Decision.** `ChordFinderViewModel.init` reads `latestKeyResultStore.topResult.value` synchronously (a single `.value` snapshot), not via `collect`. Once the seed decision is made, later Key Finder recomputes never override a seeded or user-owned selection.

**Alternatives considered.** Collecting from `topResult` reactively so the Chord Finder always tracks the latest Key Finder result.

**Rationale.** The product spec is "seed-once, then user-owned". A reactive subscription would violate this by silently changing the user's current view when they are in a different tab. The synchronous read guarantees the seed happens exactly once.

---

## 2026-06-07 — Phase 8.3: ChordFinderUiState uses spelledRoot + scaleType instead of a title String

**Decision.** `ChordFinderUiState` exposes `spelledRoot: String` (from `ScaleSpeller.rootName()`) and `scaleType: ScaleType` rather than a pre-formatted `title: String`. The composable combines them using `stringResource(scaleType.primaryLabelKey, spelledRoot)`.

**Alternatives considered.** A pre-formatted `title: String` computed in the ViewModel; injecting an Android `Context` into the ViewModel.

**Rationale.** `ScaleType.primaryLabelKey` is a string-resource key (e.g. `"scale_type_label_natural_minor"`). Resolving it in the ViewModel requires a Context dependency, which violates the domain's zero-Android-dependency rule. Delegating string resource formatting to the composable is the standard Compose pattern.

---

## 2026-06-07 — Phase 8.3: ChordVoicingsViewModel is not @HiltViewModel in Phase 8.3

**Decision.** `ChordVoicingsViewModel` extends `ViewModel()` with explicit constructor parameters (`ChordKey`, `chordName`, `noteNames`, `VoicingRepository`, `SelectedTuningStore`) but carries no `@HiltViewModel` or `@Inject` annotation. Phase 8.5 will convert it to `@HiltViewModel` + `SavedStateHandle` for navigation-arg extraction.

**Rationale.** Phase 8.3 has no UI or navigation routes. Adding `@HiltViewModel` now would require `SavedStateHandle` wiring that has no callsite until 8.5. The plain constructor form is unit-testable without Hilt and is a trivial upgrade in Phase 8.5.

---

## 2026-06-07 — Phase 8.3: Unsupported tier falls back to a second Standard lookup

**Decision.** When `VoicingRepository.lookup(chord, tuning)` returns `VoicingLookupResult.Unsupported`, `ChordVoicingsViewModel` performs a second lookup against `GuitarTuning.STANDARD_6` and surfaces those voicings with `tier = UNSUPPORTED`. The UI (Phase 8.5) renders them with a "shown for standard tuning" indicator.

**Alternatives considered.** Expose an empty voicings list for unsupported tunings; combine the fallback lookup inside the repository.

**Rationale.** An empty diagram screen is a worse UX than diagrams with a disclaimer, especially for users with drop tunings who can manually compensate. Keeping the fallback in the ViewModel (not the repository) preserves SRP — the repository reports what it knows; the ViewModel decides the fallback policy.

---

## 2026-06-07 — Phase 8.4: Info affordance uses an AlertDialog with a brief explainer

**Decision.** The info icon (outlined `Info`) on the Chord Finder list screen opens a Material3 `AlertDialog` with a one-paragraph body explaining the screen's purpose and a "Got it" dismiss button. No bottom sheet or custom dialog is used.

**Alternatives considered.**
- *Bottom sheet* — heavier, requires `ModalBottomSheetLayout` and scaffold coordination; disproportionate for a one-paragraph blurb.
- *Tooltip* — not reliably accessible on touch screens; long-press invocation is non-obvious.
- *No info affordance* — DESIGN.md §8.4 explicitly calls for an info `ⓘ` affordance; omitting it would violate the spec.

**Rationale.** DESIGN.md §14 lists the info-sheet content as an "open question" to resolve. An `AlertDialog` is the lightest well-supported primitive that satisfies the requirement: accessible, dismissible, no extra scaffold plumbing. All strings live in `strings.xml` so the copy can be updated without touching code.

---

## 2026-06-08 — Phase 8.5: ChordVoicingsViewModel converted to @HiltViewModel + SavedStateHandle

**Decision.** `ChordVoicingsViewModel` is now `@HiltViewModel @Inject constructor(savedStateHandle, voicingRepository, selectedTuningStore)`. Navigation arguments `rootPc` (Int), `quality` (String, ChordQuality enum name), and `chordName` (String, URL-encoded) are extracted from `SavedStateHandle`. Note names are derived internally from `ChordKey` using `ChordQuality.intervalsFromRoot` and a chromatic name array — they are no longer passed as a navigation argument.

**Alternatives considered.**
- *Keep plain constructor + factory ViewModel* — requires a `ViewModelProvider.Factory` and coupling in the navigation composable.
- *Pass note names as a nav arg* — requires URL-encoding a comma-separated list; fragile and unnecessary since the information is fully derivable from `ChordKey`.

**Rationale.** `SavedStateHandle` is the idiomatic pattern for extracting nav args in Hilt ViewModels. Deriving note names in the ViewModel keeps navigation args minimal and avoids double-encoding risks. This supersedes the Phase 8.3 decision not to use `@HiltViewModel`.

---

## 2026-06-08 — Phase 8.5: FretboardRenderModel is a presentation-layer type in chordfinder/presentation/ui

**Decision.** `FretboardRenderModel` and its `Voicing.toRenderModel()` extension live in `chordfinder/presentation/ui/`. `FretboardDiagram` lives in `ui/components/` and imports the model from the feature package. No shared `ui/model/` package is introduced.

**Alternatives considered.**
- *Move the model to `ui/components/`* — would make it a shared UI type detached from its only consumer. The mapper extension would also move there, fragmenting chordfinder presentation logic.
- *Create a new shared `ui/model/` package* — premature; no other feature currently needs a `FretboardRenderModel`.

**Rationale.** `FretboardRenderModel` is chordfinder-specific. In a single-module app, placing it with its consumer (chordfinder) is simpler than inventing a shared layer. If a second feature ever needs fretboard rendering, the model can be promoted at that point.

---

## 2026-06-08 — Phase 8.5: CAGED shape names omitted from voicings screen

**Decision.** Voicing cards show fret-range labels (e.g. "FR 3–7"), technique badges (OPEN / BARRE / SHAPE), and fretboard diagrams. CAGED shape names (E-shape, A-shape, etc.) are never shown.

**Alternatives considered.** Showing CAGED names as subtitles or tooltips on each card; toggling them on/off.

**Rationale.** CAGED is a teaching framework, not a universal standard. Many guitarists do not learn via CAGED; displaying those names would confuse as many users as it would help. The fret-range label and technique badge carry all the information a guitarist needs to locate and play the chord. CAGED support can be added as a user preference in a future release.

---

## 2026-06-08 — Phase 8.5: Tier-3 unsupported tuning notice rendered as an inline card above the grid

**Decision.** When `tier == UNSUPPORTED`, a quiet one-line notice box (BgElev1 background, Caption text, Radius.Md) appears in the header section above the voicing grid. It uses the existing `cf_unsupported_tuning_notice` string. No modal, no snackbar, no empty state.

**Alternatives considered.** Snackbar (dismissed too quickly; user may miss it); modal dialog (too disruptive for a non-blocking notice); empty state with illustration (misleading — voicings are shown).

**Rationale.** An inline notice is persistent (stays visible while the user scrolls), non-blocking (doesn't require dismissal), and contextual (right above the diagrams it qualifies). Consistent with the app's flat, text-driven visual language.

---

## (Template for future entries)

## YYYY-MM-DD — Short title of decision

**Decision.** What was chosen.

**Alternatives considered.** What else was on the table, briefly.

**Rationale.** Why this choice won.

**Supersession trigger.** (Optional) What would cause us to revisit.
