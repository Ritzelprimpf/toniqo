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

## (Template for future entries)

## YYYY-MM-DD — Short title of decision

**Decision.** What was chosen.

**Alternatives considered.** What else was on the table, briefly.

**Rationale.** Why this choice won.

**Supersession trigger.** (Optional) What would cause us to revisit.
