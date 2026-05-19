# Toniqo — Future Improvements

This file tracks deliberate technical deferrals: things the team considered during implementation but explicitly deferred, with enough context to pick them up later without re-litigating the original decision.

Each entry records what was deferred, why, and what a future implementation would look like.

---

## Context-aware enharmonic spelling for `frequencyToNote`

**Current behaviour.** `MusicTheory.frequencyToNote()` always spells accidentals with sharps (e.g. it returns `C#4`, never `Db4`). The same sharp-only rule applies to `Note.displayName()` unless `useFlats = true` is passed explicitly.

**Why deferred.** Choosing the correct enharmonic spelling (sharp vs. flat) is only possible when the musical key is known — `C#` is correct in the key of A major, but `Db` is correct in the key of F minor. The Guitar Tuner's pitch-detection path does not know the key; it works chromatically. Implementing key-aware spelling in Phase 5.1 would add unused complexity to a layer that has no access to key information.

**What a future implementation looks like.**

1. Add a `fun preferFlats(mode: Mode): Boolean` helper to `MusicTheory` (or inline it) that returns `true` for modes that conventionally use flat spellings (Phrygian, Aeolian, Dorian, Locrian, Mixolydian starting on a non-sharped root, etc.).
2. Add an overload of `Note.displayName(key: Scale): String` that chooses sharp or flat based on the key's convention.
3. Alternatively, add a `NoteName.inKey(mode: Mode, root: NoteName): String` that returns the correct spelling for a pitch class within a given mode.
4. Update `Chord.displayName()` and scale-display helpers to accept an optional key context.

**Affected modules.** Key Finder (results display), Chord Finder (chord labels), and any future transposition feature. The Guitar Tuner's chromatic readout does **not** need this — flat/sharp is a user preference there, handled separately.

**Prerequisite.** Context-aware spelling should only be added once a module actually needs it (likely when the Chord Finder or Key Finder display layer is implemented).
