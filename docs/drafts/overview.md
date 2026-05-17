# Toniqo
This project is about an Android-App for guitarists. The App targets experienced guitar players
and is meant as an everyday companion. It is not a beginners-learns-his-first-chords-App. 

The App shall help a guitar player with his daily tasks: 
- Tuning his guitar 
- Practicing with a metronome 
- Finding Keys 
- Finding Chords for Keys 
- probably more in the future

## Modules

The App is structured in modules. Each described task is a module:
- Guitar-tuner
- Metronome
- Keyfinder
- Chordfinder

The user can select the different modules in the App. There is always only one module active.

### Guitar-tuner

A tuner for a guitar with predefined tunings. The user may select a tuning from a list of tunings. The tunings will be categorized.

For example:
- 6-String
  - Standard
    - E
    - G
    - etc
  - Open
    - Open G
    - Open D 
    - DADGAD
    - etc
  - Dropped
    - Dropped D
    - Dropped C
    - Dropped B
    - etc
- 7-String
    - Standard
        - [...]
    - Open
        - [...]
    - Dropped
        - [...]
- 8-String
  - [...]

The tuner module must be able to use the phones microphone to listen to a string played on a guitar.

TODO: This part is guessed and needs improvement
The App needs to translate a tone to its frequency. The frequency is then compared to the frequency of the target tone.
During tuning, the target tune is the tone in the tuning starting from the lowest string (E in standard tuning). 
The user should get visual feedback on whether his current tone if flat or sharp compared to the target tone. After the
frequencies match, the next target tone is selected.

### Metronome

The metronome is a tool, that can produces tones (or clicks) in a predefined time-signature. The user should be able to
work with three parameters:
- BPM (Beats per minute from 1 to 300)
- Time signature. 4/4 3/4 8/8 
- Beat value. wholes, halves, quarters, ..., triads

There needs to be a start and a stop button for the user to interact with.

### Keyfinder

The key finder is a tool that finds the key of a song by its notes. The user may input a set of notes.
EG: A, A#, C, E and a starting/target note
The App then matches these notes to all known modes in western music and produces an output about what 
modes fit the notes and what have the highest probability including the starting/target note.
For example:
Userinput: C, D, E, F Starting/Target note: A
Answer: Probably a minor, but could also be C major

### Chordfinder

TheChordfinder is a tool that finds matching Chords for a given mode. The user selects a musical mode and
the App shows matching Chords.
For example: User selects C-Major
App answers with: C, A minor, F, etc.

## Implementation notes

The implementation should be done in phases:

### Phase-1

General setup. Working unit tests. App does compile. A set of default libraries is installed. 

### Phase-2

General backend outline of the App. No functional implementation. Modules should be defined, packages
should be configured, classes with empty methods created that illustrate the general structure of the
App.

### Phase-3

General frontend outline of the App. No functional implementation of any module, just placeholders of
each modules view, a navigation/menu, an info section that will be filled with stuff like legal docs,
help articles, links to rate and share the app.

### Phase-4

This phase is the actual implementation of the first Module: Guitar-Tuner. The Phase will be devided 
in sub-phases (Phase-4.1, Phase4.2) and so on. This is not part of the first draft and will be done later.