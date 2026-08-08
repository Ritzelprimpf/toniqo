# CLAUDE.md — Android App Development Guide

This file governs how an AI coding agent should behave when building and extending this Android project. It is the single source of truth for process, architecture, and coding standards.

---

## 1. Core Mandate

Build a clean, testable, maintainable Android app. Every decision must serve that goal. No shortcuts that trade correctness for speed.

This is an Android app written in Kotlin. Do not generate TypeScript, JavaScript, or React files.

---

## 2. Interactive Process — Non-Negotiable

Before writing any code, the agent **must** confirm it has enough information to proceed without ambiguity. If anything is unclear, **stop and ask**. Do not guess, do not assume, do not leave a TODO comment and move on.

### When to stop and ask

- The feature requirements are ambiguous or incomplete.
- There are two or more valid design approaches with meaningfully different tradeoffs.
- A dependency, library version, or API contract is unspecified.
- The expected behavior at an edge case is unknown.
- A naming or structural decision would cascade across the codebase.

### How to ask

Ask one focused question at a time. If multiple blockers exist, list them all upfront and work through them in sequence. Once answered, confirm understanding and then proceed.

**Example:**

> Before I create the `TunerPresetRepository`, I need to clarify two things:
> 1. Should tuning presets be hardcoded constants, a JSON asset, or stored in Room?
> 2. Should the user be able to add custom presets in this phase?
>
> Once you answer, I will proceed.

---

## 3. Project Structure — Feature-First with Clean Architecture Inside Each Module

The top-level structure is **feature-first**: each user-facing module owns its full stack. Inside each module, code is organized by the Clean Architecture layers (data → domain → presentation). Shared code lives in `common/` and `ui/`.

```
app/src/main/java/de/ritzelprimpf/toniqo/
├── tuner/
│   ├── data/                 # Repository implementations, audio I/O, asset/JSON loading
│   ├── domain/
│   │   ├── model/            # Tuner-specific domain models (e.g., TunerPreset)
│   │   ├── repository/       # Repository interfaces
│   │   └── usecase/          # One class per use case
│   ├── presentation/
│   │   ├── ui/               # Composables for this module
│   │   └── viewmodel/        # ViewModels + UI state classes
│   └── di/                   # Hilt module for this feature
├── metronome/                # Same internal structure as tuner/
├── keyfinder/                # Same internal structure
├── chordfinder/              # Same internal structure
├── common/
│   ├── model/                # Shared domain types: Note, Scale, Mode, Chord, Interval
│   └── util/                 # Shared pure utilities (e.g., MusicTheory)
├── ui/
│   ├── theme/                # Color.kt, Type.kt, Theme.kt
│   ├── navigation/           # NavHost, route definitions
│   └── components/           # Reusable composables (e.g., AppNavigationBar)
└── di/                       # App-level Hilt modules (e.g., AppModule)

app/src/test/java/de/ritzelprimpf/toniqo/   # Unit tests mirroring main/
app/src/androidTest/java/de/ritzelprimpf/toniqo/   # Instrumented tests (optional)
```

Deviations from this structure require explicit justification recorded in `DECISIONS.md` and user approval before implementation.

---

## 4. SOLID Design Principles

Every class written must satisfy these principles. Violations are bugs, not style issues.

### Single Responsibility Principle
Each class has exactly one reason to change. A ViewModel does not parse JSON. A Repository does not format strings for the UI. A UseCase does not hold Android context.

### Open/Closed Principle
New behavior is added by extension, not by modifying existing classes. Prefer interfaces and abstract classes at layer boundaries. Adding a new data source should not require changes to the domain layer.

### Liskov Substitution Principle
Any implementation of an interface must be fully substitutable for that interface. A fake/mock implementation used in tests must behave consistently with the real one at the contract level.

### Interface Segregation Principle
Interfaces are narrow and role-specific. A `TunerPresetRepository` interface should not expose methods only needed by one caller. Split it if needed.

### Dependency Inversion Principle
High-level modules (domain, presentation) depend on abstractions, not on concrete implementations. All dependencies are injected via constructor.

**Exception — pure stateless utilities.** A top-level `object` (e.g., `MusicTheory`) is permitted *only* when it has no state, no I/O, and no platform dependencies — i.e., it is mathematically pure. Such utilities do not need to be injected. If at any point a "utility" needs configuration, state, or platform access, it must be converted to a class and injected.

---

## 5. Architecture

The project follows **Clean Architecture** *within each feature module*, with three layers:

```
presentation/  →  domain/  →  data/
```

- **presentation/** contains ViewModels and Composables. It depends only on domain interfaces, use cases, and domain models.
- **domain/** contains use cases, domain models, and repository interfaces. It has zero Android dependencies. Pure Kotlin only.
- **data/** implements domain repository interfaces. It owns all I/O concerns (audio capture/playback, asset loading, preferences, and — if needed — network or database).

Dependency injection is handled by **Hilt**. Each feature module has a dedicated Hilt module under `<module>/di/`. App-level singletons live in `di/` at the top level.

---

## 6. Testing Requirements

Every non-trivial function must be covered by a test. "Non-trivial" means anything with logic, branching, mapping, or I/O.

### Unit tests (required for all business logic)

- Location: `src/test/`
- Framework: **JUnit 4** + **MockK**
- Coverage targets:
  - All UseCase classes: **100%**
  - All ViewModel logic: **100%**
  - All Repository implementations (with fakes): **100%**
  - Utility/mapper functions: **100%**

### Test structure per class

```kotlin
class FindKeysUseCaseTest {

    private val service: KeyFinderService = mockk()
    private val useCase = FindKeysUseCase(service)

    @Test
    fun `returns ranked matches when service succeeds`() { /* ... */ }

    @Test
    fun `returns empty list when no notes match any mode`() { /* ... */ }

    @Test
    fun `prioritizes modes starting on provided tonic`() { /* ... */ }
}
```

Every test method name must describe the behavior being tested, not the implementation.

### Writing tests before or alongside code

For new features: write the test first or write it immediately after the function — not at the end of the task.

### Fakes over mocks for repositories

Prefer hand-written `FakeTunerPresetRepository` implementations over mocks for repository boundaries. This makes tests more robust and self-documenting.

---

## 7. Coding Standards

### Kotlin

- Use `data class` for immutable models.
- Prefer `sealed class` / `sealed interface` for result and state types.
- Use `Flow` for reactive data streams; `StateFlow` in ViewModels.
- Use `suspend fun` for all async operations. No callbacks.
- Never use `!!`. Handle nullability explicitly.
- No logic in `init {}` blocks.

### Naming

| Element | Convention | Example |
|---|---|---|
| Class | PascalCase | `DetectPitchUseCase` |
| Function | camelCase, verb | `detectPitch()` |
| Variable | camelCase | `currentFrequency` |
| Constant | SCREAMING_SNAKE | `SAMPLE_RATE_HZ` |
| Test method | backtick sentence | `` `returns null when buffer is silent` `` |

### No magic numbers or strings

All constants live in a named `companion object` or a top-level `object` under `common/` or the feature's `data/` / `domain/` package. No inline literals with semantic meaning. All user-visible strings live in `res/values/strings.xml`.

### Error handling

Use a sealed result type consistently:

```kotlin
sealed interface Result<out T> {
    data class Success<T>(val data: T) : Result<T>
    data class Error(val cause: Throwable) : Result<Nothing>
}
```

Do not swallow exceptions silently. Every error must surface to the ViewModel's UI state.

---

## 8. Dependency Management

- All library versions are declared in `gradle/libs.versions.toml` (version catalog).
- No version is hardcoded in `build.gradle.kts` files.
- Before adding a new dependency, ask if one already in the project can serve the same purpose.
- Any new dependency must be explicitly approved before it is added.

---

## 9. Git Discipline

Git is the **user's** responsibility — the agent does not commit, push, or invoke `git` from the command line. The agent proposes code changes; the user reviews them, applies them in Android Studio, verifies the build, and commits.

When the agent proposes a change set, it should organize the proposal in a way that maps cleanly to commits the user might want to make:

- One logical change per proposed change set.
- A suggested commit message in the format `<type>: <short description>`
  - Types: `feat`, `fix`, `refactor`, `test`, `chore`, `docs`
  - Example: `feat: add FindKeysUseCase with unit tests`
- Tests are proposed in the same change set as the code they test.
- The agent flags when a proposed change is incomplete or known not to build yet, so the user does not accidentally commit broken code.

---

## 10. What the Agent Must Never Do

- Generate placeholder implementations with `TODO()` and move on without flagging it. (Phase 2 stubs are an exception, but they must be flagged explicitly in the commit message.)
- Write a class with more than one responsibility and justify it as "pragmatic."
- Skip tests because the logic "seems obvious."
- Add a library or change a major architectural decision without asking first.
- Guess at requirements. Ambiguity is a blocker, not a challenge to improvise around.

---

## 11. Programming Languages and Frameworks

| Concern | Choice |
|---|---|
| Language | Kotlin |
| UI framework | Jetpack Compose (Material 3) |
| Build system | Gradle (Kotlin DSL) with version catalog |
| Dependency injection | Hilt |
| State management | Compose State + Kotlin `StateFlow` |
| Navigation | Jetpack Navigation Compose |
| Async | Kotlin Coroutines |
| Testing | JUnit 4, MockK |

**Not part of the baseline stack** (add only if a feature requires them, with explicit approval):
- Retrofit / OkHttp — no network calls are anticipated.
- Room — tuning presets fit comfortably as hardcoded constants or a JSON asset; Room is overkill unless user-defined presets are added later.

---

## 12. File Extensions

- Kotlin files: `.kt`
- Build scripts: `.kts` (Kotlin DSL)

---

## 13. Additional Guidelines

- Use the specified project structure and file organization (Section 3).
- Follow the SOLID design principles strictly (Section 4), with the one stateless-utility exception noted there.
- Ensure all code adheres to the Kotlin coding standards (Section 7).
- Use Jetpack Compose for UI components and state management.
- Implement error handling as described in Section 7.
- Record every non-trivial architectural decision in `DECISIONS.md`.
- Follow the design system as described in Section 14.

---

## 14. Design System

The project has a separate design contract: `DESIGN.md`. It is authoritative for appearance and dimension — colours, typography, spacing, radii, components, motion, and module-specific visuals. `APP_SPECIFICATION.md` is authoritative for behaviour. Where the two conflict, raise the conflict — do not silently choose.

### When the agent must read `DESIGN.md`

- **Always**, when working on any phase from Phase 3 onward.
- **Before any UI code is written**, even a placeholder.
- **Before answering questions about** colour, spacing, type, components, or motion.

### Rules the agent must follow

- **No hardcoded design values.** Every colour, font, spacing, and radius comes from the `Tq` token object (defined in Phase 3) or from `MaterialTheme.colorScheme.*` (which is itself wired to `Tq` via `ToniqoTheme`). No `Color(0xFF...)` outside `Tq.kt`. No `14.sp`, `16.dp`, `8.dp` literals outside the token object — use `Tq.Type.*`, `Tq.Sp.*`, `Tq.Radius.*` instead.
- **No new components without checking `DESIGN.md` §6 first.** If a button, chip, card, or other primitive is needed, see if `DESIGN.md` §6 already defines it. If yes, implement it per spec. If no, stop and ask before designing a new pattern.
- **Respect the "Don'ts" list in `DESIGN.md` §12.** No emoji, no gradient decoration, no drop shadows on cards, no SVG illustrations, no glows outside the two specified ones, no Material You dynamic colour.
- **Respect the open questions in `DESIGN.md` §14.** These are explicit "stop and ask" items. Do not improvise an answer.
- **Accessibility behaviours from `DESIGN.md` §13 are non-negotiable.** `NonScalingText` for `display.xl` / `display.l`. Reduced-motion handling per §9. Tap targets ≥ 44×44dp.

### How design tokens get implemented

Phase 3 implements the entire token layer (`ui/theme/Tq.kt`, `ToniqoTheme`, `NonScalingText`, fonts). Every subsequent UI phase consumes those tokens. If a screen needs a new component primitive that isn't yet built, build it in `ui/components/` as part of that phase and reuse it thereafter.

---

## 15. Compiling and Testing from the CLI

This machine has no `JAVA_HOME` set and no `java`/`javac` on `PATH` by default, so a bare `./gradlew ...` fails immediately with a `JAVA_HOME is not set` error. Do not go hunting the filesystem for a JDK each time — use Android Studio's own bundled JBR (JetBrains Runtime), the same JDK Android Studio itself builds with:

```
JAVA_HOME=/home/ritzelprimpf/.local/share/JetBrains/Toolbox/apps/android-studio/jbr
```

Prefix any Gradle invocation with it, e.g.:

```bash
JAVA_HOME=/home/ritzelprimpf/.local/share/JetBrains/Toolbox/apps/android-studio/jbr ./gradlew :app:compileDebugKotlin -q
JAVA_HOME=/home/ritzelprimpf/.local/share/JetBrains/Toolbox/apps/android-studio/jbr ./gradlew :app:testDebugUnitTest -q
```

Notes:

- If Android Studio is ever reinstalled or updated to a new version directory, re-locate the JBR with `find /home/ritzelprimpf/.local/share/JetBrains/Toolbox/apps -maxdepth 2 -iname jbr -type d` and update this section.
- `:app:testDebugUnitTest` compiles the **entire** unit test source set before running anything, even when filtered with `--tests`. A pre-existing compile error in an unrelated test file (e.g. a different feature module) will fail the whole task — that is not necessarily a regression caused by the current change. Check `git status`/`git diff` to confirm whether the failing file was touched before concluding a change broke it.
- The agent may compile and run tests from the CLI using the above. This does not change Section 9: the agent still does not `git commit`, `git push`, or otherwise act on git history — compiling/testing is verification, not a git operation.
