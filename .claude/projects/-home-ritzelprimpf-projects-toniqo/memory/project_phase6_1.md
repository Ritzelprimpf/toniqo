---
name: project-phase6-1
description: Phase 6.1 completion — click synthesizer & audio format foundation for the metronome
metadata:
  type: project
---

Phase 6.1 completed on 2026-05-21. Pure-Kotlin audio foundation for the metronome.

**Files added:**
- `metronome/data/audio/MetronomeAudioFormat.kt` — 48 kHz / mono / 16-bit PCM constants
- `metronome/data/audio/ClickParameters.kt` — v1 synthesis parameters (tunable in 6.2)
- `metronome/data/audio/ClickSynthesizer.kt` — PCM click buffer generator (sine + attack/decay)
- `metronome/domain/model/ClickKind.kt` — ACCENTED / STANDARD / SUBDIVISION enum
- `metronome/domain/model/BeatPattern.kt` — `clicksPerBar()` and `clickKindFor()` pure functions
- `metronome/domain/model/TempoDescriptor.kt` — 5-label tempo lookup + `tempoDescriptorFor()`
- Tests: `ClickSynthesizerTest`, `SubdivisionTest`, `BeatPatternTest`, `TempoDescriptorTest`

**Files modified:**
- `metronome/domain/model/Subdivision.kt` — added `val multiplier: Int` constructor argument
- `docs/DECISIONS.md` — 7 new entries (Items 1, 2, 5, 11, plus 6.1-specific entries)

**Architectural deviation from plan:** `ClickKind` placed in `domain/model/` (not `data/audio/` as
the plan specified) to avoid a domain→data dependency. `BeatPattern.kt` in `domain/model/` returns
`ClickKind`; placing `ClickKind` in `data/` would violate Clean Architecture. Recorded in DECISIONS.md.

**Why:** Sets up all pure-JVM audio math that Phase 6.2 (AudioTrack player) will consume.
**How to apply:** Phase 6.2 imports `ClickSynthesizer`, `ClickKind`, `MetronomeAudioFormat`,
`clicksPerBar`, `clickKindFor` directly. No structural changes needed.
