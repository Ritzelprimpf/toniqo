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

## 2026-07-19 — Info/More menu scope reduced to Help, Licenses, Feedback, Support the Project

**Decision.** For the initial release, the Info/More menu contains four items: Help, Open Source Licenses, Feedback, and "Support the Project". Feedback and Support the Project are both external links (GitHub Issues and GitHub Sponsors respectively) opened via `Intent.ACTION_VIEW` directly, with no dedicated in-app screen. The Privacy Policy and Rate & Share screens, routes, and strings are deleted from the codebase rather than hidden.

**Alternatives considered.**
- *Keep Privacy Policy/Rate & Share code but hide from menu.* Rejected by explicit user choice — avoids unused code sitting around per CLAUDE.md §10; can be re-added when actually needed (e.g. ahead of a Play Store submission, which likely requires a Privacy Policy).
- *Give "Support the Project" its own screen.* Rejected — an external link needs no intermediate screen, and skips re-introducing the "no back button" problem being fixed elsewhere in this same session.

**Rationale.** The user wants a minimal, non-pushy way for people to say thanks (GitHub Sponsors) without the tone of "give me money," and doesn't want unfinished/unneeded menu items shipped. "Support the Project" was chosen as the label over "Say Thanks" or "GitHub Sponsors" (user's pick from options).

**Known gap.** `GITHUB_SPONSORS_URL` in `InfoHomeScreen.kt` is a placeholder (`https://github.com/sponsors/REPLACE_ME`) — must be replaced with the real URL before shipping. `GITHUB_ISSUES_URL` is already set to the real repo (`https://github.com/Ritzelprimpf/toniqo/issues`).

**Supersession trigger.** Play Store submission will likely require a Privacy Policy again — re-add that screen at that point rather than un-deleting old code verbatim, since requirements may have changed.

**Update 2026-08-09.** The "Support the Project" row is now hidden behind `SUPPORT_ROW_ENABLED = false` in `InfoHomeScreen.kt` — GitHub Sponsors clearance hasn't come through yet, so the row (and its still-placeholder URL above) is hidden rather than shipped live. Kept in code, not deleted, per this entry's own reasoning about unfinished items — the difference here is it's blocked on external clearance, not on being unfinished work. Flip the flag back to `true` once cleared; the Feedback row's `isLast` was updated to `!SUPPORT_ROW_ENABLED` so the card list still ends cleanly either way.

---

## 2026-08-08 — Tuner auto-advance gets its own haptic, ring flash, and pill-highlight transition

**Decision.** Auto-advance (moving the tuner's target to the next string, 200ms after the previous string is confirmed in tune) now has its own dedicated cue, separate from the "string tuned" cue that already existed:
- A new `TunerEvent.StringAdvanced(stringIndex)`, emitted at the exact moment `currentStringIndex` increments in `TunerViewModel.onStringSustainedInTune()`.
- A distinct haptic (`HapticFeedbackType.TextHandleMove` via `stringAdvancedHaptic()`), deliberately different from `tunedStringHaptic()`'s `LongPress`.
- A short mint `SuccessRing` flash around the readout well — the same component/colour already used for the ALL_STRINGS_TUNED celebration, reused here with a much shorter 250ms hold (`ADVANCE_FLASH_HOLD_MS` in `TunerScreen.kt`) instead of the celebration's 1200ms, before SuccessRing's existing 320ms fade-out.
- `StringPill`'s active border/halo also cross-fades (320ms, `LinearOutSlowInEasing`, matching `SuccessRing`'s fade timing; instant under reduced motion) instead of snapping instantly between pills.

**Alternatives considered.**
- *Haptic + pill cross-fade only, no ring flash.* Shipped first; user feedback was "too subtle" — the pill highlight alone didn't draw the eye enough. Superseded by adding the ring flash.
- *Build a brand-new visual element for the flash.* Rejected — reusing `SuccessRing` as-is (just with a shorter externally-driven hold, which its own doc comment already anticipated: "The 1200ms hold is driven externally") avoids introducing a second glow/ring pattern into the design language.

**Rationale.** User-reported bug: with auto-advance on, the user didn't notice when the tuner moved to the next string. Root cause: the only existing feedback (haptic pulse + check-mark) fires at the "string is in tune" moment; the actual advance 200ms later — where the readout well silently re-targets — had zero signal. The haptic + pill cross-fade alone wasn't noticeable enough per direct user feedback, so a short flash of the existing mint success-ring was added on top — same colour/component as the established "success" language, just briefer, keeping it consistent with DESIGN.md §9's "tools, not toys" principle (no new celebratory pattern, just a shorter use of an existing one).

**Known gap.** The last-string case (auto-advance transitioning into `ALL_STRINGS_TUNED`) intentionally does *not* emit `StringAdvanced` — that transition already has its own dedicated feedback (`SuccessRing` at its full 1200ms hold + `allTunedHaptic`).

---

## 2026-08-09 — Chord diagrams anchor to the nut instead of the lowest fretted note

**Decision.** `Voicing.toRenderModel()` (`FretboardRenderModel.kt`) now picks the diagram's fret window base as follows:
- **Barre voicings** (`barre != null`) window from `baseFret` (the lowest fret in the shape) as before — unchanged, since that already equals the barre fret for every barre voicing in the curated data.
- **Non-barre voicings** (open or fingered shapes) window from the nut (`base = 1`) whenever the whole shape fits in one window from there (`fretRange.last <= FRET_WINDOW_SIZE`). Shapes that can't reach the nut (e.g. an open-position voicing using a fret above the window size) fall back to windowing from `baseFret`, unchanged from before.

**Alternatives considered.**
- *Force barre chords to window from `barre.fret` specifically, rather than `baseFret`.* Rejected after checking the curated voicings JSON: 13 "hybrid" voicings (mostly `AUGMENTED` quality) pair a partial barre with an individually-fretted note *below* the barre fret. Windowing from `barre.fret` there would push that lower note off the top of the visible window (negative `fretWithinWindow`). `baseFret` already equals `barre.fret` for every ordinary full-barre voicing, so leaving barre-chord windowing untouched satisfies "bar chords start where the bar is" without breaking these edge cases.
- *"Center" the shape within the window (i.e. pad above and below).* Rejected — the user's own examples (open G, Em) describe nut-anchoring, not mid-window centering; nut-anchoring is also the universal convention for real-world "open position" chord charts.

**Rationale.** User-reported bug: chord diagrams for open chords (e.g. open G, `x32013`... min fret 2) and Em (`022000`, min fret 2) started their visible window at the lowest fretted note (fret 2), instead of at the nut like conventional chord charts. This left an oddly truncated diagram with no visual "give" above the fingering. Root cause was `Voicing.toRenderModel()` always windowing from `baseFret = fretRange.first`, which is the right rule for barre chords but wrong for open/shape chords that are reachable from the nut.

**Supersession trigger.** If a future non-barre voicing legitimately needs a position label despite fitting within reach of the nut (none currently exist in the curated data), this rule would need a per-voicing override rather than the blanket `fretRange.last <= FRET_WINDOW_SIZE` check.

---

## 2026-08-09 — Key Finder gets a large first-visit "add note" CTA

**Decision.** While the user has fewer than `MatchScalesUseCase.MIN_NOTES_TO_MATCH` (3) notes entered, `KeyFinderScreen`'s idle state shows a 72dp filled `signal.mint` circle with a `+` icon, centered above the existing "Add at least 3 notes to see matches" hint text (`BigAddNoteButton` in `KeyFinderScreen.kt`). It calls the same `showPickerSheet = true` action as the small 30dp dashed add-button already in the chip rail. Once 3+ notes are present, only the small rail button remains — the CTA is tied to the same 3-note gate the matching use case itself uses, not just "any notes present," since a match is never even attempted below that count.

**Alternatives considered.** Presented to the user as a choice among three styles for the CTA: a filled mint circle, a scaled-up dashed outline circle (matching the existing rail button's visual language), and a labeled `btn.primary` pill ("+ Add a note"). User picked the filled mint circle.

- *Give the new button the 24dp mint glow, matching the description offered when presenting the option.* Rejected during implementation — DESIGN.md §10/§12 permit exactly two glows app-wide (the `btn.primary` 52dp variant, and the active bottom-nav pill), and this circular button is neither, so adding a third glow would violate an explicit "Don't" even though the user's chosen preview mockup depicted one. Shipped without a glow.

**Rationale.** User-reported UX gap: the Key Finder's first-visit state was "pretty empty" with no clear call-to-action — the only way to add a note was the small 30dp dashed circle at the end of an otherwise-empty chip rail, easy to miss. DESIGN.md has no existing large circular CTA component, so per CLAUDE.md §14 the exact visual treatment was confirmed with the user via option previews rather than guessed.

**Known gap.** No DESIGN.md §8.3 entry was added for this new component/size; DESIGN.md's own §8.3 note already flags Key Finder as spec-by-prose pending a real design pass, so this joins that backlog rather than being written into the doc as a new locked-in token.

---

## 2026-08-09 — Note picker sheet stays open across multiple note additions

**Decision.** `NotePickerSheet` (`KeyFinderScreen.kt`) no longer auto-closes when a note is tapped. The `onAddNote` callback passed from `KeyFinderScreen` now only forwards to `viewModel.addNoteFromPicker(...)`; it no longer also sets `showPickerSheet = false`. The sheet now closes only via its existing `onDismiss` (tap-outside / swipe-down), letting the user add several notes in one sitting. A tapped pitch class still becomes disabled immediately (`presentPitchClasses`, driven by `uiState.notes`), so the grid itself shows what's been added so far without needing a separate confirmation.

**Alternatives considered.**
- *Auto-close once 3 notes are present (the `MatchScalesUseCase.MIN_NOTES_TO_MATCH` threshold), otherwise stay open.* This was the fallback the user offered if fully-manual-close turned out to feel disruptive. Not needed — the reactive disable-on-add feedback is a small, expected update (not a jarring layout change), and the 3-note threshold is a matching-engine detail, not a natural "done adding notes" signal; a user picking a 5- or 6-note scale would have the sheet vanish mid-selection under that rule. Went with manual-close-only instead.

**Rationale.** User feedback: closing the sheet after every single note forced a re-open tap for each additional note, which is unnecessary friction for what's fundamentally a multi-select action. `ModalBottomSheet` already provides dismiss affordances (tap-outside, swipe-down), so no new UI element was needed to support "manually closed."

---

## 2026-08-09 — Key Finder's info button now wires up a help dialog

**Decision.** The Key Finder header's info `IconButton` (previously a no-op — see its old comment `/* Info navigation – not wired in this phase */`) now opens an `AlertDialog` explaining what the matcher needs: at least 3 distinct notes to get any results, what the FULL badge means, and that long-pressing a note to mark it tonic improves ranking and earns the TONIC badge. Implementation directly mirrors Chord Finder's existing `InfoDialog` in `ChordFinderScreen.kt` (same `AlertDialog` structure, `TextButton` "Got it" confirm) — no new component, just the established pattern applied to this module's copy.

**Rationale.** User request: Chord Finder already has a working info button with this quick-help pattern; Key Finder's identical-looking button did nothing, which reads as broken. The explanatory copy was sourced directly from `MatchScalesUseCase`'s actual scoring rules (`MIN_NOTES_TO_MATCH = 3`, the root/tonic bonus, `isFull`) rather than guessed, so it stays accurate if those thresholds ever change without a matching doc update.

---

## 2026-08-09 — Tuner smooths the displayed frequency, not the sustained-tone decision

**Decision.** `DetectTunedStringUseCase.execute()` now keeps a second small rolling buffer (`frequencyWindow`, size `FREQUENCY_SMOOTHING_WINDOW_SIZE = 2`) of the raw per-buffer frequencies from valid (non-null) detections, alongside the existing 6-element sustained-tone boolean window. The emitted `DetectionEvent.Detection.detectedFrequencyHz` and `.centsOff` are now derived from the **average** of the last 2 valid readings, not the single raw per-buffer reading. Everything that decides in-tune/auto-advance — the tolerance check that feeds the sustained-tone window, and therefore `isSustainedInTune` — still uses the **raw**, unsmoothed per-frame cents, completely untouched by this change. Note identity (`detectedNote`, and chromatic-mode target resolution) also still resolves from the raw frequency, so the note letter itself keeps switching instantly; only the fine numeric offset (needle position, Hz readout) is damped.

**Alternatives considered.**
- *Window size 3.* Shipped first, then reverted same-day on direct user feedback: "horrible... way too slow... feels like it sleeps in between tones." A 3-reading average takes 2 extra buffer cycles (~186ms) to fully displace a stale reading after a genuine tone change (new string, new note), which read as lag rather than calm. Went back to raw (no smoothing) briefly, then re-added at window size 2 per the user's follow-up: "smoothing is alright, it just was too much." 2 readings converges to a step change within a single buffer cycle (~93ms) instead of two, while still damping single-frame noise spikes — the minimum window size that still does anything (1 would be a no-op).
- *Increase the capture buffer size instead.* Would give YIN more wave periods per analysis window for low strings (see Rationale below), directly improving the raw estimate rather than papering over noise after the fact. Not done here — it changes capture latency/behavior for the whole pipeline (including the sustained-tone window's own responsiveness), a bigger and riskier change than a display-only smoothing pass. Worth a future revisit if smoothing alone doesn't feel sufficient.
- *Smooth inside `TunerViewModel` instead of the use case.* Rejected — the sustained-tone window already lives in the use case and already resets per-`execute()`-call (fresh window per new target), so adding the frequency window in the same place gets an identical, already-tested reset lifecycle for free. Keeping both concerns (raw sustain logic, smoothed display value) in one place also made it easy to guarantee the two never cross-contaminate.
- *Smooth `centsOff` directly (average of recent cents values) instead of averaging Hz and re-deriving cents.* Rejected — averaging in the Hz domain is the more physically meaningful "what frequency did we actually hear," and since the target frequency is fixed for the lifetime of a single `execute()` call, converting the smoothed Hz to cents via the existing `MusicTheory.centsBetween` afterward is equivalent and reuses code instead of introducing a second smoothing pathway.

**Rationale.** User-reported UX issue: the tuner needle "jumps a lot" on low strings, especially low E. Root cause (confirmed against `YinPitchDetector` and the buffer-size decision already recorded above): a 4096-sample buffer only fits ~3-4 wave periods of E2 (≈82 Hz) — the *minimum* YIN needs for a reliable estimate, per this file's own "Phase 5.2: sample rate / buffer size" entry — versus ~15 periods for a string like high E. Fewer periods means a shallower, noisier correlation dip, so the raw per-buffer estimate has more frame-to-frame variance specifically on low strings. Nothing between the raw detector output and the UI was damping that variance. A light moving average on the *display* value cuts the visible jitter without touching the actual in-tune decision, so a string is never held "not sustained" or falsely marked "sustained" because of the smoothing — it only calms what the user sees. The window size itself needed one round of live user feedback to land right (see Alternatives above) — 3 felt laggy, 2 didn't.

**Known gap.** Window size 2 and the underlying buffer size (4096 frames) were both chosen from user feedback on-device rather than instrumented measurement — if 2 still doesn't feel calm enough on a real low E string, the buffer-size alternative above is the next lever, not a larger smoothing window (which is what just got reverted for feeling laggy).

---

## 2026-08-09 — Feedback row split into Bug Report / Feature Request, loaded via in-app WebView

**Decision.** The single "Feedback" Info-menu row (previously an external-browser `Intent.ACTION_VIEW` to GitHub Issues) is replaced with two rows — "Report a Bug" and "Request a Feature" — each navigating to a new `FeedbackWebViewScreen` (`ui/info/FeedbackWebViewScreen.kt`) that loads a Tally.so form inside an in-app `android.webkit.WebView`, instead of handing off to an external browser. The two form URLs (`BUG_REPORT_URL = "https://tally.so/r/WO64gv"`, `FEATURE_REQUEST_URL = "https://tally.so/r/RGQOlj"`) are private top-level constants in that file — the only place they'd need to change if the forms move. Two new routes (`Routes.BUG_REPORT`, `Routes.FEATURE_REQUEST`) were added to the existing nested Info nav graph, reusing the same back-button + H1-title chrome as `HelpScreen`/`LicensesScreen`. `INTERNET` permission was added to the manifest (the app previously made no network calls at all). System/gesture back navigates the WebView's own history first (via `BackHandler` + `WebView.canGoBack()`/`goBack()`), only popping the screen once there's no more in-form history to unwind — otherwise following a link inside the form would strand the user outside the app on first back-press.

**Alternatives considered.**
- *Keep it external-browser like the old Feedback row.* Rejected — explicit user request: "I would like them to be loaded inside the App eg in a webview component."
- *Route the URL through nav arguments to a single generic WebView route instead of two thin wrapper composables (`BugReportScreen`/`FeatureRequestScreen`) with hardcoded constants.* Rejected as unnecessary indirection — both URLs are fixed, curated destinations (not user-provided or dynamic), so passing them as nav-route string arguments would just add URL-encoding surface area for no benefit. The two wrapper composables keep the constants colocated with their one usage each, which is what "configurable in a constant" (the user's own phrasing) asked for.
- *No loading indicator / no WebView.canGoBack() back-handling.* Considered for a smaller diff, but a bare WebView with no loading state reads as broken while the Tally form's JS boots, and without in-form back handling, tapping any link inside the form (e.g. a "privacy policy" footer link Tally forms commonly include) would trap the user with no way back except fully exiting the Info section.

**Rationale.** User feedback: "The feedback button through github does not work as I expected" — the GitHub Issues flow required a GitHub account and left the app entirely, which is more friction than a curated no-login web form needs. Two separate forms/routes were chosen (matching the user's two supplied URLs) rather than one combined form, since bug reports and feature requests likely want different structured fields on the Tally side.

**Known gap.** No test coverage for `FeedbackWebViewScreen` — `android.webkit.WebView` isn't meaningfully testable under Robolectric/JVM unit tests without heavier tooling, and the project's `androidTest` source set doesn't currently compile via CLI in this environment (pre-existing, unrelated issue — see CLAUDE.md §15). Manual verification (does the WebView actually load Tally, does back-navigation behave) is pending the user's on-device check.

**Update 2026-08-09 — file upload.** User-reported follow-up: the Tally forms' file-upload field (`<input type="file">`) did nothing when tapped. A plain `WebView` doesn't support this at all — Android requires the host app to implement `WebChromeClient.onShowFileChooser` itself and launch a system file picker on the page's behalf. Added: a `rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult())` launcher (`fileChooserLauncher`) plus a `WebChromeClient` override that launches `fileChooserParams.createIntent()` (the standard system document/content picker, no extra permission needed — it runs out-of-process via Storage Access Framework) and resolves the pending `ValueCallback<Array<Uri>>` via `WebChromeClient.FileChooserParams.parseResult(...)` when the picker returns. Camera-capture ("take a photo now" as an alternative to picking an existing file) was **not** wired up — that needs a separate `FileProvider`-backed capture intent chained into the chooser via `Intent.createChooser` + `EXTRA_INITIAL_INTENTS`, plus a `CAMERA` permission request, which is a materially bigger change than "picking an existing file" and wasn't part of what was reported broken. Revisit if a user specifically asks for in-form camera capture.

---

## 2026-08-09 — Runtime light/dark theme toggle

**Decision.** Added a user-facing "Dark Theme" switch to the Info menu (`InfoHomeScreen.kt`), persisted via a new `ThemePreferences`/`ThemePreferencesImpl` DataStore pair and a new `ThemeViewModel`. Dark is always the starting state — the toggle does **not** follow the device's system dark/light setting at all (resolves DESIGN.md §14 open question 5, previously "default to system; confirm before building"). `MainActivity` reads `ThemeViewModel.isDarkTheme` and passes it into `ToniqoTheme(useDarkTheme = ...)`; the value is threaded down as a plain parameter through `MainScreen` → `AppNavHost` → `InfoHomeScreen` rather than re-fetched via `hiltViewModel()` inside the `info_home` nav destination, because a `hiltViewModel()` call from *inside* a `NavHost` destination scopes to that destination's back-stack entry, not the Activity — re-fetching there would have produced a second, independent `ThemeViewModel` instance instead of sharing state with the root-level one.

The harder part of this change: **`Tq.Color` was previously a flat, hardcoded-dark object** — `Tq.LightColor` and the Material `ColorScheme` plumbing for light mode already existed (`ToniqoTheme.kt` already built a correct `lightColorScheme()`), but ~93% of the app's ~340 `Tq.Color.*` call sites read the raw dark object directly, never `MaterialTheme.colorScheme.*`, so the light scheme was previously unreachable in practice. Fixed by making `Tq.Color`'s properties `@Composable get()`s backed by a new `LocalTqPalette` `CompositionLocal` (payload type `TqPalette`, instances `TqPalette.Dark`/`TqPalette.Light`), which `ToniqoTheme` now provides alongside the Material `ColorScheme`. This let the ~315 ordinary composable-scope call sites keep working completely unchanged — only call sites reading `Tq.Color.*` from a **non-composable** context needed fixing:
- `TuningStatus.toSignalColor()` and `DegreeColor.of()` (the DESIGN.md §2.4-mandated status/quality → colour mappers) now take an explicit `palette: TqPalette` parameter instead of reading `Tq.Color` internally, and stayed plain (non-`@Composable`) functions on purpose — see their kdoc. Composable callers pass `Tq.Palette` (a new top-level composable property, same `LocalTqPalette` read, exposed as a value instead of per-token).
- `NeedleGauge.kt`'s five `DrawScope` draw helpers, and two inline `Canvas` draw lambdas in `KeyFinderScreen.kt`, now receive colours as parameters captured in composable scope beforehand — extending the "capture as local val before entering DrawScope" idiom `FretboardDiagram.kt`/`BeatIndicator.kt` already used.
- Two `remember(key) { lerp(Tq.Color.X, Tq.Color.Y, t) }` blocks in `KeyFinderScreen.kt` had the `remember` dropped entirely — cheap enough per-recomposition that memoizing wasn't worth losing reactivity for.

The old dark-hardcoded object is renamed `Tq.DarkColor` (parallel to the existing `Tq.LightColor`) and is now read only by `ToniqoTheme` (building the Material `ColorScheme`) and `TqPalette.Dark`'s initializer — nowhere else.

**Alternatives considered.**
- *Refactor all ~340 call sites to `MaterialTheme.colorScheme.*` instead* (the "textbook" Material 3 approach `ToniqoTheme`'s own kdoc half-envisioned). Rejected — 10×+ the blast radius of the `CompositionLocal` approach for no behavioural difference; a pre-implementation scoping pass (see below) found only 22 call sites across 5 files would actually break under the chosen approach, versus needing to touch all ~340 under this one.
- *Let the toggle follow the system theme by default, with the manual switch as an override.* Rejected — explicit user instruction: "Dark should still be the default," read as an unconditional default regardless of system setting, not merely a starting suggestion. Confirmed against DESIGN.md §14's own open question, which this resolves.
- *Make `toSignalColor()`/`DegreeColor.of()` themselves `@Composable`* instead of taking an explicit `palette` parameter. Rejected — both are covered by fast plain-JVM unit tests (`TuningStatusExtTest`, `DegreeColorTest`); making them `@Composable` would force those tests into a Compose UI test host (heavier, and the project's `androidTest` source set doesn't currently compile via CLI at all — see CLAUDE.md §15), a real testability regression just to avoid one parameter.

**Rationale.** User request: dark is the preferred/default aesthetic, but "a lot of users might prefer a lighter color scheme," with the choice persisted per-user. A scoping investigation (Explore agent, read-only) confirmed the `CompositionLocal` approach was safe before starting: only 22 of ~340 call sites (6.5%), concentrated in 5 files, would need changes — small and enumerable, not a sprawling risk.

**Known gap.** No visual QA has been done on the light palette end-to-end (colours were already defined per DESIGN.md §2.2, but never actually rendered app-wide until this change wired them up) — worth a full light-mode pass on-device before shipping, since some component may look wrong in light mode despite compiling and unit-testing cleanly (unit tests don't render pixels).

---

## 2026-08-09 — String pills play a reference tone on tap, mic muted for the duration

**Decision.** Tapping a string pill in the tuner's string selector now also plays a 1-second sine-wave reference tone at that string's target pitch (`TunerViewModel.TONE_DURATION_MS = 1_000L`), alongside its existing behaviour of jumping the tuner's target to that string. New pieces: a `TonePlayer` domain interface (`tuner/domain/repository/TonePlayer.kt`, one `suspend fun play(frequencyHz, durationMs)`), backed by `AudioTrackTonePlayer` — a `MODE_STATIC` one-shot `AudioTrack` player, unlike the metronome's continuous `MODE_STREAM` player, since a reference tone is always a short, self-contained blip with no ongoing config to stream. `ToneSynthesizer` generates the PCM sine buffer (linear fade-in/fade-out envelope to avoid a click/pop at the buffer edges), mirroring the existing `ClickSynthesizer`'s structure but taking frequency/duration as runtime parameters instead of a fixed enum, since the target pitch changes on every tap.

**The mic-feedback problem.** While a reference tone plays through the speaker, the tuner's own mic is still listening by default — on a phone, speaker output is often picked up by the on-device mic strongly enough that the app could hear its *own* reference tone and mistake it for the user's guitar, triggering a false "in tune" reading or even a spurious auto-advance purely from previewing a pitch. Fixed by muting the mic for the tone's duration: `TunerViewModel.playReferenceTone()` sets `tunerInput.value = null` before starting playback (routing the pipeline to the no-op `DetectionEvent.Listening` branch instead of `DetectTunedStringUseCase.execute()`, per the existing `flatMapLatest` wiring) and only restores the real target once `tonePlayer.play(...)` completes. Tapping a new string pill mid-tone cancels the previous tone's playback job outright — the old tone's "resume the mic" continuation never runs, only the new tap's does, so the mic never resumes for a string the user has since tapped away from.

**Alternatives considered.**
- *Let the mic keep listening during the tone (accept the risk).* Rejected — the false-positive risk is concrete and self-inflicted (the app would be reacting to its own audio output), and every plausible mitigation (echo cancellation, audio-source tricks) is strictly more complex than simply not listening for one second.
- *Stream the tone (`MODE_STREAM`, matching the metronome's player) instead of `MODE_STATIC`.* Rejected — streaming exists to support an unbounded, config-changing playback (the metronome runs indefinitely at a live-updating BPM); a reference tone has a fixed, known duration decided up front, so a single static buffer is simpler and suffices.
- *Give `TonePlayer` a `Flow`-based/collector-scoped lifetime like `MetronomePlayer`.* Rejected — that shape exists because the metronome's lifetime is "as long as something is collecting," which doesn't apply to a one-shot; a plain `suspend fun` that the ViewModel launches and can cancel via a job it already owns is simpler and sufficient.

**Rationale.** User request: "each button on press to play the correct note for one second" — a real reference-pitch feature many tuner apps have, letting the user hear the target pitch instead of only reading it off the needle/note-letter.

**Known gap.** No audio-focus request around the tone (contrast with the metronome, which requests `AUDIOFOCUS_GAIN` and abandons it on stop). For a 1-second blip this was judged not worth the added complexity — it won't meaningfully duck or interrupt background media — but revisit if that turns out to feel wrong in practice.

**Update 2026-08-09 — silent playback bug.** User-reported follow-up: no sound played at all on-device. Root cause: `AudioTrackTonePlayer`'s init check gated on `audioTrack.state != AudioTrack.STATE_INITIALIZED`, but a freshly-built `MODE_STATIC` `AudioTrack` reports `STATE_NO_STATIC_DATA` — its normal, valid pre-`write()` state — not `STATE_INITIALIZED`. Every call was therefore treated as a construction failure and bailed out silently (logged at `Log.e`, but nothing surfaced to the UI or the calling ViewModel) before `write()`/`play()` ever ran. This is exactly the kind of bug the "known gap" above already flagged as untestable from a JVM unit test — `FakeTonePlayer` never touches a real `AudioTrack`, so nothing in the test suite could have caught it; it only shows up on-device. Fixed by only treating `STATE_UNINITIALIZED` as failure, which is the only state that actually means construction failed.

**Update 2026-08-09 — too quiet.** Second on-device follow-up, once sound was actually playing: the tone was "very very quiet." `ToneParameters.AMPLITUDE` started at `0.5`, matching the metronome click's mid-tier peak (`ClickParameters.AMPLITUDE_STANDARD`). That comparison doesn't transfer: a click's perceived loudness comes from its sharp transient (attack + fast decay within ~30ms), not sustained energy, so a *sustained* 1-second tone at the same peak amplitude reads far quieter to the ear. Bumped `AMPLITUDE` to `0.9` — close to full scale, since a clean sine tone doesn't produce the same harsh clipping character a click transient would if pushed that high.

**Update 2026-08-09 — still quiet after maxing peak amplitude.** Third on-device follow-up: still too quiet, uniformly across the whole pitch range (ruling out a phone-speaker bass-rolloff explanation, which would only affect low strings). With `AMPLITUDE` already at `0.9` — a peak increase to `1.0` would gain under 1dB, not a meaningful "louder" — the real ceiling was the *waveform*, not the peak: a pure sine is the quietest possible waveform for a given peak (its RMS is only ~70.7% of peak), so there was no more headroom on the peak-amplitude lever alone. Added `ToneParameters.DRIVE` (`= 2.5`): a `tanh` soft-clip applied to the raw sine in `ToneSynthesizer.generate()` before scaling, which pushes the waveform's shoulders closer to full scale — raising RMS (perceived loudness) at the *same* peak, at the cost of adding mild harmonic content (moving away from a pure sine, toward a squarer wave). Chosen so `tanh` is odd and monotonic, meaning it reshapes the wave without moving zero-crossings, DC offset, or where the fade envelope reads zero — the entire existing `ToneSynthesizerTest` suite (edges, DC offset, zero-crossing-rate frequency check) needed no changes. Added one new test (`soft-clip drive raises RMS above a pure sine's inherent 70.7 percent of peak`) asserting the actual loudness gain, measured over the steady-state (post-fade) region of the buffer.

**Known gap (compounding the one above).** `DRIVE = 2.5` is an unmeasured first guess, same as `AMPLITUDE` was — there was no way to verify actual on-device loudness or listen for unwanted harshness/buzziness from this session. If still not loud enough, or if it now sounds too "buzzy"/distorted rather than like a clean tone, `DRIVE` is the one value to adjust (higher = louder but more square-wave-like; `1.0` ≈ back to a pure sine).

**Update 2026-08-09 — audible "crack" before each tone.** Fourth on-device follow-up, once loudness was resolved: a crack/pop right before the tone starts, on every tap. Root cause: each string-pill tap builds and plays a **brand-new** `AudioTrack` (`AudioTrackTonePlayer` has no persistent player instance — see its class kdoc), and a freshly-started `AudioTrack` has a genuine cold-start artifact as the mixer/HAL output path spins up. The metronome hit the exact same class of problem for its *first* click ("Fixed Metronome first sound Bug" — see the git history) and solved it by writing a silent priming buffer before the scheduler's anchor is stamped; that fix only had to run once per metronome session because the metronome reuses one long-lived `AudioTrack`. This tone player creates a new `AudioTrack` on *every* tap, so the same absorption has to happen every time, not once.

Fixed the same way: added `ToneParameters.WARMUP_MS = 150L` (same value/rationale as the metronome's `WARMUP_SILENCE_MS`). `ToneSynthesizer.generate()` now prepends that many milliseconds of true silence (`ShortArray`'s default `0` value) before the tone content — one `write()` + `play()` call still handles the whole thing, since `AudioTrack` is in `MODE_STATIC`, so there's no second write mid-playback to coordinate. `AudioTrackTonePlayer.play()`'s hold-open `delay()` was extended from `durationMs` to `WARMUP_MS + durationMs` to match the now-longer buffer, so cleanup doesn't cut off the tail of the actual tone. The `TonePlayer` interface's public contract (`play(frequencyHz, durationMs)` = duration of *audible tone*, not total buffer) is unchanged — `TunerViewModel` needed no changes; the extra 150ms is purely internal to how long the mic stays muted, which is harmless.

`ToneSynthesizerTest`'s buffer-length and steady-state-RMS assertions were updated for the new two-part (silence + tone) buffer shape; the zero-crossing/frequency and DC-offset checks needed no changes, since a run of exact zeros doesn't register as a crossing or shift the mean.

---

## 2026-08-09 — Voicing generator: more voicings per chord, inversions, two bug fixes

**Decision.** Reworked `tools/voicing-generator/generate_voicings.py` and the Kotlin chord-voicing domain model together to produce richer, more correct per-chord voicing sets.

Generator changes:
- `MAX_PER_CHORD` 6→5 root-position voicings, plus (new) exactly one appended inversion — 6 total per chord, comfortably clearing the "at least 4" floor with slack for hand-curation to trim.
- **Bug fix:** `MAX_SPAN` was 6 in the generator but `Voicing.kt`'s actually-enforced `MAX_FRET_SPAN` was 4 — the generator could produce (and nearly ship) unplayable 6-fret non-barre stretches the app would reject outright. Per user direction this was resolved by *raising* the Kotlin limit to meet the generator, not lowering the generator: fret spacing narrows going up the neck, so a 6-fret stretch is a real, playable reach higher up even though it wouldn't be at fret 1-2. Both now agree at 6.
- **Bug fix:** the barre-detection heuristic treated any two strings sharing the lowest fret as automatically meaning a barre spans between them — even when a string *in between* was open or fretted differently, which is physically impossible (the index finger can't lie flat across a string while another finger frets it higher, or while it needs to ring open). This was wrong in 33% of generated barre specs. Fixed to require every string within a candidate barre's span be either muted or at the barre fret.
- **New: inversions.** A second search pass (`require_root_bass=False`, threaded through `passes_filters`/`canonicalize_voicing`/`self_check_voicing`) finds voicings with the third or fifth in the bass. Exactly one is appended per chord, chosen to explicitly exclude any candidate that's just an already-selected root-position shape with one extra optional string un-muted (e.g. `x32010`'s low E left ringing becomes `032010` — flips the bass to an inversion but isn't a genuinely different shape, exactly the near-duplicate pattern this generator otherwise avoids). Verified against the regenerated draft: all 48 chords found a genuine inversion, 0 barre bugs, 0 near-duplicate pairs, 0 span violations, 0 finger-count violations.
- Reverted a same-session over-correction: `MAX_FRET`'s default was briefly lowered 15→12 to avoid fret 13-15 "filler" candidates, then reverted back to 15 per explicit user direction ("I think going up to fret 15 is a good thing").

Kotlin changes (`Voicing.kt`):
- `MAX_FRET_SPAN` 4→6, matching the generator.
- `validated()`'s invariant 3 no longer requires root-in-bass. It now instead computes the lowest sounded string's true role (root/third/fifth, from `ChordKey.quality.intervalsFromRoot`) and requires the passed-in `bassDegree` match it exactly — same "never trust the caller, verify" pattern already used for `rootStringIndices`. `ChordToneRole` already had `THIRD`/`FIFTH`/`OTHER` values sitting unused ("the inversion seam for FP-1" per its own kdoc) — this was always the intended extension point.
- `VoicingJsonParser.kt` no longer hardcodes `bassDegree = ChordToneRole.ROOT` for every parsed entry; it now computes the true value from the shape (`computeBassDegree`), so a curated inversion in the JSON parses correctly instead of failing the new invariant.

**Alternatives considered.**
- *Position-scaled max span* (tighter near the nut, looser higher up, matching physical hand-reach more precisely). Rejected for now as more complexity than requested — user picked the flat-6 option explicitly; revisit if flat 6 proves too strict high up or too loose at fret 1-2.
- *Relax the root-position search itself to allow inversions, instead of a separate pass.* Rejected — would let inversions compete with root-position shapes for the same `MAX_PER_CHORD` slots and `SPREAD_MIN_SPACING` logic, risking root-position variety getting crowded out. A separate pass with exactly one guaranteed appended slot is simpler to reason about and guarantees the ask precisely.
- *Show inversions with a "/E"-style badge or label.* Rejected (user's choice among three options) — inversions render exactly like any other voicing; a curious player will notice the different bass note on the diagram itself. No new UI work.

**Rationale.** User feedback: after a previous round of dedup work, the generator was producing too few voicings post-curation (the shipped asset sits at ~2/chord almost everywhere) with no inversions at all — and inversions were structurally impossible to ship before this change, since `Voicing.validated()` hard-required root-in-bass. The two bug fixes were discovered while investigating *why* curation kept landing on so few: much of the raw candidate pool was either unplayable (span mismatch) or carried a nonsensical barre claim, crowding out genuinely good candidates.

**Known gap.** The shipped `app/src/main/assets/chordfinder/voicings_standard_6.json` asset is **untouched** — this entry covers the generator + app-side capability only. A fresh draft (5 root-position + 1 inversion per chord, all checks clean) was written to `tools/voicing-generator/voicings_standard_6.draft.json` (untracked) for hand-curation and merge, per the tool's own documented workflow (`generate_voicings.py`'s docstring: "curate the output by hand, commit the curated JSON as the Phase 8.2 asset") — curation was not attempted here.

---

## 2026-08-09 — Fixed barre-detection regression: fingered-higher-than-barre was wrongly rejected

**Decision.** The previous entry's barre-adjacency fix was itself too strict: it required every string within a candidate barre's span to be *either muted or exactly at the barre fret* — but that rejects completely ordinary shapes like the classic F major grip (`1,3,3,2,1,1`), where fingers 2-4 press strings on top of the fret-1 barre at *higher* frets. The result was that the regenerated draft had **zero** barre voicings anywhere. Corrected the condition to `frets[i] == "x" or frets[i] >= min_fret` — the only thing actually physically impossible is an *open* string inside the span (the flat barre finger would necessarily fret it); a string fretted higher, with a second finger pressing on top, is normal and common.

Re-ran the generator after the fix: 78/288 voicings (33 of 48 chords) now carry a barre, span/finger-count/per-chord-count checks all still clean — confirms the fix didn't just trade one bug for another.

**Alternatives considered.** None — this was a straightforward regression from the previous session's over-correction, caught by direct user report ("Now, there are no bar chords at all"), not a design choice with real tradeoffs.

**Rationale.** The original "every string in the span must be exactly the barre fret" rule conflated two different things: strings the barre finger *provides the pitch for* (must be muted or barred) vs. strings a *second* finger overrides with a higher fret (fine, extremely common). Only the true physical impossibility — an open string a flat finger would necessarily press — needed rejecting.

---

## 2026-08-09 — Drop-D-family voicing generator, ChordQuality.POWER, multi-family VoicingRepositoryImpl

**Decision.** Added a second, independently-curated voicing library for 6-string drop tunings (Drop D as the reference; Drop C#/C/B/Bb/A reach it via the existing uniform-offset fret-shifting tier, same mechanism Eb/D/C#/C standard already use against `STANDARD_6`), including power chords and compact 3-4-string triads — the shapes actually used when riffing in a drop tuning, as opposed to the standard library's fuller open-position voicings.

Domain model changes:
- **`ChordQuality.POWER`** added (`intervalsFromRoot = [0, 7]`, `symbol = "5"`). Chosen over the alternatives below because a power chord genuinely has its own, distinct tone set ({root, fifth}, no third) — modelling it as a real `ChordQuality` keeps `Voicing.validated()`'s "every chord tone must sound" invariant meaningful and untouched for every other quality, rather than punching a hole in it. It is never produced by diatonic harmonization (`MusicTheory`, `ChordQualityResolver.triad` only ever resolve to the 4 triads), so it doesn't appear in the Chord Finder's scale-degree list; it's reachable only where a caller deliberately asks the voicing repository for it (today, that's just the drop-D-family JSON — there is no new UI entry point for browsing power chords directly, see Known gap below).
- **`ChordKey.classifyToneRole(pc)`** (new, in `ChordKey.kt`) replaces the index-based bass-degree classification that used to live separately in both `Voicing.kt` (invariant 3) and `VoicingJsonParser.kt` (`computeBassDegree`). The old code assumed `quality.intervalsFromRoot[1]` is always "the third" and `[2]` is always "the fifth" — true for every triad, but POWER's 2-element array puts the *fifth* at index 1 and has no third at all, so the old code would have either misclassified it or thrown `ArrayIndexOutOfBoundsException`. The replacement classifies by interval *value* (third ∈ {3,4}, fifth ∈ {6,7,8}) instead of array position, which is correct for any quality's shape and also deleted real duplication between the two call sites.
- Four exhaustive `when (quality)` blocks (`FindChordsUseCase.triadSuffix`/`buildRomanNumeral`, `ChordDegreeRow`'s quality-abbreviation lookup, `DegreeColor.of`) needed a `POWER` branch purely to keep compiling — all four are documented as unreachable in practice, since their inputs are always `ChordQualityResolver.triad()`'s return value.
- **`GuitarTuning.DROP_D_6`** added as a second reference tuning.
- **`VoicingRepositoryImpl`** generalized from one hardcoded reference tuning to a small ordered list of `(reference tuning, asset path)` families, tried in order via `uniformOffsetFrom`; the first match wins. A family whose asset doesn't exist yet (the common case here, since the drop asset isn't shipped) is treated as an empty library — lookups return `Standard` tier with zero voicings — rather than crashing, since a not-yet-curated family is an expected rollout state, not a bug.

Generator changes:
- `voicing_core.py` (new) — extracted every tuning-agnostic piece of `generate_voicings.py` (search, invariant filters, canonicalization, dominance pruning, finger/barre assignment, spread selection) into a shared module both drivers import, so a future bug fix (like the barre one two entries up) only has to happen once. Verified byte-identical output from the refactored `generate_voicings.py` vs. pre-refactor.
- Added an optional `max_sounded` cap to the search (previously only a `min_sounded` floor existed) and an `include_inversion` flag (default `True`, preserving `generate_voicings.py`'s existing behaviour) — both needed by the drop driver, neither changes the standard driver's output.
- `generate_voicings_drop_d.py` (new, separate file per explicit user direction — "easier to maintain that way") — Drop D open pcs, the 4 triads plus POWER, tighter search window (span ≤3 vs. the standard library's ≤5, capped at 3-4 sounding strings for triads / 2-3 for power chords — compact/movable, not full open-position shapes), no inversion pass (not requested, and a "power chord inversion" is a dubious concept the product didn't ask for). Generated draft: 60/60 (root, quality) pairs each get exactly 5 voicings, 0 span/finger/sounded-count violations. Cross-checked by parsing the draft through the real Kotlin `VoicingJsonParser`/`Voicing.validated()` (not just the Python-side re-implementation of the same invariants) — clean, including the never-before-exercised POWER path.

**Alternatives considered.**
- *Relax `Voicing.validated()`'s "all chord tones must sound" invariant* instead of adding `ChordQuality.POWER`, attaching a power-chord voicing to both the MAJOR and MINOR `ChordKey` for a root. Rejected (user's choice among three options) — that invariant is what stops every other voicing in the app from silently dropping a tone; weakening it for everyone to accommodate one quality is a worse trade than adding one enum value.
- *A wholly separate, parallel power-chord type* outside the `ChordKey`/`Voicing`/quality system entirely. Rejected — most isolated, but the UI/repository layer would need to know about and query two unrelated data paths for what is, structurally, just another chord quality.
- *3-note chords = root-fifth-octave specifically*, rather than "compact triads on ≤3-4 strings." Rejected (user's choice) — the compact-triad reading reuses the existing MAJOR/MINOR/DIM/AUG machinery unchanged (already reachable through today's diatonic Chord Finder UI when the tuning is drop-family), where the specific root-fifth-octave shape would have needed the same missing-third domain change as POWER for no clear added benefit.

**Rationale.** Drop tunings are used differently from standard tuning — riffs favour compact, movable 2-4-string grips (often deliberately omitting the third, which sounds muddy through distortion at low pitch) over the standard library's fuller open-position voicings. Serving that well needed both new content (a second curated library) and a real domain gap closed (power chords don't fit the existing "always 3 chord tones, always a triad" assumption baked into `Voicing.validated()` and the bass-degree classification).

**Known gap.** `voicings_drop_d_6.json` is **not shipped** — same hand-curation workflow as the standard library (see the entry above and the generator's own docstring). The draft lives at `tools/voicing-generator/voicings_drop_d_6.draft.json` (untracked). Until it's curated and placed under `assets/chordfinder/`, every Drop-D-family tuning resolves to `Standard` tier with an empty voicings list (no crash, see `VoicingRepositoryImpl` above). Separately: `ChordQuality.POWER` and the drop-family JSON schema for it now exist end-to-end, but there is **no UI entry point** to browse power-chord voicings specifically — the Chord Finder's chord list is always diatonically derived (`MusicTheory`/`ChordQualityResolver`), which never produces POWER by design. Adding a way to actually reach a POWER `ChordKey` from the UI (e.g. a toggle analogous to the existing seventh-chord one) is a product decision that wasn't part of this task and would need its own discussion.

---

## 2026-08-15 — Seventh chords: ChordKey gains a seventh dimension; sevenths derived by mutating curated triad shapes

**Decision.** Fixed a bug where selecting any seventh chord in the Chord Finder (e.g. Cmaj7) silently showed its parent triad's voicings (e.g. plain C major) instead. Root cause: `ChordKey` — the identity used to look up voicings — carried only `rootPitchClass` + triad `ChordQuality`, with no seventh dimension at all, and `ChordFinderViewModel.selectChord()` built the lookup key from `degreeChord.triadQuality` only, discarding `degreeChord.seventhQuality`. Separately, no seventh-chord voicing data existed anywhere — `ChordQuality` itself has no seventh variants, and both curated JSON libraries only ever contained MAJOR/MINOR/DIMINISHED/AUGMENTED(/POWER) entries.

Fixed end-to-end:
- `ChordKey` gained `val seventhQuality: SeventhQuality? = null` — a seventh chord is now a distinct key from its parent triad (`ChordKey(0, MAJOR, MAJOR_SEVENTH)` ≠ `ChordKey(0, MAJOR)`), each backed by its own curated voicing set. Default `null` keeps every existing 2-arg call site source-compatible.
- `SeventhQuality` gained `semitonesFromRoot` (mirrors the intervals already implicit in `ChordQualityResolver.seventh()`), used to compute the chord's 4th pitch class wherever needed.
- `ChordToneRole` gained `SEVENTH`, and `ChordKey.classifyToneRole()` classifies it — needed so an inverted seventh-chord voicing (seventh in the bass) can be validated/labelled correctly, same as `THIRD`/`FIFTH` already were for triad inversions.
- `Voicing.validated()`'s required-pitch-classes computation now adds the seventh's pitch class when `chordKey.seventhQuality != null`, so a seventh-chord voicing must sound all 4 tones, not just the triad's 3.
- `VoicingJsonParser` parses an optional `seventhQuality` field per chord entry.
- `VoicingRepositoryImpl` now loads a second asset per tuning family (`voicings_standard_6_seventh.json`, `voicings_drop_d_6_seventh.json`) and merges it into the same lookup map as the triad asset — safe as a plain `+` union since triad keys always have `seventhQuality == null` and seventh keys never do, so the two maps can never collide. A missing seventh asset behaves the same as a missing triad asset already did: empty, not a crash.
- `ChordFinderViewModel.selectChord()` now passes `degreeChord.seventhQuality` through into the lookup `ChordKey`. `Routes`/`AppNavHost`/`ChordVoicingsViewModel` carry it as a new optional query-style nav arg (`?seventhQuality=...`, nullable, defaulting to absent) so triad navigation keeps its existing URL shape.
- `ChordVoicingsViewModel.deriveNoteNames()` appends the seventh's spelled name as a 4th entry when present.

Generator tooling: two new driver scripts, `generate_seventh_voicings.py` / `generate_seventh_voicings_drop_d.py`, sharing `voicing_core.py` with the existing triad drivers. Unlike the triad drivers (which search the whole fretboard from scratch), these **never** run an independent search — they read the **curated** triad JSON under `assets/chordfinder/` and, for each already-approved triad shape, try to derive a seventh-chord shape via a new `mutate_add_seventh()` in `voicing_core.py`: mutate exactly one currently-sounded, non-bass string whose note is doubled elsewhere in the shape, changing its fret so it sounds the seventh instead, keeping everything else (which strings sound, the bass note/degree, all other frets) identical. A triad shape with no doubled tone to sacrifice yields no derivative for that shape and is reported at the end, not silently dropped. Output goes to new, separate files (`voicings_standard_6_seventh.json`, `voicings_drop_d_6_seventh.json`) — the existing hand-curated triad files are never read-write, only read.

Ran both generators against the real curated assets: 84/84 (root × triad-quality × applicable-seventh) combinations produced 2–6 voicings each, zero empty entries, in both the standard and drop-D families.

**Alternatives considered.**
- *Fresh independent search per seventh chord* (reuse `generate_voicings()`'s existing combinatorial search with a 4-tone pitch-class set, ignoring the specific curated triad shape — same mechanism used for the triads themselves before curation). Rejected (user's choice among two options) — would produce shapes unrelated to, and not vetted the way, the triad shapes the user already hand-reworked; the mutation approach guarantees every seventh voicing is anchored to a fingering already approved.
- *Encode the seventh as a 5th/6th `ChordQuality` enum value* instead of a separate `seventhQuality` field on `ChordKey`. Rejected — `ChordQuality`'s kdoc already documents it as strictly the 4 triads + POWER, produced only by diatonic harmonization; conflating it with seventh chords (which are a triad *plus* a 4th tone, not a wholly different triad shape) would have broken that invariant and duplicated the triad/seventh split `DegreeChord` already models separately.
- *Ship the generated seventh JSON directly into `assets/chordfinder/` as part of this change.* Rejected — matches the project's own generator convention (`README.md`'s "Curate and commit" step): raw generator output needs human review for awkward/unplayable shapes before becoming a shipped asset, same as every triad library before it. The seventh files were left as review output in `tools/voicing-generator/` (untracked), not copied into `assets/`.

**Rationale.** The bug was a genuine identity-model gap, not a data gap alone: even with perfect seventh-chord voicing data available, the old `ChordKey` had no way to distinguish "C major" from "C major seventh" as lookup targets. Closing that gap needed a small, symmetric extension (mirroring `DegreeChord`'s existing `triadQuality`/`seventhQuality` split) rather than a new parallel data path. Deriving seventh shapes from the curated triads (rather than searching fresh) keeps the two curated libraries in sync by construction — re-running the seventh generator after any future triad re-curation regenerates matching sevenths for free.

**Known gap.** `voicings_standard_6_seventh.json` and `voicings_drop_d_6_seventh.json` are **not shipped** — they exist only as review output under `tools/voicing-generator/` (untracked), per the alternative above. Until they're curated and copied to `assets/chordfinder/`, `VoicingRepositoryImpl` treats every seventh-chord lookup as "matched family, zero curated voicings" (empty list, not a crash, not a fallback to the triad) — the code-side fix is real and tested (`VoicingJsonParserTest`, `VoicingRepositoryImplTest`'s merge tests, `ChordKeyTest`/`VoicingTest`'s new `SEVENTH` coverage, a `ChordFinderViewModelTest` regression test reproducing the original bug), but the app won't show real seventh-chord diagrams to end users until that curation pass happens.

---

## 2026-08-15 — Shared ScreenHeader component across Tuner, Metronome, Key Finder, Chord Finder

**Decision.** Extracted the kicker-line-above-H1-title header pattern, already duplicated near-identically across Key Finder and (as of the same session's earlier fix) Chord Finder, into a shared `ui/components/ScreenHeader.kt` composable, and migrated all four feature screens (Tuner, Metronome, Key Finder, Chord Finder) to use it — user's explicit request after noticing the duplication while reviewing the Chord Finder header fix.

`ScreenHeader(title: String, kicker: @Composable () -> Unit, trailingAction: (@Composable BoxScope.() -> Unit)? = null)` renders only the structural skeleton: a `Box` containing a `Column` (kicker content, `Tq.Sp.s2` spacer, H1 title text), plus an optional trailing action the caller positions itself (typically via `Modifier.align(Alignment.TopEnd)` inside the `BoxScope` receiver) — mirroring the "info button floats independently so its touch target doesn't inflate the kicker line and push the title down" fix Key Finder's header already had. Both `kicker` and `trailingAction` are composable slots, not fixed content, because every screen's actual kicker/action differs:
- Key Finder / Chord Finder: plain `Tq.Type.Kicker` text kicker, `Icons.Outlined.Info` trailing action (default `IconButton` sizing).
- Metronome: `MetronomeStatusKicker` (pulsing dot + dynamic RUNNING/STOPPED text) as the kicker, no trailing action.
- Tuner: `ReferencePitchKicker` (mic-active dot + reference-pitch text) as the kicker, `Icons.Outlined.Settings` trailing action sized per DESIGN.md §8.1's `Tq.Sp.s10` (40dp) icon-round spec — a size distinct from Key Finder/Chord Finder's, deliberately preserved unchanged rather than homogenized.

`ReferencePitchKicker` was slimmed to render only the dot+text kicker line (dropped its former `presetDisplayName`/H1-title and `onSettingsClick`/settings-button responsibilities, which moved to `TunerScreen`'s `ScreenHeader` call) — no other file referenced its old 3-parameter signature.

**Alternatives considered.**
- *Standardize the trailing action's icon size/style across all four screens* as part of the shared component (e.g. force everything to Key Finder's default `IconButton` sizing). Rejected — Tuner's 40dp icon-round settings button is a documented DESIGN.md §8.1 value; changing it would be a visual spec change disguised as a refactor, not requested and not something to improvise per CLAUDE.md §14.
- *A plain `String` kicker parameter* instead of a composable slot. Rejected — would only fit Key Finder/Chord Finder; Metronome's and Tuner's kickers both carry a leading dot plus dynamic content that a string can't express, and forcing them to a string would have required dropping real information (the pulsing/mic-active dots) to fit the abstraction.

**Rationale.** The duplication was real (two screens with byte-for-byte-identical Box/Column/Spacer/Text skeletons before this change, a third about to grow the same way), and the parts that differ per screen (kicker content, trailing action content/size) were already cleanly separable as slots without forcing any screen's actual appearance to change — the shared component captures exactly the structural rule (kicker above title, action floats independently) and nothing else.

**Known gap.** None — verified via `compileDebugKotlin` (clean) and the full `testDebugUnitTest` suite (unaffected, no unit tests target these composables directly). `MetronomeContentTest.kt` (an instrumented Compose UI test asserting kicker/title text via `onNodeWithText`, semantics-based rather than layout-tree-position-based) is unaffected by the `ScreenHeader` wrapping, but could not be *run* to confirm — this environment has no `adb`/emulator, and a pre-existing, unrelated `androidx.compose.ui.test` API mismatch (`onNode`/`onAllNodes` unresolved) already breaks `compileDebugAndroidTestKotlin` for the whole module, predating this change (confirmed via `git log` — last touched in an old "Phase 6.4" commit, files untouched by this change).

---

## 2026-08-15 — Removed the Tuner's `MIC LIVE` indicator (broken vertical-text layout)

**Decision.** Removed the `MIC LIVE` mint-dot-plus-label indicator from the Tuner's preset chip row entirely, per explicit user report: it was rendering as unreadable single-character-per-line vertical text instead of the intended horizontal `MIC LIVE` label.

Root cause: `PresetChip`'s internal layout puts its label `Box` inside a `Modifier.weight(1f)` within its own `Row`. A weighted child implicitly requires its `Row` to claim the full width available to it (weight has no defined meaning otherwise), so `PresetChip` — even though never explicitly given `fillMaxWidth()` at its call site — expanded to claim essentially all of the outer `PresetChipRow`'s width when placed as its first, unweighted child. That left the sibling `MIC LIVE` `Row` almost no width to measure into, and Compose's default `Text` wrapping broke the string one character per line — reading as vertical text. This was very likely a pre-existing bug (not introduced by the same session's `ScreenHeader` refactor, which never touched horizontal space above `PresetChipRow`), just not previously noticed/reported.

Since `PresetChipRow` become a pure one-line pass-through to `PresetChip` once the indicator was deleted, and it had exactly one caller (`TunerScreen.kt`, no tests/previews), deleted `PresetChipRow.kt` entirely rather than leaving a pointless wrapper — `TunerScreen.kt` now calls `PresetChip` directly. Also removed the now-unused `tuner_mic_live` string resource and updated `DESIGN.md` §8.1 (Tuner preset chip row + idle-state bullets) to stop documenting an indicator that no longer exists.

**Alternatives considered.**
- *Fix the layout bug and keep the indicator* (e.g. give `PresetChip` its own bounded width via `Modifier.weight` at the call site, or drop its internal `weight` in favour of `wrapContentWidth`, so both children of the outer row get their natural size). Not pursued — user's request was explicitly to remove the indicator, not preserve it in a fixed form; the underlying `PresetChip` layout bug is separately worth revisiting only if something else in `PresetChip` needs its label to truncate/ellipsize against a bounded width, which nothing currently does (its labels are always short, fixed-format strings like "6-STRING · DROP").

**Rationale.** Direct user request to remove a visibly broken UI element; no product requirement depends on `MIC LIVE` surviving in some fixed form, so removing it (rather than debugging and preserving it) is the correct, minimal-scope response.

**Known gap.** None — verified via `compileDebugKotlin` (clean) and the full `testDebugUnitTest` suite (unaffected; no tests referenced `PresetChipRow` or `tuner_mic_live`). Not visually re-verified in a running app (no `adb`/emulator in this environment, same limitation noted in the preceding `ScreenHeader` entry).

---

## 2026-08-15 — ScreenHeader extended to back-navigable sub-pages, with an inline back arrow and meaningful kicker text everywhere

**Decision.** Extended `ui/components/ScreenHeader.kt` (previously used only by the four top-level tab screens) to also cover every back-navigable sub-page: Chord Voicings, and the Info section's Help / Licenses / Bug Report / Feature Request screens — user's explicit follow-up request after the tab-screen consolidation.

Added `onBack: (() -> Unit)? = null` to `ScreenHeader`. When non-null, it renders a standardized back arrow (`Icons.AutoMirrored.Outlined.ArrowBack`, `fg.secondary` tint, new shared `common_cd_back` string) inline, immediately before the H1 title, on the same line — the user's explicit ask ("the back-arrow can be on the same line as the big header"), replacing every sub-page's previous pattern of a back button alone on its own line/row above a separate title line. Unlike the existing `trailingAction`/`kicker` slots, `onBack` is not a composable slot: every back button in the app already rendered byte-identically (confirmed via a codebase-wide search — five call sites, one pattern), so standardizing it removes real duplication rather than forcing an artificial shared look.

Consolidated the two duplicate "Back" content-description strings (`cf_cd_back`, `info_cd_back`) into one shared `common_cd_back`, now owned by `ScreenHeader` itself rather than repeated at each call site.

Added a kicker line to every sub-page — user's explicit ask ("provide some meaningful text for the sub-headers"), since Help/Licenses/Bug Report/Feature Request had none at all, and Chord Voicings' existing kicker (`"VOICINGS · {chord name}"`) duplicated the H1 title now sitting right next to it. Settled on a `{PARENT SECTION} · {PAGE}` format mirroring the existing top-level kickers' own `SECTION · DETAIL` style (`CHORD FINDER · DIATONIC`, `TUNER · A4 = 440 HZ`):
- Chord Voicings: `CHORD FINDER · VOICINGS` (static now — the chord name itself is the H1 title, no longer interpolated into the kicker too).
- Help: `INFO · HELP`. Licenses: `INFO · LICENSES`. Bug Report: `INFO · BUG REPORT`. Feature Request: `INFO · FEATURE REQUEST`.

Each sub-page's `ScreenHeader` call uses asymmetric horizontal padding (`start = Tq.Sp.s3`, `end = Tq.Sp.s5`, not the tab screens' symmetric `Tq.Sp.s5`) — carried over unchanged from Chord Voicings' pre-existing back-row code, which already worked this out: the back arrow's icon glyph sits inset within its own touch target, so a smaller container start-padding is needed for the glyph to visually line up with the kicker text above it. Applied uniformly to all five sub-pages for consistency, rather than only preserving it where it already existed.

Where a sub-page's outer `Column` previously carried a single `.padding(horizontal = Tq.Sp.s5)` spanning both header and body content (Help, Licenses), that padding was moved down to wrap only the body content, so the header could carry its own distinct asymmetric padding without the two stacking.

**Alternatives considered.**
- *Keep `onBack` as a composable slot*, like `trailingAction`. Rejected — every back button already looked identical across all five call sites; a slot would just make callers repeat the same five lines of icon/tint/content-description code `ScreenHeader` can now own once.
- *A per-screen bespoke kicker taxonomy* (e.g. "FEEDBACK" as a category distinct from "INFO" for the two web-form screens). Rejected in favour of the literal `{PARENT SECTION} · {PAGE}` pattern — inventing a new category not reflected anywhere in the actual navigation hierarchy (`Routes.INFO_GRAPH` covers all four Info sub-pages equally) would be guessing at an information architecture the product hasn't defined, where the literal parent-graph name is already unambiguous and consistent with how Chord Finder's own kicker names its section.
- *Leave Chord Voicings' kicker dynamic* (`"VOICINGS · {chord}"`) alongside the newly-adjacent H1 chord name. Rejected — the two would show the same chord name twice, immediately next to each other, which reads as a mistake rather than information.

**Rationale.** Same motivation as the tab-screen consolidation: real, growing duplication (five near-identical back-button-plus-title blocks) collapsed into one shared structural rule, while every screen-specific piece (icon/size of any trailing action, the kicker's exact text) stays exactly as specific as it needs to be.

**Known gap.** None — verified via `compileDebugKotlin` (clean) and the full `testDebugUnitTest` suite (unaffected; no unit tests target any of the five migrated composables directly, confirmed via search). Not visually re-verified in a running app — same `adb`/emulator limitation noted in the preceding two 2026-08-15 entries.

---

## 2026-08-15 — Rate/Share/Data Privacy rows; FeedbackWebViewScreen generalized to WebViewScreen

**Decision.** Added three rows to the Info section, per explicit user request: "Rate This App" (opens the Play Store listing), "Share This App" (system share sheet with the Play Store link), "Data Privacy" (in-app WebView of `https://toniqo.ritzelprimpf.de/datenschutz.txt`).

"Rate This App" uses the standard Android pattern: `Intent.ACTION_VIEW` on `market://details?id={applicationId}` with `setPackage("com.android.vending")` (so it opens the Play Store app directly rather than showing an ambiguous chooser if some other installed app also registers the `market://` scheme), falling back to the `https://play.google.com/store/apps/details?id={applicationId}` web URL inside a `try`/`catch(ActivityNotFoundException)` for devices without the Play Store app installed. `{applicationId}` is `BuildConfig.APPLICATION_ID`, not a literal string — single source of truth with `app/build.gradle.kts`'s `applicationId`.

"Share This App" uses a plain `Intent.ACTION_SEND` / `text/plain` / `Intent.createChooser`, sharing the same Play Store URL (also built from `BuildConfig.APPLICATION_ID`, which happens to produce byte-identical output to the literal URL the user specified, since it equals the app's own `applicationId`).

"Data Privacy" reuses the existing in-app WebView screen — renamed from `FeedbackWebViewScreen.kt`/`FeedbackWebViewScreen` to `WebViewScreen.kt`/`WebViewScreen` (private composable) since it's no longer feedback-form-specific once a third, unrelated use case (a static legal document, not a Tally form) uses it. `BugReportScreen`/`FeatureRequestScreen` (existing) and the new `DataPrivacyScreen` are three thin public wrappers around the one shared private implementation, matching the file's existing wrapper pattern. `WebViewScreen`'s file-upload/`WebChromeClient` machinery (needed for Tally's forms) is harmless-but-unused for the plain-text privacy document — no new parameter was added to disable it, since a `.txt` response has no JS or file inputs to trigger it regardless.

Added `Routes.DATA_PRIVACY` and wired it in `AppNavHost.kt`, following the exact same nested-info-graph pattern as `HELP`/`LICENSES`/`BUG_REPORT`/`FEATURE_REQUEST`.

**Alternatives considered.**
- *Google Play Core's in-app review API* (`ReviewManager`) for "Rate This App" instead of a plain market:// intent, which shows an in-app rating dialog without leaving the app. Rejected — a new dependency requiring explicit approval per CLAUDE.md §8, not requested (user asked for "pretty default" behavior, which the market:// intent pattern already is — it's the standard approach most apps use).
- *A separate, simpler WebView composable for Data Privacy* instead of reusing/renaming `FeedbackWebViewScreen`. Rejected — the existing implementation already does exactly what's needed (`ScreenHeader` chrome, loading spinner, WebView history back-navigation); duplicating it for a cosmetic "not really feedback" distinction would be pure duplication for no behavioral gain, whereas renaming the file/composable to a name that fits all three current uses removes the naming mismatch instead of accepting it.
- *Hardcode the literal Play Store URL the user provided*, verbatim, for both "Rate This App"'s fallback and "Share This App". Rejected in favour of building it from `BuildConfig.APPLICATION_ID` — produces the exact same string today, but stays correct automatically if `applicationId` ever changes, rather than needing a manual find-and-replace across two more call sites plus the two already in `build.gradle.kts`.

**Rationale.** Straightforward feature addition; the one design choice worth recording is the `WebViewScreen` rename, since it's the kind of "quietly reuse a component under a name that no longer fits" drift that's worth catching immediately rather than accumulating.

**Known gap.** None — verified via `compileDebugKotlin` (clean, confirms `Icons.Outlined.PrivacyTip`/`Share`/`Star` resolve from the already-present `material-icons-extended` dependency) and the full `testDebugUnitTest` suite (unaffected). The `market://` / Play Store intents cannot be exercised at all without a real device or emulator with Play Store installed — same `adb`/emulator limitation noted in every entry above; this one in particular should get an actual on-device tap-through before shipping, since intent-resolution behavior (chooser vs. direct launch, fallback triggering) is exactly the kind of thing that only shows itself at runtime.

---

## 2026-08-15 — Metronome info dialog; InfoDialog extracted as a shared component

**Decision.** Added the same "i" info button + explanatory dialog that Key Finder and Chord Finder already had to Metronome, per explicit user request. Since this would have created a *third* byte-for-byte-identical `AlertDialog` implementation (Key Finder's and Chord Finder's `InfoDialog` composables already differed only in which string resources they read), extracted a shared `ui/components/InfoDialog.kt` (`title: String, body: String, onDismiss: () -> Unit`) instead of copy-pasting a third private copy, and migrated Key Finder/Chord Finder onto it too, deleting their private duplicates.

Also consolidated the two identical "Got it" confirm-button strings (`keyfinder_info_dialog_ok`, `cf_info_dialog_ok`) into one shared `common_info_dialog_ok`, mirroring the `common_cd_back` consolidation from the same day's `ScreenHeader` work.

Metronome's info button is wired the same way Key Finder's/Chord Finder's already were: an `Icons.Outlined.Info` `IconButton` (18dp icon, `fg.tertiary` tint) in `ScreenHeader`'s `trailingAction` slot, toggling local `showInfoDialog` state. New copy (`metronome_info_dialog_title` = "About Metronome", `metronome_info_dialog_body`) summarizes BPM entry (typed/slider/±/tap-tempo), time signature + subdivision, and the accented downbeat — mirroring the existing (longer) Help-screen Metronome placeholder text at dialog length rather than duplicating it verbatim.

**Alternatives considered.**
- *Copy-paste a third private `InfoDialog` into `MetronomeContent.kt`*, matching the (already duplicated) status quo exactly. Rejected — the user's own ask was the trigger to notice a second copy was about to become a third; extracting now is strictly cheaper than doing it after a fourth screen needs the same thing.

**Rationale.** Same motivation as every other shared-component extraction this session: the parts that are genuinely identical (dialog chrome, confirm button) collapse into one place; the only per-screen variation (title/body text) stays exactly that specific.

**Known gap.** None — verified via `compileDebugKotlin` (clean) and the full `testDebugUnitTest` suite (unaffected; no unit tests target any of the three migrated/added info-dialog call sites). Not visually re-verified in a running app — same `adb`/emulator limitation noted throughout this session's other entries.

---

## (Template for future entries)

## YYYY-MM-DD — Short title of decision

**Decision.** What was chosen.

**Alternatives considered.** What else was on the table, briefly.

**Rationale.** Why this choice won.

**Supersession trigger.** (Optional) What would cause us to revisit.
