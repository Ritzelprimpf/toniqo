# Phase 1 — Requirements & Acceptance Criteria

Phase 1 has two kinds of acceptance criteria: things the **agent** is responsible for (writing the code), and things the **user** is responsible for verifying inside Android Studio. The phase is complete when both lists are satisfied.

## Agent Responsibilities

### Project Configuration

- [ ] Package name in `app/build.gradle.kts` is `de.ritzelprimpf.toniqo`
- [ ] `minSdk = 31` (Android 12)
- [ ] `compileSdk` and `targetSdk` are set to a current stable Android version (34 or higher)
- [ ] Kotlin Compose Compiler plugin is applied
- [ ] All library and plugin versions live in `gradle/libs.versions.toml` — no inline version numbers in `build.gradle.kts` files

### Package Skeleton

- [ ] The following packages exist (each containing at least one `.kt` file — a smoke test or a single-line file with only the `package` declaration):
  - [ ] `de.ritzelprimpf.toniqo.tuner` with sub-packages `data`, `domain`, `presentation`, `di`
  - [ ] `de.ritzelprimpf.toniqo.metronome` with sub-packages `data`, `domain`, `presentation`, `di`
  - [ ] `de.ritzelprimpf.toniqo.keyfinder` with sub-packages `data`, `domain`, `presentation`, `di`
  - [ ] `de.ritzelprimpf.toniqo.chordfinder` with sub-packages `data`, `domain`, `presentation`, `di`
  - [ ] `de.ritzelprimpf.toniqo.common` with sub-packages `model`, `util`
  - [ ] `de.ritzelprimpf.toniqo.ui` with sub-packages `theme`, `navigation`, `components`
  - [ ] `de.ritzelprimpf.toniqo.di` (app-level)

### Hilt Setup

- [ ] `ToniqoApplication` class annotated `@HiltAndroidApp` exists at `de.ritzelprimpf.toniqo.ToniqoApplication`
- [ ] `AndroidManifest.xml` references the application class via `android:name=".ToniqoApplication"`
- [ ] `MainActivity` is annotated `@AndroidEntryPoint`
- [ ] Hilt uses **KSP** (not KAPT) — the `com.google.devtools.ksp` plugin is applied and `ksp(libs.hilt.compiler)` appears in `dependencies`

### Dependencies

All of the following are present in `gradle/libs.versions.toml` and referenced from `app/build.gradle.kts`:

- [ ] Jetpack Compose BOM (Material 3, Activity Compose, Lifecycle Compose) — already in the template; verified present
- [ ] Jetpack Navigation Compose — added in this phase
- [ ] AndroidX Lifecycle ViewModel Compose — added in this phase
- [ ] Hilt runtime, compiler (via KSP), and `hilt-navigation-compose` — added in this phase
- [ ] `kotlinx-coroutines-android` — declared explicitly even if transitively available
- [ ] JUnit 4 — already in the template; verified present
- [ ] MockK — added in this phase

### Template Cleanup

- [ ] `Greeting` composable removed from `MainActivity.kt`
- [ ] Template sample strings (e.g., `hello_world`) removed from `res/values/strings.xml`
- [ ] `MainActivity`'s `setContent { ... }` contains only a single phase-1 placeholder composable (clearly named, e.g., `Phase1Placeholder`) that uses `MaterialTheme.colorScheme` and `stringResource(...)`
- [ ] A `phase_1_placeholder` string and an `app_name` string exist in `res/values/strings.xml`

### Tests

- [ ] At least one unit test exists in each of the following test source directories under `app/src/test/java/de/ritzelprimpf/toniqo/`:
  - [ ] `tuner/`
  - [ ] `metronome/`
  - [ ] `keyfinder/`
  - [ ] `chordfinder/`
  - [ ] `common/`
- [ ] Each test has a descriptive method name (no `test1`, `smoke`, etc.)

### Handoff

- [ ] A summary message to the user lists: files added, files modified, files removed, and anything to double-check after Gradle sync

## User Responsibilities (Verification in Android Studio)

The user performs the following and reports any failures back to the agent for fixing:

- [ ] After applying the changes, **File → Sync Project with Gradle Files** completes without errors
- [ ] **Build → Make Project** completes successfully with no errors
- [ ] The app installs and launches on an Android 12+ emulator or device without crashing
- [ ] The placeholder text renders correctly with the template's theme
- [ ] No exceptions appear in Logcat during launch
- [ ] **Run → Run 'app'** unit tests reports all tests green

## Decision Log

- [ ] Any non-trivial decision taken during Phase 1 (e.g., the actual Compose BOM version chosen, deviations from the template, AGP version pinning) is recorded in `DECISIONS.md`
