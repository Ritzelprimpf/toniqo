# Phase 1 — Project Setup

## Goal

Adapt the existing Android Studio project to match the project's conventions and prepare it for the backend and frontend scaffolding in Phases 2 and 3.

This phase produces no user-visible functionality. Its job is to take the **Empty Activity (Compose)** template Android Studio generated and turn it into a foundation the rest of the plan can build on.

## Starting Point

The user has already created the Android Studio project from the **Empty Activity (Compose)** template with package name `de.ritzelprimpf.toniqo` and Kotlin DSL build scripts. The agent does **not** create the project — it modifies what is already there.

What the template provides out of the box (verify and keep unless noted):
- `app/build.gradle.kts`, `build.gradle.kts`, `settings.gradle.kts` (Kotlin DSL)
- A default `MainActivity` using Compose
- A `Greeting` composable and a basic `Theme.kt` / `Color.kt` / `Type.kt` triple under `ui/theme/`
- `AndroidManifest.xml` registering `MainActivity` as the launcher
- A version catalog at `gradle/libs.versions.toml` (recent template versions; if missing, create it as the first task)
- An `androidx.compose.bom`, `material3`, `activity-compose`, and `lifecycle-runtime-ktx` already declared
- A basic unit test (`ExampleUnitTest`) and instrumented test (`ExampleInstrumentedTest`)

## Scope

- Verify the build configuration matches the project's standards (minSdk 31, Kotlin version, Compose Compiler plugin, Java toolchain managed by Android Studio)
- Extend the version catalog with the additional baseline dependencies (Hilt, Navigation Compose, MockK, etc.)
- Add the Hilt plugin and create the `ToniqoApplication` class
- Create the feature-first package skeleton (empty packages, smoke tests only)
- Replace the template's `Greeting` placeholder with a clearly-marked phase-1 stub (Phase 3 will replace `MainActivity`'s content properly)
- Confirm everything still compiles in Android Studio (this confirmation is performed by the user, not the agent)

## Out of Scope

- No business logic
- No real navigation (Phase 3)
- No UI beyond a placeholder in `MainActivity`
- The agent does **not** run Gradle or attempt to launch the emulator — Android Studio handles the toolchain, build, and runtime. The user verifies these and reports issues back.

## Toolchain and Environment

The Java/JDK toolchain is whatever Android Studio supplies and configures. The agent does **not**:
- Specify a `jvmToolchain(...)` block unless one was already in the template
- Recommend a specific JDK install
- Run `./gradlew` from a terminal to "verify" anything

If the template already pins Java source/target compatibility (typically 17 or 21), leave it as-is. If a future phase needs to change it, that becomes its own decision recorded in `DECISIONS.md`.

## Steps

### 1. Inspect what the template gave us

Before changing anything, view and confirm the following in the existing project:
- The package name in `app/build.gradle.kts` matches `de.ritzelprimpf.toniqo`
- `minSdk` is at least 31 (if the template defaulted lower, raise it; record a decision if the template offered a different default)
- `compileSdk` and `targetSdk` are set to a current stable Android version (34 or higher)
- The Kotlin Compose Compiler plugin is applied (Kotlin 2.x requires this — recent templates apply it automatically)
- `gradle/libs.versions.toml` exists; if it does not, create it and migrate any inline versions into it

Report any deviations from these expectations before proceeding.

### 2. Extend the version catalog

Add to `gradle/libs.versions.toml` (do not hardcode versions in `build.gradle.kts`):
- Hilt (`hilt-android`, `hilt-compiler`, plus the `hilt-navigation-compose` integration)
- Jetpack Navigation Compose
- AndroidX Lifecycle ViewModel Compose (already present as `lifecycle-runtime-ktx`; add `lifecycle-viewmodel-compose`)
- `kotlinx-coroutines-android` (often pulled transitively, but declare it explicitly)
- MockK (test scope)
- Turbine (test scope, optional — only if a test in this phase needs it)

JUnit 4 is already present via the template — keep it.

### 3. Apply the Hilt plugin

- Add the Hilt Gradle plugin to the version catalog and apply it in `app/build.gradle.kts`
- Use **KSP** (not KAPT) for Hilt's compiler. The template uses KSP by default for modern Compose; confirm and add `id("com.google.devtools.ksp")` to the plugins block if not already applied.
- Add `ksp(libs.hilt.compiler)` to the `dependencies` block

### 4. Create the `ToniqoApplication` class

- Create `de.ritzelprimpf.toniqo.ToniqoApplication` annotated with `@HiltAndroidApp`
- Register it in `AndroidManifest.xml` via `android:name=".ToniqoApplication"`
- Annotate `MainActivity` with `@AndroidEntryPoint`

### 5. Create the feature-first package skeleton

Create the following empty packages under `de.ritzelprimpf.toniqo`. Each package needs at least one `.kt` file because Gradle does not pick up empty source directories — the smoke tests in step 7 can serve this purpose, or place a single-line file containing only the `package` declaration in each.

- `tuner/` (with sub-packages `data`, `domain`, `presentation`, `di`)
- `metronome/` (same sub-packages)
- `keyfinder/` (same)
- `chordfinder/` (same)
- `common/` (with sub-packages `model`, `util`)
- `ui/` (with sub-packages `theme`, `navigation`, `components`)
- `di/` (app-level Hilt modules)

The template already has `ui/theme/`. Leave `Theme.kt`, `Color.kt`, `Type.kt` where they are if they're already under `de.ritzelprimpf.toniqo.ui.theme`; Phase 3 will customize them.

### 6. Tame the template's placeholders

The Empty Activity template ships with a `Greeting("Android")` call and matching strings in `res/values/strings.xml`. These will be replaced properly in Phase 3 but should not be left looking like real content right now.

- Replace the body of `MainActivity`'s `setContent { ... }` with a single placeholder composable that displays the app name and a "Phase 3 will replace this" note. Use `MaterialTheme.colorScheme` and `stringResource(...)` — no hardcoded color or string literals.
- Remove the template's `Greeting` composable and its `@Preview`.
- Remove the template's sample string (`hello_world` or similar) and add `app_name` (if not present) plus a single `phase_1_placeholder` string.

### 7. Write smoke-test unit tests

Add one trivial passing unit test in each of these test source directories under `app/src/test/java/de/ritzelprimpf/toniqo/`:

- `tuner/`
- `metronome/`
- `keyfinder/`
- `chordfinder/`
- `common/`

Test method names must be descriptive (e.g., `` `tuner package compiles and tests run` ``) — not `test1` or `smoke`.

The template's `ExampleUnitTest` can be deleted, or left in place if you prefer to keep a known-good reference test.

### 8. Hand off for verification

When the changes are complete, hand off to the user with a short summary of:
- Files added
- Files modified
- Files removed
- Anything the user should double-check in Android Studio (e.g., "after Gradle sync, the Hilt plugin should appear without errors in the Build Output panel")

The agent does **not** attempt to verify the build or run the app. The user runs the build in Android Studio (Build → Make Project) and the app on an emulator, then reports any failures back so the agent can fix them.

## Completion Criteria

See `Phase1-REQUIREMENTS.md`.
