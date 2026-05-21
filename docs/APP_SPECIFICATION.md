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

The tuner ships with the following presets, organized by string count and category. Notes are listed **from lowest string to highest** — the order in which the tuner cycles through them in sequential mode.

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
- Drop A#/Bb: A#1, F2, A#2, D#3, G3, C4
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
- Drop F#: F#1, C#2, F#2, B2, E3, G#3, C#4

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
- Drop C# (8-string): C#1, G#1, C#2, F#2, B2, E3, G#3, C#4
- Drop C (8-string): C1, G1, C2, F2, A#2, D#3, G3, C4

> **Implementation note.** Each preset is identified by a stable string ID (e.g., `six_string_standard_e`) so presets can be referenced from saved state and analytics without depending on display names. Presets are hardcoded constants for the initial release. User-defined presets are out of scope until a future phase.

> **Low-fundamental caveat.** Tunings with a lowest string at or below C1 (≈32.7 Hz) — currently `Drop C (8-string)` — push the boundary of what microphone-based pitch detection can reliably resolve on a phone. Fundamental energy is weak at this pitch and harmonic confusion is more likely. The preset ships anyway; if detection proves unreliable on real hardware during Phase 5.2, document the limitation rather than removing the preset.

### Tuning Flow

The tuner supports two operating modes, both available within the same session:

**Sequential mode** (default when a tuning is first selected):

1. The user selects a tuning from the categorized list.
2. The tuner targets the **lowest string first** and advances string by string upward.
3. The device microphone listens for audio input continuously.
4. The app detects the fundamental frequency of the incoming audio using a pitch-detection algorithm (YIN; see Phase 5 plan).
5. The detected frequency is converted into a **cents offset** from the target frequency (`cents = 1200 × log2(f_detected / f_target)`), and the result is displayed as flat / in tune / sharp via the needle gauge per `DESIGN.md` §8.1.
6. When the cents offset stays within **±5 cents for at least 500 ms** of continuous detection, the tuner advances to the next string.
7. The user can also tap any string pill in the selector to jump to it out of order at any time.

**Chromatic mode** (also referred to as "free mode"):

8. After all strings have been tuned once — or at any time, via a mode toggle — the user can switch to chromatic mode.
9. In chromatic mode, the app continuously detects the played pitch and automatically identifies which string of the *currently selected tuning* is closest to the detected note. The needle then shows the cents offset from that closest target.
10. This supports fine-tuning out of order: the user can pluck whichever string they want to check, and the tuner figures out which target to compare against.
11. The mode toggle is exposed in the UI; the specific placement is decided in Phase 5.4.

After the final string is brought in tune in sequential mode, the success state from `DESIGN.md` §8.1 plays once, and the tuner automatically switches into chromatic mode so the user can fine-tune without further interaction.

### Chromatic Mode

Chromatic mode (also called "free mode") is a secondary operating mode of the tuner. It runs alongside the sequential preset-string mode and is the default state after all strings have been tuned once in a session.

**Entry conditions:**
- Automatically entered after the last string satisfies the sustained-tune condition in sequential mode (after a 1.2 s success hold per `DESIGN.md` §8.1).
- Explicitly entered by the user via the mode toggle in the UI (Phase 5.4).

**Behavior in chromatic mode:**
- The tuner does not target a specific string from the preset. Instead, for each audio buffer, the nearest equal-tempered note to the detected fundamental frequency is computed via `MusicTheory.frequencyToNote(detectedHz, referencePitchHz)`.
- The needle shows the cents offset between the detected frequency and that nearest note's target frequency.
- If `frequencyToNote` returns `null` (frequency out of musical range or no fundamental detected), the frame is skipped and the display stays on the previous state.
- No auto-advance. The user plays whichever string they wish; the tuner automatically determines the target each frame.
- `tunedStringIndices` is cleared when chromatic mode is entered.
- `currentStringIndex` is set to 0 but is meaningless in this mode; the UI should not highlight a specific string pill.

**Exit conditions:**
- Tapping any preset in the picker (including the current one) re-arms `PRESET` mode at string 0.
- Tapping a specific string pill re-arms `PRESET` mode at that string's index.

**Implementation note.** The chromatic target is resolved inside `DetectTunedStringUseCase` when `TunerInput.mode == TunerMode.CHROMATIC`. The ViewModel supplies a null `targetNote` for chromatic inputs; the use case fills it per-frame. This is new behavior introduced in Phase 5.3 and is not reflected in the Phase 2 backend outline.

---

### Operating Modes

The tuner operates in one of two modes within a session.

**Preset mode** is the default when a tuning is selected. The tuner targets the lowest string of the chosen preset and advances through the strings in order. The current string can be jumped to by tapping its pill in the string selector; auto-advance then continues from the tapped string forward.

**Chromatic mode** can be entered explicitly via the mode selector on the preset chip, or automatically after all strings of the current preset have been confirmed in tune. In chromatic mode the tuner does not target a specific string — it identifies the nearest equal-tempered note to whatever pitch is detected and shows cents-off relative to that note. Auto-advance does not apply. The user returns to preset mode either by tapping the "Preset" item in the mode selector (which restores the string they were on before entering chromatic mode, if the entry was user-initiated; otherwise resets to the lowest string) or by tapping any string in the string selector (which jumps directly to that string).

---

### Reference Pitch & Tolerance

- **Reference pitch:** A4 = **440 Hz** by default. The user can optionally select A4 = 432 Hz from the tuner settings sheet (opened via the sun-icon button in the top-right of the screen; see `DESIGN.md` §14 Q1 — resolved).
- **In-tune tolerance:** **±5 cents**. To advance to the next string in sequential mode, the pitch must remain within tolerance for **at least 500 ms** of continuous detection (to avoid spurious advances from transients).
- **Frequency-to-cents conversion** is the standard formula: `cents = 1200 × log2(f_detected / f_target)`. Cents are used (not raw Hz) because the human perception of "in tune" is scale-invariant: ±5 cents feels equally tight at E2 (~82 Hz) and at E4 (~330 Hz), where the corresponding Hz tolerances are very different.

### Permissions

- Requires `RECORD_AUDIO` permission (must be requested at runtime via the Activity Result API).
- If `RECORD_AUDIO` permission is not granted when the tuner is opened, the readout area is replaced with a permission-explainer card and a **"Grant access"** button. The button triggers the system permission prompt on first use; after a permanent denial, it opens the system app-settings screen instead. The card's body copy stays consistent across both states.
- If permission is denied permanently, the **"Grant access"** button opens the system app settings (via `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` intent). The string selector and preset chip remain browsable while the card is shown.

### Technical Notes

- Frequency for any equal-tempered semitone offset `n` from A4: `f = referencePitchHz × 2^(n / 12)`.
- Pitch detection runs on a background coroutine; results are posted to UI via `StateFlow`.
- Target accuracy: detect pitch within ±1 cent under normal playing conditions on the open strings of a standard guitar.
- Audio capture parameters (sample rate, buffer size, mono / stereo) are confirmed in Phase 5.2 and recorded in `DECISIONS.md`. The plan-of-record baseline is 44.1 kHz, mono, 16-bit PCM, with a buffer size derived from `AudioRecord.getMinBufferSize()` (floor 2048 frames).

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
