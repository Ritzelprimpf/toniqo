# Toniqo — Implementation Notes

These notes apply to the entire project and should be read once at the start of any implementation session.

> **Required reading.** Every session: `PROJECT_PLAN.md`, `CLAUDE.md`, this file, `DECISIONS.md`. For Phase 3 onward, also `DESIGN.md`. For functional behaviour questions on any module, `APP_SPECIFICATION.md`.

## Target Platform

- **Platform:** Android
- **Language:** Kotlin
- **Minimum SDK:** 31 (Android 12). Chosen deliberately to target modern devices only — released October 2021, covers the realistic device base for the target audience. *(Note: an earlier draft said "minSdk 12" — that was a misunderstanding of "Android version 12" as an SDK level. The correct SDK level for Android 12 is 31.)*
- **Target SDK:** 34 (or current stable at implementation time)
- **Compile SDK:** matches target SDK
- **Build system:** Gradle with Kotlin DSL (`build.gradle.kts`) and the version catalog (`gradle/libs.versions.toml`)

## Architecture

- The top-level layout is **feature-first**: each module (`tuner`, `metronome`, `keyfinder`, `chordfinder`) owns its full vertical stack.
- **Inside each module**, Clean Architecture layering applies: `data/` → `domain/` → `presentation/`.
- Shared cross-feature code lives in `common/` (pure music-theory primitives) and `ui/` (theme, navigation shell, reusable composables).
- See `CLAUDE.md` Section 3 for the full directory layout. The same layout is the source of truth for all phase plans.
- Follow the **MVVM pattern** at the presentation layer (ViewModel ↔ Composable via `StateFlow`).
- Navigation between modules uses **Jetpack Navigation Compose**.

## Module Isolation

Only one module is active at a time. Module selection is handled by top-level navigation. The chosen mechanism — bottom navigation bar — is decided and recorded in `DECISIONS.md`; it is implemented in Phase 4 per `DESIGN.md` §6.4.

A module must not reference another module's package. Cross-feature concerns live in `common/` or `ui/`.

## Testing

- All business logic must have unit tests.
- Framework: **JUnit 4** + **MockK**. *(JUnit 5 was considered but rejected: Android's instrumented test runner still expects JUnit 4 by default, and the `android-junit5` plugin adds complexity without benefit for this project. Decision recorded in `DECISIONS.md`.)*
- Tests must pass before a phase is considered complete.
- Compose UI tests are optional in early phases but encouraged for navigation-level flows from Phase 3 onward.
- Optional: **Turbine** for `Flow` testing if `StateFlow` collection logic gets non-trivial.

## Audio

- Microphone capture (tuner) uses **`AudioRecord`** in a coroutine-driven loop.
- Click playback (metronome) uses **`AudioTrack`** in streaming mode for low latency. `SoundPool` is acceptable as a fallback if `AudioTrack` integration proves troublesome — record the choice in `DECISIONS.md`.
- Microphone access requires the `RECORD_AUDIO` permission, requested at runtime via the Activity Result API (`rememberLauncherForActivityResult`).

## Music Theory Primitives

The `common/` package contains reusable music-theory constructs used across multiple modules. These are pure Kotlin — no Android dependencies — so they are trivially unit-testable.

- **Note names & enharmonic equivalents**: `C`, `C#`/`Db`, `D`, `D#`/`Eb`, `E`, `F`, `F#`/`Gb`, `G`, `G#`/`Ab`, `A`, `A#`/`Bb`, `B`
- **Frequency table** for the equal-tempered scale anchored at A4 = 440 Hz (configurable; see `APP_SPECIFICATION.md`)
- **Scale/mode definitions**: interval patterns for the 7 diatonic modes (Ionian, Dorian, Phrygian, Lydian, Mixolydian, Aeolian, Locrian)
- **Chord definitions**: triad qualities per scale degree (Major, Minor, Diminished, Augmented), plus optional seventh-chord extensions

`MusicTheory` may be a top-level `object` because it is stateless and pure. This is the documented exception to the no-singletons rule (see `CLAUDE.md` Section 4).

## Conventions

- **No hardcoded strings in UI** — use `res/values/strings.xml`.
- **No hardcoded colors** — use the app's Material 3 theme.
- **No magic numbers** — extract BPM limits, frequency constants, tolerances, etc. into named constants.
- All public classes, functions, and non-trivial logic must have KDoc comments.
- Use meaningful names: prefer `fundamentalFrequencyHz` over `freq`, `targetNote` over `note`.

## Dependencies (Baseline Starting Set)

All versions go in `gradle/libs.versions.toml`. Confirm the latest stable versions at implementation time.

| Library | Purpose |
|---|---|
| Jetpack Compose BOM | UI framework (Material 3, Activity Compose, Lifecycle Compose) |
| Jetpack Navigation Compose | In-app navigation |
| AndroidX Lifecycle ViewModel + Compose | MVVM + state collection in composables |
| Hilt + `hilt-navigation-compose` | Dependency injection (use KSP, not KAPT) |
| Kotlin Coroutines (`kotlinx-coroutines-android`) | Async audio processing |
| JUnit 4 | Unit testing |
| MockK | Mocking in unit tests |
| Turbine *(optional)* | `Flow` testing |

**Not in the baseline** (add only if a phase requires them, with explicit approval):
- Retrofit / OkHttp — no network calls are anticipated.
- Room — tuning presets fit as hardcoded constants or a JSON asset. Revisit only if user-defined presets become a requirement.

## Working with Android Studio (Human/Agent Split)

The project lives in **Android Studio**. The human user is the only one with hands on the IDE: they run builds, launch the emulator, debug, and read Logcat. The agent produces code changes in chat for the user to apply.

This shapes how the agent works:

- **The agent does not invoke `./gradlew`** to "verify" anything. Android Studio manages the JDK toolchain (it bundles JetBrains Runtime), AGP version, and Gradle wrapper. Running Gradle from a separate environment can pick a different JDK and produce misleading results.
- **The agent does not specify a Java toolchain** (`jvmToolchain { ... }`) unless the project already pins one. Source/target compatibility values that came with the Android Studio template stay as they are.
- **Build success is verified by the user** in Android Studio (Build → Make Project) and reported back. The agent's job is to produce code that *should* build; if it does not, the user reports the error and the agent fixes it.
- **Runtime success is verified by the user** by launching on an emulator or device and watching Logcat. The agent does not have access to either.
- **Tests are verified by the user** via Android Studio's run configurations (or `./gradlew test` if they prefer). Same loop: write tests, hand off, user runs them, agent fixes any failures.

In practice this means each work cycle looks like:
1. Agent proposes changes (new/modified/removed files with full contents).
2. User applies them in Android Studio and runs build/tests.
3. User reports results — green, or errors with the relevant Logcat / build output.
4. Agent fixes and the cycle repeats.

The agent never marks a phase complete on its own. The user confirms the user-side checklist in each `PhaseN-REQUIREMENTS.md` before the phase is closed.

## Phase Completion Criteria

A phase is complete when:
1. All agent-side requirements in the phase's `REQUIREMENTS.md` are met.
2. The user has run Gradle sync, Build → Make Project, and the relevant tests in Android Studio, and reports them green.
3. The app compiles and runs without crashes on the phase's happy path (user-verified).
4. Code is reasonably clean — no debug logging left in, no obvious dead code, no commented-out blocks.
5. Any non-trivial decision taken during the phase is recorded in `DECISIONS.md`.
