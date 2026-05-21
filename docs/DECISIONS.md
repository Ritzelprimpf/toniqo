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

## (Template for future entries)

## YYYY-MM-DD — Short title of decision

**Decision.** What was chosen.

**Alternatives considered.** What else was on the table, briefly.

**Rationale.** Why this choice won.

**Supersession trigger.** (Optional) What would cause us to revisit.
