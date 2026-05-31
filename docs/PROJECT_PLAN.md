# Toniqo — Project Plan

## Overview

**Toniqo** is an Android application targeting experienced guitar players as an everyday companion tool. It is not a beginner learning app, but a practical utility for daily use by intermediate-to-advanced guitarists.

## Document Structure

This project is organized into the following documents. When implementing a specific phase, only the relevant phase documents need to be loaded — the general project docs apply throughout.

```
PROJECT_PLAN.md            ← This file. Start here.
CLAUDE.md                  ← Agent process, architecture, and coding standards (always read)
IMPLEMENTATION_NOTES.md    ← Tech stack and conventions (always read)
APP_SPECIFICATION.md       ← Functional specification of all modules (behaviour)
DESIGN.md                  ← Design tokens, components, and module visuals (appearance)
DECISIONS.md               ← Architectural decision log (append-only)

Phase1-PLAN.md             ← Phase 1: Project Setup (adapt Android Studio template)
Phase1-REQUIREMENTS.md
Phase2-PLAN.md             ← Phase 2: Backend Outline (packages, classes, method stubs)
Phase2-REQUIREMENTS.md
Phase3-PLAN.md             ← Phase 3: Design Tokens (theme implementation)
Phase3-REQUIREMENTS.md
Phase4-PLAN.md             ← Phase 4: Navigation Shell & Placeholders
Phase4-REQUIREMENTS.md
Phase5-PLAN.md             ← Phase 5: Guitar Tuner (sub-phase files added during 5.1)
```

## App Modules

The app is structured around discrete, independently selectable modules. Only one module is active at a time.

| Module | Description | Status |
|---|---|---|
| Guitar Tuner | Chromatic / preset-driven tuner using device microphone | **Done (Phase 5)** |
| Metronome | BPM / time-signature metronome with start/stop control | **Done (Phase 6)** |
| Key Finder | Identifies musical keys/modes from a set of input notes | **Done (Phase 7)** |
| Chord Finder | Lists chords for a selected musical mode | **Next (Phase 8)** |

## Implementation Phases

| Phase | Name | Description |
|---|---|---|
| 1 | Project Setup | Adapt the Android Studio template: dependencies, Hilt, package skeleton, smoke tests |
| 2 | Backend Outline | Package structure, empty classes, method stubs |
| 3 | Design Tokens | Implement the `DESIGN.md` system as Compose code (palette, type, spacing, radii, `ToniqoTheme`) |
| 4 | Navigation Shell & Placeholders | Bottom nav, module placeholder screens, Info section |
| 5 | Guitar Tuner | Full implementation of the Guitar Tuner module (sub-phased) |
| 6+ | Remaining Modules | Order to be confirmed after Phase 5. Likely sequence: Metronome → Chord Finder → Key Finder, because the metronome's audio output stack reuses Phase 5 audio learnings, and Chord Finder is a pure-logic prerequisite for parts of Key Finder. |

## Reading Guide for the Agent

- **Starting any phase:** Read `PROJECT_PLAN.md` → `CLAUDE.md` → `IMPLEMENTATION_NOTES.md` → `DECISIONS.md` → the relevant `PhaseN-PLAN.md` and `PhaseN-REQUIREMENTS.md`.
- **For UI-touching work (Phase 3 onward):** Also read `DESIGN.md`. For Phase 5+ work on a specific module's screen, focus on the relevant subsection of `DESIGN.md` §8.
- **For functional behaviour:** See `APP_SPECIFICATION.md`. When it conflicts with `DESIGN.md`, behaviour wins on logic and design wins on appearance — raise the conflict, don't silently choose.
- **Adding new phases:** Add `PhaseN-PLAN.md` and `PhaseN-REQUIREMENTS.md` at the repository root following the existing naming pattern.
- **Recording a decision:** Append an entry to `DECISIONS.md` with a date, the decision, the alternatives considered, and the rationale. Never overwrite past entries.
