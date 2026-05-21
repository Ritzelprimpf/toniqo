# Phase 6 (Metronome) — Planning Decision Log

This document captures decisions made during the interactive Phase 6 requirements review.
Each entry will later be folded into the formal `Phase6-PLAN.md` / `Phase6-REQUIREMENTS.md`
and the relevant ones appended to `DECISIONS.md`.

---

## Item 1 — Audio source: assets vs. synthesized clicks

**Decision.** Synthesize clicks in code at startup.

**Alternatives considered.**
- **A: Bundle pre-rendered audio assets (WAV/OGG).** Best fidelity, but requires a sound designer or a content pipeline.
- **B: Synthesize in code.** Chosen.
- **C: Hybrid — synthesize at build time via a small generator, commit resulting WAVs as assets.** Runtime simplicity of A with development control of B, but adds a build-time step.

**Rationale.**
- No sound designer is available, so A is not viable.
- Synthesizing in code keeps the project consistent with how the tuner is structured (audio behavior in source, not in opaque binary files).
- Metronome clicks are simple enough that synthesizing them is well-trodden ground (windowed sinusoids with an envelope).
- Extensibility: alternative timbres, pitch shifting, or user-selectable click sounds become small code changes rather than content tasks.

**Implications for the Phase 6 plan.**
- A dedicated component (provisional name `ClickSynthesizer`) lives in `metronome/data/audio/` and exposes something like `fun generate(kind: ClickKind, sampleRateHz: Int): FloatArray`.
- `ClickKind` enum: `ACCENTED`, `STANDARD`, `SUBDIVISION`.
- All sound parameters (frequencies, envelope shape, durations, amplitudes) are named constants — no magic numbers.
- The synthesizer is pure (no Android dependencies, no I/O), fully unit-testable: output length, peak amplitude, no clipping, no DC offset, deterministic for the same inputs.
- Clicks are generated **once at player initialization**, not per beat. Per-beat hot path is free of allocation and CPU work.
- Concrete synthesis parameters (frequencies per click kind, envelope shape, exact durations) will be locked once the full requirements sweep is complete.

---

## Item 2 — Timing / scheduling strategy

**Decision.** Anchor-based drift-corrected `delay()` loop (Strategy B).

**Alternatives considered.**
- **A: Plain `delay(60_000 / bpm)` loop.** Simplest. Rejected — drift accumulates audibly over a session and integer division loses BPM precision.
- **B: Anchor-based drift correction.** Chosen.
- **C: Sample-accurate scheduling via direct `AudioTrack` buffer writes.** Best possible timing, but substantial complexity (effectively a tiny audio mixer); test surface is larger; user-perceived improvement over B is small for typical tempos.

**Rationale.**
- A drifting metronome is worse than no metronome — musicians notice. A is ruled out.
- C is excellent but overkill for v1; can be revisited as a polish phase if B's residual jitter (1–10 ms typical) turns out to be audible in practice.
- B hits the sweet spot: no drift, simple and testable code, jitter within human perception thresholds for "evenly spaced." This is the approach most commercial metronome apps use.

**Implications for the Phase 6 plan.**
- Beat times are computed from a fixed start anchor in nanoseconds:
  `targetNs = startTimeNs + (beatIndex * 60_000_000_000L / bpm)`.
- BPM math done in nanoseconds, not milliseconds, to avoid precision loss.
- A clock abstraction is injected so unit tests can virtualize time (`System.nanoTime()` is not virtualized by `kotlinx-coroutines-test`).
- Tempo changes mid-run: re-anchor on the next beat (no glitch needed; the next beat just becomes the new anchor for the new tempo).
- Time-signature / subdivision changes mid-run: per the existing spec, restart the beat cycle from beat 1 on the next downbeat.
- **Visual/audio sync (deferred but noted):** the UI `currentBeat` emission will precede the audible click by the `AudioTrack` output latency (typically 20–80 ms). Accept the visual leading slightly for v1 (eyes are more tolerant than ears for sync). Revisit only if it looks wrong in practice.

---

## Item 3 — Tempo descriptor BPM ranges

**Decision.** Five labels (as already in `APP_SPECIFICATION.md`), with fixed BPM boundaries and the lowest/highest labels extending to cover the full 1–300 range at the edges.

| Range (BPM) | Label |
|---|---|
| 1 – 75 | Adagio |
| 76 – 107 | Andante |
| 108 – 119 | Moderato |
| 120 – 167 | Allegro |
| 168 – 300 | Presto |

**Alternatives considered.**
- **B: Same ranges as A, with a "(very slow)" / "(very fast)" suffix at the extremes.** Rejected — adds UI complexity for a descriptor that's intentionally minimal and informational.
- **C: Expand to seven labels (add Largo and Prestissimo).** Rejected — contradicts the existing spec, and the descriptor is a glance-at indicator, not a functional control.

**Rationale.**
- Honors the existing five-label decision in `APP_SPECIFICATION.md`.
- Boundaries reflect mid-point consensus from common references; Moderato gets a narrow band matching its real-world meaning (right between Andante and Allegro).
- Extending edge labels to cover the full 1–300 range is what users expect — the BPM number provides the precision; the label provides the feel.

**Implications for the Phase 6 plan.**
- Four named constants for the boundaries: `TEMPO_BOUNDARY_ANDANTE = 76`, `TEMPO_BOUNDARY_MODERATO = 108`, `TEMPO_BOUNDARY_ALLEGRO = 120`, `TEMPO_BOUNDARY_PRESTO = 168`. No magic numbers.
- Lookup is a simple `when` block in a pure function — fully unit-testable.
- Label strings live in `res/values/strings.xml` (no hardcoded UI strings, per project conventions).

---

## Item 4 — Persistence

**Decision.** Persist BPM, time signature (numerator + denominator), and subdivision across app launches via a separate DataStore file. Do not persist `isPlaying` or `currentBeat` — the metronome always launches in the stopped state.

**Alternatives considered.**
- **No persistence.** Rejected — adds friction for a practice tool. Standard expectation in this category of app is that settings survive across sessions.
- **Persist `isPlaying` too.** Rejected — auto-starting audio on launch is startling, consumes audio focus uninvited, and not what users expect.
- **Share the tuner's DataStore file.** Rejected — each module owns its own preferences; no cross-module coupling.

**Rationale.**
- BPM, time signature, and subdivision are session-spanning configuration values. A guitarist working a piece at 92 BPM in 3/4 should find 92 BPM in 3/4 still there next session.
- Reuses the `androidx.datastore:datastore-preferences` dependency already added in Phase 5.3 — no new dependency.
- Mirrors the proven Phase 5.3 pattern for tuner preset persistence.

**Implications for the Phase 6 plan.**
- New DataStore file: `metronome_preferences`, parallel to `tuner_preferences`.
- Interface `MetronomePreferences` in `metronome/data/`:
  - `val config: Flow<MetronomeConfig>`
  - `suspend fun setConfig(config: MetronomeConfig)`
- `MetronomePreferencesImpl` backed by the new DataStore file; bound via Hilt in `MetronomeModule`.
- `FakeMetronomePreferences` for tests (in-memory `MutableStateFlow`-backed), under `app/src/test/java/.../metronome/fakes/`.
- ViewModel reads `MetronomePreferences.config` on init; writes back on every user change to BPM / time signature / subdivision.
- First launch (no persisted config): use the spec defaults (BPM 120, time signature 4/4, subdivision NONE). These get persisted on the first user interaction.
- BPM writes may need debouncing (e.g., during slider drag) — implementation detail, not a requirements-level concern.

---

## Item 5 — Background / lifecycle behaviour

**Decision.** The metronome runs only while the metronome tab is the active, foreground screen. Any lifecycle transition away from that — tab change, app backgrounded, audio focus loss — stops playback. Settings (BPM, time signature, subdivision) are preserved (per Item 4); returning to the tab restores them in the stopped state, and the user explicitly restarts.

| Event | Behavior |
|---|---|
| Tab change within app | Stop. Settings preserved. |
| App backgrounded | Stop. Settings preserved. |
| Audio focus loss (phone call, etc.) | Stop. Settings preserved. |

**Alternatives considered.**
- **Keep running on tab change** (so the metronome accompanies tuning, key finding, etc.). Rejected — having a click run while tuning or searching for keys makes no sense for the actual practice flow.
- **Foreground service to keep running while app is backgrounded.** Rejected — significant scope (Service, notification channel, media session, audio focus integration, Android 14+ foreground service types) for a marginal use case.
- **Auto-resume on audio focus regain.** Rejected — consistent with the "never auto-start audio" principle from Item 4.

**Rationale.**
- In real practice, users typically take a count-in (1–2 bars) when starting the metronome, so requiring an explicit restart after any interruption costs them nothing — they'd do it anyway.
- After tuning, users normally restart the metronome regardless. Auto-continuing would actually be wrong: the player wants a fresh downbeat aligned with their re-entry.
- Collapses all three lifecycle events to one simple rule, which is easy to reason about, easy to test, and impossible to violate accidentally.
- Eliminates the need for a foreground service in v1 entirely.

**Implications for the Phase 6 plan.**
- `MetronomePlayer` lifetime is tied to the metronome **screen's** lifecycle. Same pattern as the tuner mic capture in Phase 5.3: the screen collects a `Flow` from the player with `collectAsStateWithLifecycle`, so the player runs iff the screen is `STARTED`.
- `MetronomeViewModel` is screen-scoped (`hiltViewModel()` inside the Metronome destination), not activity-scoped.
- Audio focus is requested when playback starts (user taps Start) and released when playback stops (any cause: user tap, lifecycle exit, focus loss). An `OnAudioFocusChangeListener` triggers `stop()` on any focus-loss event.
- No `Service` class, no notification channel, no media session integration in Phase 6. These can be added later as their own phase if backgrounded-metronome becomes a requested feature.
- `isPlaying` state is reset to `false` whenever the screen leaves `STARTED`. Settings remain untouched (they're read from DataStore on next entry).

---

## Item 6 — Tap-tempo algorithm

**Decision.** Rolling window over the last 5 taps (i.e., the last 4 intervals between them), simple average, no outlier rejection. BPM is emitted starting from the second tap; the window fills as more taps arrive.

| Aspect | Decision |
|---|---|
| First BPM emission | After tap 2 (1 interval available) |
| Window | Rolling, last 5 taps (last 4 intervals) |
| Averaging | Simple mean of intervals in the window |
| Reset timeout | 2 seconds between taps → start a new session on next tap |
| Outlier rejection | None |
| Range clamping | Clamp computed BPM to [1, 300] |
| Rounding | Round to nearest integer for the committed `MetronomeConfig.bpm` |
| Behaviour while metronome is running | Live update on each tap; player re-anchors to the new tempo |

**Alternatives considered.**
- **Cumulative average over all taps in the session.** Rejected — slow to react; if the user starts wrong and corrects, the cumulative mean clings to the old taps for too long.
- **Outlier rejection** (drop taps implying a BPM too far from current estimate). Rejected — adds complexity, can fight the user when they're deliberately changing tempo, and the rolling window already smooths normal jitter.
- **Wait for 3+ taps before emitting** (more stable first estimate). Rejected — feels unresponsive; the user wonders if it's working.
- **Buffered update or "Apply" button while metronome is running.** Rejected — live update is the universal convention and lets the user hear whether the new tempo is right.

**Rationale.**
- The rolling window organically handles the "ramp-up" phase: a user grooving in over 3–4 taps before settling has those early taps naturally fall out of the window as they keep tapping. No explicit reset needed.
- Same mechanism lets the user change their mind mid-session — keep tapping at a new tempo and the BPM follows after a few taps.
- 5 taps / 4 intervals is enough to average out normal human tap jitter but small enough to react to deliberate changes within a few taps.
- 2-second timeout corresponds to 30 BPM, comfortably below the minimum musical range; any gap longer than that signals an actual pause, not a slow tap.

**Implications for the Phase 6 plan.**
- Named constants (no magic numbers):
  - `TAP_TEMPO_WINDOW_SIZE = 5` (taps; 4 intervals)
  - `TAP_TEMPO_RESET_TIMEOUT_MS = 2000`
  - `TAP_TEMPO_MIN_INTERVALS_FOR_BPM = 1` (i.e., BPM emitted after the 2nd tap)
- The tap-tempo logic is a pure function (or small stateful component) over a `List<Long>` of tap timestamps — no Android dependencies, fully unit-testable.
- Likely shape: `TapTempoCalculator` class with `fun onTap(timestampMs: Long): Int?` returning the new BPM (or null for the first tap of a session).
- Unit tests cover: first-tap returns null; second-tap emits 1-interval BPM; window fills correctly; sliding window discards oldest tap after 5; timeout resets the session; BPM is clamped to [1, 300]; rounding behaviour at boundaries.
- The calculator is owned by the ViewModel. The screen wires the TAP button's `onClick` to `viewModel.onTapTempo()`, which calls the calculator with `System.currentTimeMillis()` (or an injected clock for testability).
- While the metronome is running, each new BPM from the calculator goes through the same `MetronomeConfig` update path as any other BPM change — the player re-anchors per the Item 2 rule.

---

## Item 7 — Time signature denominators and "beat unit"

**Decision.** All time signatures are treated as simple meters. The denominator literally dictates the beat unit: /4 signatures click on quarters, /8 signatures click on eighths. The numerator dictates the number of main beats per bar (and therefore the number of segments in the beat indicator). Beat 1 is accented; all other main beats are standard clicks.

| Signature | Beat indicator segments | Click pattern (no subdivision) |
|---|---|---|
| 2/4 | 2 | ACCENT, standard |
| 3/4 | 3 | ACCENT, standard × 2 |
| 4/4 | 4 | ACCENT, standard × 3 |
| 5/4 | 5 | ACCENT, standard × 4 |
| 6/8 | 6 | ACCENT, standard × 5 |
| 7/8 | 7 | ACCENT, standard × 6 |
| 9/8 | 9 | ACCENT, standard × 8 |
| 12/8 | 12 | ACCENT, standard × 11 |

**BPM interpretation.** BPM refers to the beat unit indicated by the denominator: in /4 signatures BPM = quarter notes per minute; in /8 signatures BPM = eighth notes per minute.

**Alternatives considered.**
- **Approach A: Treat /8 signatures as compound, beat = dotted quarter.** 6/8 = 2 beats per bar, 9/8 = 3, 12/8 = 4. Rejected — creates a tangled interaction with the subdivision parameter (compound feel already contains internal subdivisions), forces special-case logic, and 7/8 doesn't fit the model at all.
- **Configurable "feel" toggle per signature.** Rejected — adds UI complexity and a third concept the user has to learn. Can be added later if needed.

**Rationale.**
- Keeps subdivision orthogonal to time signature: in any signature, "eighths" means clicks at twice the beat rate, "sixteenths" four times, etc. No double meaning.
- Predictable: numerator → segment count. No mode-switching based on tempo.
- Treats 7/8 the same as every other signature (no special case).
- The compound feel (e.g., 6/8 felt in 2) remains recoverable later via per-beat accent customization (item 11, still to come): user accents beats 1 and 4 → felt-in-2 click pattern.
- Matches the behaviour of most simple metronome apps; "feel" toggles are advanced features that can be layered in later without breaking this foundation.

**Implications for the Phase 6 plan.**
- `MetronomeConfig.timeSignatureDenominator` carries semantic meaning: it determines the beat unit and therefore the BPM interpretation. The player uses it together with BPM to compute the per-beat interval (in /8, the interval is `60_000_000_000L / bpm`; in /4, same formula — BPM math is uniform regardless of denominator, because BPM already refers to the beat unit).
- Beat indicator segment count = `MetronomeConfig.timeSignatureNumerator`. The composable renders that many segments.
- Beat index 0 is always the accented beat; beat indices 1..(N-1) are standard. This is hardcoded for Phase 6; per-beat accent customization is deferred to a future phase (see item 11 when we get to it).

---

## Item 8 — Subdivision behaviour

**Decision.** Subdivision is a uniform multiplier on the beat rate, regardless of time signature. All "between-beat" ticks use the SUBDIVISION click kind. Main beats always win at collisions — subdivision clicks only fill gaps between main beats.

| Subdivision | Multiplier | Click rate |
|---|---|---|
| NONE | 1 | = beat rate |
| EIGHTHS | 2 | 2 × beat rate |
| SIXTEENTHS | 4 | 4 × beat rate |
| TRIPLETS | 3 | 3 × beat rate |

**Click kind selection per click index within a bar:**
- `ACCENTED` if click index == 0 (the downbeat)
- `STANDARD` if click index is a non-zero multiple of the subdivision multiplier (a main beat that isn't beat 1)
- `SUBDIVISION` otherwise (a between-beats tick)

**Edge case: EIGHTHS in /8 signatures.** Mathematically a no-op — the beat already is an eighth, so "eighth subdivision" adds no clicks. Left as-is in the UI (option visible and selectable, just produces the same output as NONE). No UI special-casing.

**Alternatives considered (for the no-op edge case).**
- **Hide EIGHTHS when denominator is 8.** Rejected — dropdowns that change shape based on other selections are surprising and accessibility-unfriendly.
- **Disable / grey out EIGHTHS when denominator is 8.** Rejected — same concern; also forces an explanation for why a normally-valid option is greyed.

**Rationale.**
- Subdivision orthogonal to time signature (per Item 7): one set of rules covers every signature.
- The collision rule ("main beat wins") matches the spec already in `APP_SPECIFICATION.md`: "The first beat of each measure still gets the accented click regardless of subdivision."
- Allowing the EIGHTHS-in-/8 no-op is mathematically consistent — there's nothing to add — and the user discovers this by trying it. No UI machinery needed.

**Implications for the Phase 6 plan.**
- Two pure helper functions in the player (or a small companion object), unit-testable in isolation:
  - `clicksPerBar(numerator: Int, subdivision: Subdivision): Int` → `numerator * subdivision.multiplier`
  - `clickKindFor(clickIndexInBar: Int, subdivision: Subdivision): ClickKind` → applies the rule above
- The scheduler (per Item 2) computes:
  - Click interval (ns) = `(60_000_000_000L / bpm) / subdivision.multiplier`
  - Bar length = `clicksPerBar(numerator, subdivision)` clicks; click index resets to 0 at the start of each bar
- `Subdivision` enum gains a `multiplier: Int` property (NONE=1, EIGHTHS=2, SIXTEENTHS=4, TRIPLETS=3) so the math reads naturally. Named, not inline.
- Unit tests cover every (signature, subdivision) combination listed in this decision, plus the EIGHTHS-in-/8 no-op identity.

---

## Item 9 — BPM input UX detail

**Decision.** Three BPM input methods, each with one clear job. Tap tempo (the fourth method) is fully covered by Item 6.

| Method | Behavior | Job |
|---|---|---|
| `+` / `−` buttons | Flat ±1 per tap. No press-and-hold acceleration. | Precision |
| Slider | Linear scale across the full 1–300 range. | Fast approximation |
| Tap-to-type | Dialog with number-pad input. OK enabled only when input ∈ [1, 300]. Commit on OK / done; cancel discards. No inline error messages. | Exact entry |

**Alternatives considered.**
- **+/− with press-and-hold acceleration** (tap = ±1, hold = continuous increment that speeds up). Rejected — the +/− buttons are for precision; users who want to traverse the range quickly use the slider. Each input method having a single, clear job is cleaner than overlapping them.
- **Logarithmic slider scale** (more resolution at low BPMs). Rejected — clever but unpredictable; linear position-to-BPM mapping is what users expect.
- **Linear slider across a practical range** (e.g., 40–240). Rejected — feels arbitrary, and some users do want 30 or 280 BPM.
- **Inline editable BPM number** instead of a dialog. Rejected — the BPM number is displayed at 96dp; editing it in-place is awkward on a phone. Dialog is cleaner.

**Rationale.**
- Three input methods, three distinct jobs: precision (+/−), approximation (slider), exact (text). No overlap, no surprise.
- Flat ±1 is the universally expected behavior for a +/− button. Press-and-hold adds complexity (acceleration curve, named constants, additional test cases) for a use case (fast traversal) that the slider already serves better.
- Linear slider gives 1 BPM per pixel at typical phone widths — the resolution the user expects when they nudge the thumb.
- Dialog-based exact entry isolates the editing action from the running display and avoids the awkward 96dp-text inline editing problem.

**Implications for the Phase 6 plan.**
- `+` / `−` buttons: simple `onClick` handlers calling `viewModel.onBpmIncrement()` / `viewModel.onBpmDecrement()`. Both clamp the result to [1, 300]. No long-press logic needed.
- Slider: `androidx.compose.material3.Slider` with `valueRange = 1f..300f`, `steps = 298` (so the slider snaps to integer BPMs). On drag end (or per-change with debouncing), commit to `MetronomeConfig`.
- Tap-to-type:
  - Tapping the BPM display area opens a `BpmInputDialog` composable.
  - The dialog has a single `TextField` with `keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)`.
  - OK button is enabled only when `input.toIntOrNull()?.let { it in 1..300 } == true`.
  - OK or keyboard "Done" action commits via `viewModel.onBpmSet(value)`; Cancel or back-press dismisses without change.
- All BPM commit paths go through a single ViewModel method `onBpmChanged(newBpm: Int)` that clamps to [1, 300] defensively and writes to `MetronomeConfig`. Per Item 4, this also persists.
- Named constants for the BPM range (already justified): `BPM_MIN = 1`, `BPM_MAX = 300`, `BPM_DEFAULT = 120`.

---

## Item 10 — Haptics

**Decision.** No haptics in Phase 6. The metronome uses audio only.

**Alternatives considered.**
- **Always-on haptic per main beat** (in addition to audio). Rejected for v1 — introduces a "metronome that vibrates" assumption that needs a toggle to be acceptable, and would surprise users with phones in pockets.
- **User-configurable haptic mode** (Off / Beats only / Beats + subdivisions). Rejected for v1 — requires a metronome settings surface that doesn't exist yet in `DESIGN.md` §8.2.
- **Haptics-only "silent mode"** (audio muted, haptic replaces it). Rejected for v1 — Android haptic timing precision (~10–30 ms actuator response) is not reliably good enough to serve as a primary timing reference, especially at fast tempos with subdivisions.

**Rationale.**
- The tuner's single success-pulse haptic is a one-off event; a metronome haptic would be a continuous stream — a substantially different scope and reliability profile.
- Android haptic precision is marginal for a primary timing role; promising it and shipping something flaky is worse than not shipping it.
- Adding haptics later is non-breaking: a "Haptics" setting can be appended to a future metronome settings panel without changing any existing behaviour.

**Future consideration.** "Phone in pocket while practicing" is a real use case worth revisiting — most likely as Option B (always-on beats-only haptic) with a single user toggle, once a metronome settings surface exists in `DESIGN.md`.

**Implications for the Phase 6 plan.**
- No haptic-related code in `MetronomePlayer` or the ViewModel.
- No haptic-related UI elements in the metronome screen.
- No `Vibrator` / `VibratorManager` dependencies in the metronome module.

---

## Item 11 — Accent customization

**Decision.** No accent customization in Phase 6. Beat 1 of every bar is accented; all other main beats are standard clicks. Fixed, not user-configurable.

**Alternatives considered.**
- **Level 1: Binary per-beat accent toggle** (user taps a beat segment to mark it as accented or standard). Rejected for v1.
- **Level 2: Three-state per-beat** (silent / standard / accent), allowing muted beats. Rejected for v1.

**Rationale.**
- Largest scope item still on the table: extends `MetronomeConfig` with a per-beat pattern, requires the beat indicator to become interactive (the current `DESIGN.md` §8.2 treats it as display-only), adds reset affordances, and substantially grows the test surface.
- The default behavior (accent on beat 1) covers the great majority of practice scenarios.
- Known trade-off from Item 7: 6/8 users wanting a felt-in-2 click pattern have to live with clicks on every eighth until accent customization arrives. This is a documented limitation, not a hidden one.

**Future consideration.** If accent customization is added later, the recommended path is:
- Start with **Level 1 (binary)** — covers the felt-in-2 and 7/8-grouping (2+2+3, 3+2+2, 2+3+2) cases, which are the actual asks.
- **Level 2 (silent state)** can come later still.
- `MetronomeConfig` would gain an optional `accentPattern: List<AccentLevel>?` field. The default (beat 1 accented) is the fallback when no pattern is set, so existing persisted configs from v1 continue to work unchanged.

**Implications for the Phase 6 plan.**
- `MetronomeConfig` carries no accent-pattern field in v1.
- The beat-indicator composable is purely a display element — no tap handlers, no toggle visuals, no accent-state rendering beyond the existing "beat 1 vs. others" distinction.
- The `clickKindFor(...)` pure function from Item 8 keeps its current form (beat index 0 → ACCENTED; non-zero multiple of subdivision multiplier → STANDARD; otherwise → SUBDIVISION).

---

## Item 12 — Volume control

**Decision.** No in-app volume control. The metronome rides on the system media volume.

**Alternatives considered.**
- **In-app master volume slider** (scaled all clicks uniformly, persisted with config). Rejected — `DESIGN.md` §8.2 has no provision for a volume control; adding one means redesigning the layout.
- **In-app main vs. subdivision balance control.** Rejected — relative balance between click kinds is fixed in the synthesizer (Item 1); if the amplitudes are right, users won't need to adjust.

**Rationale.**
- `AudioTrack` with the music stream automatically respects system media volume — no extra code needed.
- Consistent with the tuner's approach (no in-app mic gain control; system handles input gain).
- The relative balance between accented / standard / subdivision clicks is a synthesizer design decision (Item 1's named constants), not a runtime user setting.
- A user who needs the metronome quieter than other media must lower system media volume globally — a known but small limitation, addressable in a polish phase if it becomes a common ask.

**Implications for the Phase 6 plan.**
- `AudioTrack` is constructed with the music usage / content type:
  - `AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)` — `CONTENT_TYPE_SONIFICATION` is the correct content type for non-musical sounds like clicks.
- No volume slider in `MetronomeScreen`.
- No volume-related fields in `MetronomeConfig` or `MetronomePreferences`.

---

## Item 13 — Count-in behaviour

**Decision.** No built-in count-in. Tapping Start triggers beat 1 immediately. Subsequent beats follow at the configured tempo. The user provides their own count-in by waiting however many bars they want before playing.

**Alternatives considered.**
- **Built-in configurable count-in** (Off / 1 bar / 2 bars). Rejected.

**Rationale.**
- A count-in is just the metronome being a metronome — there's no functional difference between "count-in beats" and "regular beats" except in the user's head. That distinction belongs to the user, not to the player.
- Symmetric with how all other tempo / config changes work in the running player ("changes apply on the next beat" — no buffering, no special modes).
- A built-in count-in would require: an extra `MetronomeConfig` field, a UI control, a persisted value, a way to visually distinguish count-in beats from real beats, and edge-case rules for tempo changes mid-count-in. All to deliver something the user can already do by waiting before they play.
- Forward-compatible: if a future need arises, a `countInBars: Int` field defaulting to 0 can be added without changing existing behaviour.

**Implications for the Phase 6 plan.**
- The player has a single "running / stopped" state. No "counting in" state.
- The beat indicator has no special visual treatment for early beats — every beat is rendered the same way per Item 7.
- `MetronomeConfig` has no count-in field.

---

## Item 14 — Screen-on behaviour

**Decision.** Keep the screen on while playback is running. Drop the flag when playback stops, for any reason (user tap, lifecycle exit, audio focus loss).

**Alternatives considered.**
- **Always on while on the metronome screen, regardless of playback state.** Rejected — burns battery if a user has the screen on the metronome tab but isn't actually playing.
- **No screen-on management.** Rejected — having the screen sleep mid-bar is a real annoyance for the metronome's primary use case (glance, look away, look back).

**Rationale.**
- The metronome is the canonical "glance-at-then-look-away-then-look-back" tool. Screen sleep mid-practice defeats the visual half of the metronome.
- The flag's lifetime tied 1:1 to playback state is the simplest correct rule.
- Battery cost is negligible during playback (audio is already keeping the CPU active); zero after stop (flag dropped).

**Implications for the Phase 6 plan.**
- The metronome screen acquires `FLAG_KEEP_SCREEN_ON` on the window when `isPlaying` becomes `true`, releases it when `isPlaying` becomes `false`.
- Likely implementation: a `DisposableEffect` keyed on `isPlaying` in the `MetronomeScreen` composable. On entry (true), `window.addFlags(FLAG_KEEP_SCREEN_ON)`; on dispose or key change to false, `window.clearFlags(FLAG_KEEP_SCREEN_ON)`.
- Per Item 5, leaving the screen (tab change, backgrounding, focus loss) sets `isPlaying = false` and the disposal logic clears the flag automatically. Single rule, multiple lifecycle paths converge correctly.

---

## Item 15 — Sample rate and audio format

**Decision.** 48000 Hz, single-channel mono, 16-bit PCM. `ClickSynthesizer` generates `ShortArray` buffers at this format.

**Alternatives considered.**
- **44.1 kHz.** Rejected — Android device audio hardware is essentially always native 48 kHz; using 44.1 kHz forces OS-level resampling, which adds latency. We want low latency (Items 1, 2).
- **Stereo output.** Rejected — a click is a point-source sound; stereo doubles buffer size for no audible benefit.
- **`FloatArray` synthesis** (as initially noted in Item 1). Revised to `ShortArray` — slightly more efficient, matches what `AudioTrack` accepts for the lowest-latency `ENCODING_PCM_16BIT` configuration.
- **Query `AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE` at runtime** for the device's preferred rate. Rejected as unnecessary — 48 kHz is universal on supported Android versions; querying adds complexity without benefit.

**Rationale.**
- 48 kHz is the native rate of essentially every modern Android device's audio path. Matching it avoids OS resampling and gives the lowest-latency path.
- 16-bit PCM provides ~96 dB of dynamic range — vastly more than needed for a click.
- Mono is correct for the sound type; stereo would be wasteful.

**Implications for the Phase 6 plan.**
- Named constants in a single audio-config object (e.g., `MetronomeAudioFormat`):
  - `SAMPLE_RATE_HZ = 48000`
  - `CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO`
  - `ENCODING = AudioFormat.ENCODING_PCM_16BIT`
- `ClickSynthesizer.generate(kind: ClickKind): ShortArray` — sample rate is implicit from the audio-config object, not a parameter.
- Buffers are passed directly to `AudioTrack.write(ShortArray, ...)`. No resampling, no conversion.
- This supersedes the `FloatArray` mention in Item 1's "Implications" section.

---

## Item 16 — Concurrent playback with the tuner

**Decision.** No cross-module coordination needed. The lifecycle rules already enforce mutual exclusion. Document this explicitly in `Phase6-PLAN.md`.

**Situation.**
- Tuner uses `AudioRecord` (mic input); metronome uses `AudioTrack` (audio output).
- These are independent audio paths — the OS does not prevent them from coexisting.
- Per Item 5 and the existing tuner design, each module is bound to its own screen's lifecycle and runs only while its tab is active. Since only one tab is active at a time, at most one of {tuner, metronome} is running at any moment.

**Corner case considered.** During a tab transition, there is a brief lifecycle handoff. The metronome's `STARTED` → `STOPPED` transition releases `AudioTrack` and audio focus before the tuner's lifecycle entry triggers mic capture. Even with millisecond overlap, `AudioRecord` and `AudioTrack` are independent streams and would not conflict.

**Rationale.**
- Lifecycle-driven mutual exclusion is already in place — adding explicit coordination would be redundant machinery.
- Worth documenting *why* there's no coordination, so future readers of the plan don't add one unnecessarily.

**Implications for the Phase 6 plan.**
- A short paragraph in `Phase6-PLAN.md`:
  > The metronome and tuner can never be running simultaneously: each is bound to its own screen's lifecycle, and only one screen is active at a time. No cross-module coordination is required. The metronome uses `AudioTrack` (output) while the tuner uses `AudioRecord` (input), so even hypothetically simultaneous operation would not produce audio conflicts.
- No code changes, no shared services, no audio-focus negotiation between modules.

---

## Item 17 — Validation of persisted config on load

**Decision.** Option A — whole-config replacement with defaults. If *any* field in the persisted config is invalid, replace the entire config with the spec defaults (BPM 120, 4/4, NONE) and write the repaired config back to DataStore.

**Validation rules (per field):**
- BPM must be in [1, 300].
- Time signature must be one of the eight explicitly listed: 2/4, 3/4, 4/4, 5/4, 6/8, 7/8, 9/8, 12/8. Validation is on the combination, not on numerator and denominator independently.
- Subdivision must decode to one of `NONE / EIGHTHS / SIXTEENTHS / TRIPLETS`.
- Missing keys (first launch) → use defaults; not an "invalid" case but the same code path produces the right result.

**Alternatives considered.**
- **Per-field validation with default fallback** (preserve valid fields, replace only invalid ones). Rejected — the user cost of reconfiguring three parameters is trivial, and partial repair trusts fields that might be valid by coincidence in a generally-corrupt file.
- **Crash / surface error.** Rejected — a practice tool that refuses to open because of a stale preferences file is worse than one that quietly recovers. Tuner doesn't do this; metronome shouldn't either.

**Rationale.**
- Three fields is small enough that wholesale replacement isn't a meaningful user inconvenience.
- An invalid field is a strong signal that *something is wrong* with the persisted file (corruption, schema mismatch, tampering). Resetting to a known-good baseline is the cleaner failure mode.
- Simpler validation code, simpler tests.

**Implications for the Phase 6 plan.**
- A pure validation function lives near `MetronomePreferencesImpl`:
  - `fun validateOrDefault(rawConfig: RawMetronomeConfig): MetronomeConfig`
  - Returns the parsed `MetronomeConfig` if all fields valid, else `MetronomeConfig.DEFAULT`.
- After validation, if the result differs from the raw input, write back to DataStore so subsequent reads are clean (self-healing).
- Unit tests cover: each individual invalid field triggers full default; first-launch (missing keys) returns default without write-back churn; valid persisted config passes through untouched.
- `MetronomeConfig.DEFAULT` is a single named constant: `MetronomeConfig(bpm = 120, timeSignatureNumerator = 4, timeSignatureDenominator = 4, subdivision = Subdivision.NONE)`.

---

## Item 18 — Start/Stop button label

**Decision.** Icon-only button. ▶ (play) icon in the stopped state, ⏸ (pause) icon in the running state. No text label on the button itself.

**Alternatives considered.**
- **Text-only** ("START" / "STOP" in mono kicker). Rejected — icon-only is more visually minimal and fits the app's design language; play/pause icons are universally recognized.
- **Icon + text combo.** Rejected — adds visual weight the button doesn't need.
- **Stop icon (⏹) instead of pause icon (⏸)** for semantic accuracy. Not adopted — ⏸ is conventional for "tap this to stop the audio," and the `DESIGN.md` §7 icon set already includes `play` and `pause`. The semantic note (the metronome resets to beat 1 on next start; it does not resume from where it stopped) is documented in the plan, not encoded in the icon.

**Rationale.**
- Play / pause icons are universally recognized — no text needed for clarity.
- Visual minimalism fits the rest of the metronome layout (BPM display, beat indicator, dropdowns).
- Uses icons already present in the `DESIGN.md` §7 set; no new asset required.

**Implications for the Phase 6 plan.**
- Start/Stop pill button per `DESIGN.md` §8.2 dimensions (60dp tall, flex 1 width).
  - Stopped state: `play` icon, mint primary background with glow.
  - Running state: `pause` icon, `bg.elev3` neutral background, no glow.
- Content description for accessibility (TalkBack): `R.string.metronome_start` / `R.string.metronome_stop` — these strings exist in `res/values/strings.xml` even though they're not rendered visually, so screen readers announce the action.
- A note in `Phase6-PLAN.md`: the metronome has no pause-and-resume; stopping resets the next start to beat 1. The pause icon is used by convention, not because of pause semantics.

---

## Item 19 — Error states

**Decision.** A single transient error path via snackbar for startup failures. No dedicated error screen. The player simply remains in the stopped state when a start attempt fails; the user can retry by tapping Start again.

| Failure | Behaviour |
|---|---|
| `AudioTrack` initialization fails | Snackbar: "Audio playback unavailable. Please try again." Player remains stopped. |
| Audio focus denied at start | Same: snackbar; player remains stopped. |
| Audio focus loss during playback | Silent stop (per Item 5). No error UI — the user is in another audio context (phone call, etc.) and doesn't need a notification from us. |
| First launch (no persisted config) | Silent fall-through to defaults (per Items 4 and 17). Not an error. |

**Alternatives considered.**
- **Silent failure.** Rejected — user has no idea why nothing is happening.
- **Dedicated full-screen error state** (like the tuner's `PERMISSION_DENIED` UI). Rejected — overkill for failures this rare; the metronome doesn't have "blocked until X" semantics like the tuner's permission flow.

**Rationale.**
- `AudioTrack` failure is genuinely rare on real devices (no runtime permission required, no environmental input). Building a dedicated error screen for it is disproportionate.
- The metronome has built-in retry via normal interaction — tapping Start again retries `AudioTrack` initialization.
- A snackbar matches Material 3 conventions for transient errors. The rest of the screen stays usable (settings preserved, user can adjust BPM, etc.).
- Audio focus loss during playback is a normal lifecycle event, not an error — no UI noise needed.

**Implications for the Phase 6 plan.**
- `MetronomeUiState` gains a transient error field. Likely shape: a one-shot event channel (e.g., `Flow<UiEvent>` with `UiEvent.AudioUnavailable`) so the snackbar is only shown once and doesn't redisplay on recomposition. Alternative: nullable `errorMessage: String?` cleared by the screen after display.
- When the player's start attempt fails:
  - The ViewModel ensures `isPlaying = false`.
  - It emits the error event.
  - It releases any partially acquired resources (audio focus, `AudioTrack`).
- The screen owns a `SnackbarHost` and shows the message when the event fires.
- String resource `metronome_error_audio_unavailable` in `res/values/strings.xml`.
- Unit tests cover: failed start → `isPlaying` stays false, error event emitted, resources released. Successful retry after failure → normal start path.

---

## Item 20 — Sub-milestone split for Phase 6

**Decision.** Four sub-phases, mirroring the proven Phase 5 pattern. Each sub-phase produces testable, reviewable code with clear acceptance criteria. Risk is front-loaded: the audio foundation is built and validated before any UI work begins.

### Phase 6.1 — Synthesizer & audio format foundation

Pure code, no Android audio playback yet. JVM-testable.

**Deliverables.**
- `ClickSynthesizer` with the parameters locked in Item 21.
- `ClickKind` enum (`ACCENTED`, `STANDARD`, `SUBDIVISION`).
- `MetronomeAudioFormat` config object with the constants from Item 15.
- Pure helper functions: `clicksPerBar(numerator, subdivision)`, `clickKindFor(clickIndexInBar, subdivision)`, tempo-descriptor lookup function.
- Unit tests: synthesis (length, peak amplitude, no clipping, no DC offset, deterministic), helpers (every (signature, subdivision) combination from Item 8 including the EIGHTHS-in-/8 no-op identity), tempo descriptor (every boundary, edge cases at BPM 1 and 300).

**Acceptance criteria.**
- All synthesizer / helper unit tests green.
- No Android-runtime dependencies beyond `AudioFormat` constants.

### Phase 6.2 — Player, scheduler, lifecycle, persistence

The audio engine and supporting infrastructure. Actual sound is produced for the first time.

**Deliverables.**
- `AudioTrackMetronomePlayer` implementing `MetronomePlayer`: owns `AudioTrack`, runs the anchor-based scheduler from Item 2, emits `currentBeat: Flow<Int>`.
- Audio focus request / abandon logic per Item 5 (request on start; abandon-and-stop on any focus loss event).
- `AudioTrack` init failure path: return a failure result, do not start; resources cleaned up.
- `TapTempoCalculator` (pure logic per Item 6).
- `MetronomePreferences` interface; `MetronomePreferencesImpl` (DataStore-backed); `FakeMetronomePreferences` for tests.
- `validateOrDefault()` function per Item 17, with self-healing write-back.
- Hilt module bindings (`MetronomeModule`).
- Unit tests with an injected clock virtualizing `System.nanoTime()`; tempo changes mid-run re-anchor correctly; signature/subdivision changes restart from beat 1; tap tempo behaviour across all cases from Item 6.

**Acceptance criteria.**
- All player / scheduler / preferences / tap-tempo tests green.
- Manual smoke test (via debug harness if needed): "does it actually sound like a metronome?" Audio at typical tempos sounds clean and on-beat.

### Phase 6.3 — ViewModel & state management

The orchestration layer between player and UI. No Android UI yet.

**Deliverables.**
- `MetronomeViewModel` exposing `StateFlow<MetronomeUiState>`.
- `MetronomeUiState` finalized: `isPlaying`, `config`, `currentBeat`, transient error event channel.
- `StartMetronomeUseCase`, `StopMetronomeUseCase` fleshed out from Phase 2 stubs.
- Persistence wiring: read `MetronomePreferences.config` on init; write back on every config change (BPM, time sig, subdivision).
- Tap-tempo wiring: tap events flow into the calculator; resulting BPM flows into the config update path.
- Error event wiring: player failures emerge as one-shot UI events.
- BPM-related ViewModel methods: `onBpmChanged`, `onBpmIncrement`, `onBpmDecrement` (all clamp to [1, 300]).
- Unit tests using `FakeMetronomePreferences` and a fake `MetronomePlayer`.

**Acceptance criteria.**
- All ViewModel tests green.
- End-to-end ViewModel exercise (start → adjust → stop → restart with persisted config) works via test harness.

### Phase 6.4 — UI

The metronome screen, per `DESIGN.md` §8.2.

**Deliverables.**
- `MetronomeScreen` composable: layout per design spec.
- Beat indicator composable: mint glow on beat 1, mint-at-35% on other lit beats, 80ms linear transitions (overrides reduced-motion per design).
- BPM display (96dp mono, fixed size — does not scale with font-size setting).
- Tempo descriptor label (Item 3).
- BPM slider (linear, full 1–300 range, snaps to integers — Item 9).
- +/− buttons (flat ±1 per tap, Item 9).
- Tap-tempo button (60dp circle, `tap` icon + uppercase "TAP" mono kicker — Item 6).
- Time signature & subdivision pill dropdowns.
- Start/Stop pill button: ▶ icon (stopped, mint primary with glow) / ⏸ icon (running, `bg.elev3` neutral) per Item 18. Content descriptions for accessibility.
- BPM tap-to-type dialog (Item 9): number-pad input, OK disabled until value ∈ [1, 300], commit on OK/done, cancel discards.
- `SnackbarHost` for error events (Item 19).
- `DisposableEffect` for `FLAG_KEEP_SCREEN_ON` keyed on `isPlaying` (Item 14).
- Compose UI tests covering all interactions and state transitions.
- Manual QA pass against `DESIGN.md` §8.2.

**Acceptance criteria.**
- All Compose UI tests green.
- Manual QA: visuals match design; interactions feel right; metronome works end-to-end on a real device.

### Why this split

- **Each sub-phase is independently reviewable.** No big-bang integration.
- **Risk front-loaded.** The two highest-risk pieces (audio synthesis quality, scheduling accuracy) ship in 6.1 and 6.2. By the time UI work begins in 6.4, the audio foundation is proven.
- **JVM-testable layers (6.1, 6.3) have fast TDD loops.** Compose UI tests (6.4) are slower and concentrated in one sub-phase.
- **6.2 is the first sub-phase where actual audio is heard.** Natural checkpoint for "does it sound like a metronome?" before investing in UI polish.

### Estimated relative effort

| Sub-phase | Approximate share |
|---|---|
| 6.1 — Synthesizer & format | 20% |
| 6.2 — Player & persistence | 35% |
| 6.3 — ViewModel & state | 20% |
| 6.4 — UI | 25% |

6.2 is the heaviest because it touches the most real Android infrastructure: `AudioTrack`, audio focus, lifecycle, DataStore.

---

## Item 21 — Concrete `ClickSynthesizer` parameters

**Decision.** Lock the following parameters as the **v1 starting values**. They are explicitly tunable: if any click sounds wrong once heard in Phase 6.2, the named constants get adjusted and re-tested. Adjustments after listening do not require revisiting this decision — they're an expected part of 6.2's smoke testing.

### Universal click parameters

- **Waveform:** sine.
- **Total duration:** 30 ms (1440 samples at 48 kHz). Comfortably under the worst-case inter-click interval (~50 ms at 300 BPM with sixteenths).
- **Attack:** 1 ms linear ramp from 0 to peak — avoids the click-pop while staying percussive.
- **Decay:** exponential to silence over the remaining 29 ms. Decay rate ≈ 160 (so the envelope reaches ~1% of peak by the end of the click).

### Per-kind parameters

| Parameter | ACCENTED | STANDARD | SUBDIVISION |
|---|---|---|---|
| Carrier frequency | 1500 Hz | 1000 Hz | 800 Hz |
| Peak amplitude (of full-scale) | 0.70 | 0.50 | 0.25 |
| Duration / envelope | per universal parameters above |  |  |

**Frequency rationale.** Spaced enough to be audibly distinct without sounding random. ACCENTED is a perfect-fifth above STANDARD (3:2 = 1500:1000); SUBDIVISION sits a minor-third-ish below STANDARD. Listeners just perceive "high / middle / low."

**Amplitude rationale.** ~3 dB drop from ACCENTED to STANDARD, ~6 dB further drop from STANDARD to SUBDIVISION. Clear hierarchy without being jarring. ACCENTED's 0.70 peak leaves ~3 dB of headroom against full-scale clipping.

### Synthesis formula

For sample index `i ∈ [0, totalSamples)`:

```
envelope =
  if i < attackSamples:
    i / attackSamples                                     (linear attack)
  else:
    exp(-decayRate * (i - attackSamples) / sampleRate)    (exponential decay)

sample[i] = (peak * envelope * sin(2π * f * i / sampleRate) * 32767).toShort()
```

### Named constants

All parameters live in a `ClickParameters` companion object (or top-level constants):

```kotlin
const val CLICK_DURATION_MS = 30
const val CLICK_ATTACK_MS = 1
const val CLICK_DECAY_RATE = 160.0

const val FREQUENCY_HZ_ACCENTED = 1500.0
const val FREQUENCY_HZ_STANDARD = 1000.0
const val FREQUENCY_HZ_SUBDIVISION = 800.0

const val AMPLITUDE_ACCENTED = 0.70
const val AMPLITUDE_STANDARD = 0.50
const val AMPLITUDE_SUBDIVISION = 0.25
```

**Alternatives considered.**
- **Other waveforms** (square, triangle, filtered noise) for a more "woodblock" or "claves" feel. Deferred — sine is the cleanest starting point; alternative timbres can be added later as a "click sound" setting per Item 1's extensibility note.
- **Longer durations** (e.g., 50 ms) for a fuller click. Rejected — overlap risk at fast subdivisions, and the existing 30 ms is already plenty long for a tick.
- **Decay-only envelopes** (no attack ramp). Rejected — a hard onset causes a click-pop artifact distinct from the click itself. 1 ms attack eliminates it without softening the attack perceptually.

**Rationale.**
- Sine + fast envelope is the standard "clean digital tick" — exactly what commercial metronome apps converge on.
- 30 ms duration is short enough to avoid overlap at any supported tempo/subdivision combination, long enough to sound like a tick rather than a transient artifact.
- The amplitude / frequency hierarchy gives a clear perceptual ranking (accented > standard > subdivision) that matches what the spec calls for.

**Implications for the Phase 6 plan.**
- All numeric parameters become named constants in Phase 6.1's `ClickSynthesizer` module. No magic numbers anywhere in the synthesis code.
- Unit tests verify: output length matches `(CLICK_DURATION_MS * SAMPLE_RATE_HZ) / 1000`, peak sample amplitude stays within ±32767 (no clipping), buffer starts and ends near zero (no clicks-on-clicks), output is deterministic for the same inputs.
- Phase 6.2 includes a manual listening smoke test: run the metronome at a few representative tempos (60 / 120 / 200 BPM) with each subdivision, listen for clarity and balance. Adjust constants if needed; record any adjustments in `DECISIONS.md`.

---

## Item 22 — Spec addenda

**Decision.** The following decisions become user-facing or architecturally durable enough to require updates to the canonical project documents, beyond the decision log itself.

### Updates to `APP_SPECIFICATION.md`

Add the following user-facing behaviour points:

1. **(from Item 3)** The five tempo descriptor labels and their exact BPM boundaries (the table from Item 3).
2. **(from Item 4)** "BPM, time signature, and subdivision are persisted across app launches. The metronome always launches in the stopped state regardless of prior playback state."
3. **(from Item 5)** "The metronome stops playback whenever the metronome screen is not the active foreground screen — tab change, app backgrounded, or audio focus loss. Settings are preserved; the user explicitly restarts."
4. **(from Item 6)** "Tap the TAP button repeatedly to set the BPM. BPM is computed from the average of intervals over the last 5 taps. After 2 seconds of no tapping, the next tap starts a new sequence."
5. **(from Item 7)** "In all time signatures, the numerator determines the number of main beats per bar. In /4 signatures BPM refers to the quarter note; in /8 signatures BPM refers to the eighth note."
6. **(from Item 8)** "Subdivision multipliers: NONE = 1×, EIGHTHS = 2×, SIXTEENTHS = 4×, TRIPLETS = 3× the main beat rate. Main beats always take precedence at collision points; subdivision clicks only fill the gaps. In /8 signatures, EIGHTHS subdivision is equivalent to NONE."
7. **(from Item 14)** "While the metronome is playing, the screen stays on. When playback stops, the screen returns to the normal system display timeout."

### Update to `DESIGN.md` §8.2

8. **(from Item 18)** "Start/Stop pill button content: `play` icon (stopped state), `pause` icon (running state). No text label. Content descriptions provided for accessibility."

### Entries for `DECISIONS.md`

These are architecturally significant or "I want to remember why" decisions that warrant top-level visibility:

- **Item 1** — Synthesis over assets (no sound designer available; consistency with tuner's pure-code audio approach; extensibility).
- **Item 2** — Anchor-based scheduling, not sample-accurate (drift-correct; complexity / quality trade-off; can be revisited if jitter is audible).
- **Item 5** — Strict screen-lifecycle binding (no foreground service in v1; rationale: practice flow always involves an explicit restart anyway).
- **Item 11** — No accent customization in v1 (deferred; forward-compatible via optional `accentPattern` field).

### Decisions NOT needing canonical updates

Pure implementation, planning, or "absence of feature" decisions stay in the Phase 6 plan and this decision log without polluting the canonical docs: Items 9, 10, 12, 13, 15, 16, 17, 19, 20, 21.

**Rationale.**
- `APP_SPECIFICATION.md` is the canonical source for user-facing product behaviour. If a behaviour will affect the user's experience and isn't already documented, it needs a line in the spec.
- `DESIGN.md` is the source for visual/interaction design specifics; the Start/Stop icon content is design-layer.
- `DECISIONS.md` is reserved for architecturally significant or future-constraining decisions — not the full log.
- Implementation details (audio format, synthesis math, validation logic, etc.) live in the Phase 6 plan only.

**Implications for the Phase 6 plan.**
- Phase 6's documentation work is structured as:
  1. Update `APP_SPECIFICATION.md` with the 7 user-facing additions.
  2. Update `DESIGN.md` §8.2 with the Start/Stop button content note.
  3. Add 4 entries to `DECISIONS.md`.
  4. Produce `Phase6-PLAN.md` and `Phase6-REQUIREMENTS.md` from this decision log.
  5. Produce per-sub-phase `Phase6_N-PLAN.md` and `Phase6_N-REQUIREMENTS.md` files as each sub-phase begins (mirroring Phase 5's structure).

---

## All items resolved

The original 14-item sweep and the 8 follow-up items (15–22) are all closed. Phase 6 is now fully scoped at the requirements level. The next step is to fold this decision log into the formal Phase 6 planning documents.
