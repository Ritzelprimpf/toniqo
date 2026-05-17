# Toniqo — App Specification

This document describes the full functional specification for all modules. It is the source of truth for what each module does and how it should behave from a user's perspective.

Reference this document when implementing any module to understand expected behavior, edge cases, and UI requirements.

> **Companion document.** Visual design — colours, typography, spacing, components, motion, module visuals — lives in `DESIGN.md`. This file is authoritative for **behaviour**; `DESIGN.md` is authoritative for **appearance**. Where the two conflict, raise the conflict rather than silently choose.

---

## General UX

- The app always has exactly one module active at a time.
- Module switching is available from a persistent navigation element. The specific mechanism (bottom navigation bar vs. drawer) is decided in Phase 3 and recorded in `DECISIONS.md`.
- An **Info / About section** is accessible from the navigation. It contains:
  - App version info
  - Help articles (per module)
  - Legal documents (privacy policy, licenses)
  - Links to rate the app and share the app

---

## Module: Guitar Tuner

### Purpose

Helps a guitarist tune their instrument by listening to a string played on the guitar and comparing it to the target pitch.

### Tuning Presets

The tuner ships with the following presets, organized by string count and category. Notes are listed **from lowest string to highest** — the order in which the tuner cycles through them.

#### 6-string

**Standard**
- E Standard: E2, A2, D3, G3, B3, E4
- Eb Standard (Half Step Down): Eb2, Ab2, Db3, Gb3, Bb3, Eb4
- D Standard (Whole Step Down): D2, G2, C3, F3, A3, D4
- C# Standard: C#2, F#2, B2, E3, G#3, C#4

**Open**
- Open D: D2, A2, D3, F#3, A3, D4
- Open G: D2, G2, D3, G3, B3, D4
- Open E: E2, B2, E3, G#3, B3, E4
- Open C: C2, G2, C3, G3, C4, E4
- Open A: E2, A2, E3, A3, C#4, E4
- DADGAD: D2, A2, D3, G3, A3, D4

**Dropped**
- Drop D: D2, A2, D3, G3, B3, E4
- Drop C#: C#2, G#2, C#3, F#3, A#3, D#4
- Drop C: C2, G2, C3, F3, A3, D4
- Drop B: B1, F#2, B2, E3, G#3, C#4
- Drop A: A1, E2, A2, D3, F#3, B3

#### 7-string

**Standard**
- B Standard: B1, E2, A2, D3, G3, B3, E4
- A# / Bb Standard: A#1, D#2, G#2, C#3, F#3, A#3, D#4
- A Standard: A1, D2, G2, C3, F3, A3, D4

**Open**
- Open Bm: B1, F#2, B2, D3, F#3, B3, D4
- Open B: B1, F#2, B2, D#3, F#3, B3, D#4

**Dropped**
- Drop A (7-string): A1, E2, A2, D3, G3, B3, E4
- Drop G# / Ab: G#1, D#2, G#2, C#3, F#3, A#3, D#4
- Drop G: G1, D2, G2, C3, F3, A3, D4

#### 8-string

**Standard**
- F# Standard: F#1, B1, E2, A2, D3, G3, B3, E4
- F Standard: F1, A#1, D#2, G#2, C#3, F#3, A#3, D#4
- E Standard (8-string): E1, A1, D2, G2, C3, F3, A3, D4

**Open**
- Open E (8-string): E1, B1, E2, G#2, B2, E3, G#3, B3

**Dropped**
- Drop E (8-string): E1, B1, E2, A2, D3, G3, B3, E4
- Drop D# / Eb (8-string): D#1, A#1, D#2, G#2, C#3, F#3, A#3, D#4
- Drop D (8-string): D1, A1, D2, G2, C3, F3, A3, D4

> **Implementation note.** Each preset is identified by a stable string ID (e.g., `six_string_standard_e`) so presets can be referenced from saved state and analytics without depending on display names. Presets are hardcoded constants for the initial release. User-defined presets are out of scope until a future phase.

### Tuning Flow

1. The user selects a tuning from the categorized list.
2. The tuner targets the **lowest string first** and advances string by string upward.
3. The device microphone listens for audio input continuously.
4. The app detects the fundamental frequency of the incoming audio using a pitch-detection algorithm (YIN is the chosen default; see Phase 4 plan).
5. The detected frequency is compared to the target frequency for the current string.
6. **Visual feedback** is displayed showing:
   - Whether the current pitch is **flat**, **in tune**, or **sharp**
   - How far off the pitch is, in cents, via a needle/gauge metaphor and a color indicator
7. When the detected pitch is within the in-tune tolerance for a sustained moment (see below), the tuner advances to the next string.
8. After all strings are tuned, a success state is shown.
9. The user can also tap a specific string in the UI to jump to it out of order.

### Reference Pitch & Tolerance

- **Reference pitch:** A4 = **440 Hz** by default. The user can optionally select A4 = 432 Hz from the tuner settings.
- **In-tune tolerance:** **±5 cents**. To advance to the next string, the pitch must remain within tolerance for **at least 500 ms** of continuous detection (to avoid spurious advances from transients).

### Permissions

- Requires `RECORD_AUDIO` permission (must be requested at runtime via the Activity Result API).
- If permission is denied, display a clear explanation and a prompt to grant it from system settings.

### Technical Notes

- Frequency for any equal-tempered semitone offset `n` from A4: `f = 440 × 2^(n / 12)`.
- Pitch detection runs on a background coroutine; results are posted to UI via `StateFlow`.
- Target accuracy: detect pitch within ±1 cent under normal playing conditions on the open strings of a standard guitar.

---

## Module: Metronome

### Purpose

Provides an audible click track at a user-defined tempo and time signature.

### Parameters

| Parameter | Range / Options | Default |
|---|---|---|
| BPM | 1 – 300 | 120 |
| Time Signature | 2/4, 3/4, 4/4, 5/4, 6/8, 7/8, 9/8, 12/8 *(expandable)* | 4/4 |
| Subdivision | None (quarter), Eighth notes, Sixteenth notes, Eighth triplets | None |

**Terminology clarification.** "Time signature" defines the bar (e.g., 4/4 = four quarter-note beats per measure). "Subdivision" defines how each beat is internally divided for additional clicks — these are *quieter* clicks layered between the main beats, not a replacement for them. The first beat of each measure still gets the accented click regardless of subdivision.

### Controls

- **Start / Stop** button — toggles the metronome on and off.
- BPM can be adjusted while the metronome is running (changes apply on the next beat).
- Time signature and subdivision are adjustable; changing either mid-run restarts the beat cycle from beat 1 on the next downbeat.
- BPM input: tap to type a value, drag a slider, or use +/− buttons. A "tap tempo" affordance (user taps a button repeatedly to set BPM) is included.

### Visual Feedback

- A visual indicator (flashing element or animated dot) pulses on each beat.
- The current beat position within the measure is highlighted (e.g., beat 1 of 4 is accented visually).

### Audio

- The first beat of each measure uses an **accented click** (higher pitch or louder).
- Remaining main beats use a **standard click**.
- Subdivision clicks (if enabled) are quieter and at a different pitch from the main click.
- Audio must be low-latency. Use `AudioTrack` in streaming mode (see `IMPLEMENTATION_NOTES.md`).

---

## Module: Key Finder

### Purpose

Identifies which musical key(s) or mode(s) best match a set of notes provided by the user.

### Input

- A **set of notes** (unordered), e.g.: `C, D, E, F, G, A, B`
- An optional **tonic / starting note**, e.g.: `C`

Notes are entered as letter names with optional accidentals: `C`, `C#`, `Db`, `A#`, etc. Enharmonic equivalents (`C#` vs `Db`) are treated as the same pitch class for matching purposes; the display label preserves the user's spelling where possible.

### Output

- A **ranked list** of matching modes/keys, best matches first.
- Each result shows:
  - The mode name, e.g. `A Natural Minor`, `C Major (Ionian)`
  - A match score (percentage of input notes that belong to the mode)
  - A "full match" badge if every input note is in the mode
- If a tonic is provided, modes whose root is the tonic are ranked higher (ties broken by match score, then alphabetical mode name).

### Matching Logic

- The app compares the user's note set against all 7 modes × 12 roots = **84 possible diatonic scales**.
- A mode is a **full match** if all input notes are contained within it.
- A mode is a **partial match** if at least one but not all input notes are in it (useful for ambiguous cases or chromatic passing tones).
- A mode with zero matching notes is excluded from results.
- Example: input `{C, D, E, F}` with tonic `A` → best match is A Natural Minor (Aeolian) (tonic-preferred, full match); C Major (Ionian) also full-matches but ranks below because it doesn't match the user's tonic.

### Modes to Support (Western Diatonic)

Ionian (Major), Dorian, Phrygian, Lydian, Mixolydian, Aeolian (Natural Minor), Locrian.

Pentatonic, harmonic/melodic minor, and other scale types may be added in a future phase.

---

## Module: Chord Finder

### Purpose

Given a musical mode (key), shows all diatonic chords that naturally belong to it.

### Input

- A **root note**, e.g.: `C`
- A **mode**, e.g.: `Major (Ionian)`

### Output

- A list of diatonic chords for the selected key/mode, organized by scale degree (I, ii, iii, IV, V, vi, vii°).
- Each chord shows:
  - Chord name (e.g., `C`, `Dm`, `Em`, `F`, `G`, `Am`, `Bdim`)
  - Scale degree (Roman numeral, with case denoting quality in standard notation)
  - Chord quality (Major, Minor, Diminished, Augmented)
- **Seventh chord toggle**: when enabled, chord names display their seventh-chord extensions (e.g., `Cmaj7`, `Dm7`, `G7`, `Bm7b5`).

### Scope

- Initial implementation: triads, with the seventh-chord toggle as part of the first release.
- Modes supported: the same 7 diatonic modes as Key Finder.

---

## Info / About Section

- **App version** (pulled from build config)
- **Help** — per-module help text explaining how to use each tool
- **Privacy Policy** — static text or webview link
- **Open Source Licenses** — third-party library attribution
- **Rate the App** — links to Google Play listing
- **Share the App** — Android share intent
