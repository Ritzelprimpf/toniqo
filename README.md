# Toniqo — the guitar toolkit

An Android companion app for intermediate-to-advanced guitarists. Not a
beginner learning app — a practical, everyday utility that lives on your
phone: tune up, keep time, work out what key you're playing in, and see
every chord that belongs to it, complete with playable fretboard diagrams.

> **Status:** in active development / beta. Feedback from testers is very
> welcome — see [Feedback & bug reports](#feedback--bug-reports) below.

## Features

- **Guitar Tuner** — chromatic and preset-driven tuning (multiple string
  counts and alternate tunings) using your device's microphone.
- **Metronome** — BPM and time-signature driven click track with
  subdivisions and tap tempo.
- **Key Finder** — identifies the musical key(s)/mode(s) that best match a
  set of notes you provide, live-ranked as you add or remove notes.
- **Chord Finder** — lists every diatonic chord for a selected root and
  mode (all 14 scale types, not just the 7 standard modes), and shows
  playable guitar voicings for any chord you tap on.

## Screenshots

<!-- Add real screenshots here once available, e.g.:
![Tuner](docs/screenshots/tuner.png) ![Chord Finder](docs/screenshots/chord-finder.png)
-->

## Tech stack

- **Kotlin**, **Jetpack Compose** (Material 3)
- **Hilt** (via KSP) for dependency injection
- **MVVM** with `StateFlow`-driven UI state
- Feature-first package layout with Clean Architecture (`data` /
  `domain` / `presentation`) inside each feature
- `minSdk 31` (Android 12)
- Unit tests with **JUnit 4** + **MockK**

## Building from source

1. Clone the repository.
2. Open it in Android Studio (a recent stable release).
3. Let Gradle sync — all dependency versions are pinned in
   `gradle/libs.versions.toml`.
4. Run on an Android 12+ (API 31+) emulator or device.

Package name: `de.ritzelprimpf.toniqo`.

## Feedback & bug reports

This project isn't currently looking for code contributions, but **bug
reports and feature requests are genuinely welcome** — that's exactly what
[Issues](../../issues) are for. Found something broken, or have an idea for
a feature? Open an issue; no need to file a pull request.

## Support the project

If Toniqo is useful to you, you're welcome to support its development
through [GitHub Sponsors](https://github.com/sponsors/Ritzelprimpf).
Entirely optional, no perks attached, no strings — Toniqo stays free either
way.

## License

Toniqo is licensed under the [MIT License](LICENSE).

The MIT license covers the source code. It does **not** extend to the
**"Toniqo" name or the app icon/branding** — please don't republish a fork
under the Toniqo name or its icon. Forking, modifying, and learning from the
code is very welcome.
