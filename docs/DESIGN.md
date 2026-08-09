# Toniqo — Design Tokens & Handoff (DESIGN.md)

> **Read this first (agent preamble).**
>
> This document is the visual contract for Toniqo. It is authoritative for **appearance and dimension**. `APP_SPECIFICATION.md` is authoritative for **behavior**. Where the two conflict, raise the conflict — do not silently choose.
>
> **Reading order:** §1 brand → §2 color tokens → §3 typography → §4 spacing → §5 radii → §6 components → §7 iconography → §8 module-specific specs → §9 motion → §10 surfaces → §11 Compose stub → §12 don'ts → §13 accessibility → §14 still-open questions.
>
> **Implementation order:** build the token layer (§2–§5) once in `ui/theme/ToniqoTheme.kt` before any screen work. Then primitive components (§6). Then screens (§8).
>
> **The design north star, when in doubt:** *"The signal is the only color that earns it; everything else gets out of the way."* If a visual choice doesn't earn its presence, remove it.
>
> **Items the agent must stop and ask about** (not yet decided in this doc):
> - Tuner idle/empty state layout (§8.1 "Idle state")
> - The 432 Hz toggle's UI placement (§14, Q1)
> - Info section screen designs (§8.5)
> - Permission-denied state for the microphone (§14, Q2)

**Scope.** Implementation reference for the Toniqo visual design. Locks every visual and dimensional decision so the build matches the design exactly. Use it alongside `APP_SPECIFICATION.md` (functional behaviour) — this file only covers *appearance*.

**Framework.** Material 3 on Android (**minSdk 31 / Android 12**), Jetpack Compose. We override Material's color scheme and typography rather than inherit defaults — the brand should read as "studio gear", not stock Material.

**Theme priority.** Dark is the design target. Light is a faithful fallback only.

---

## 1. Brand

### Wordmark and monogram
- Master files: `brand/toniqo-monogram.svg` (256×256), `brand/toniqo-wordmark.svg`.
- **App icon** uses the monogram on the icon chassis colour `#28302E`. Adaptive icon: monogram on the dark layer, mint dot on its own foreground layer.
- **Wordmark** is Space Grotesk SemiBold, `letter-spacing: -0.045em`, with a mint period sized roughly 18% of cap height, baseline-aligned and offset about 3% of cap height to the right of the final "o". Outline the text before shipping anywhere the font isn't guaranteed.
- **Pronunciation** (for voice copy and any future about-screen prose): *ton-EE-koh*. The mint dot reads as the terminal beat.

### Brand color slots
Brand uses **three** slots — chassis, foreground, signal. Everything else is UI tokens (§2).

| Slot           | Dark      | Light                              | Notes                                                                            |
|----------------|-----------|------------------------------------|----------------------------------------------------------------------------------|
| `brand/chassis`| `#28302E` | `#EEF1EF`                          | App icon background only. Distinct from the screen background `bg.base`.         |
| `brand/fg`     | `#F3F6F5` | `#1C2422`                          | Wordmark and monogram strokes.                                                   |
| `brand/signal` | `#9CFF8B` | `oklch(0.62 0.18 140)` ≈ `#37A85F` | Mint lock. Appears once per screen, sparingly. Used in app as `signal.mint`.     |

> **Note on chassis vs. screen base.** `brand/chassis` (`#28302E`) is *only* for the app icon and any external brand surface (splash, store listing). The actual screen background inside the app is the slightly darker `bg.base` (`#1A1F22`) — see §2.1. Do not use `brand/chassis` for screen surfaces.

---

## 2. Color palette

All values are **OKLCH**, with hex equivalents for code. The cool slate hue is **220°**; the mint signal hue is **140°**. **All neutral chroma stays ≤ 0.006** — only semantic states carry colour.

> **Source of truth for code:** use the **hex values** in the tables below. The OKLCH values are documentation. Compose 1.6+ does support OKLCH, but pinning hex keeps colour reproduction deterministic across Compose versions and toolchain updates.

### 2.1 Dark theme (primary)

#### Surface tokens
| Token         | OKLCH                   | Hex       | Use                                         |
|---------------|-------------------------|-----------|---------------------------------------------|
| `bg.base`     | `oklch(0.16 0.005 220)` | `#1A1F22` | Chassis / screen root                       |
| `bg.elev1`    | `oklch(0.20 0.005 220)` | `#222729` | Cards, list rows                            |
| `bg.elev2`    | `oklch(0.235 0.006 220)`| `#292E30` | Segments, chips, secondary buttons          |
| `bg.elev3`    | `oklch(0.27 0.006 220)` | `#30353A` | Hover / pressed, selected items             |
| `bg.inset`    | `oklch(0.13 0.005 220)` | `#161A1C` | Readout wells (tuner, BPM)                  |
| `line.faint`  | `oklch(0.27 0.006 220)` | `#30353A` | Card borders at rest                        |
| `line`        | `oklch(0.33 0.006 220)` | `#3F4548` | Default border                              |
| `line.strong` | `oklch(0.45 0.006 220)` | `#585F62` | Active border, slider track outer           |

#### Text tokens
| Token            | OKLCH                  | Hex       | Use                       |
|------------------|------------------------|-----------|---------------------------|
| `fg.primary`     | `oklch(0.96 0.004 220)`| `#F2F4F5` | Body, hero numerals       |
| `fg.secondary`   | `oklch(0.72 0.005 220)`| `#A8AFB2` | Sub-labels                |
| `fg.tertiary`    | `oklch(0.52 0.006 220)`| `#7A8084` | Captions, kickers         |
| `fg.quaternary`  | `oklch(0.40 0.006 220)`| `#5E6468` | Disabled, hint text, idle tuner state |

#### Signal tokens — single chroma family, hue is the variable
| Token            | OKLCH                  | Hex       | Use                                              |
|------------------|------------------------|-----------|--------------------------------------------------|
| `signal.mint`    | (use exact hex)        | `#9CFF8B` | In tune, primary action, "locked", success      |
| `signal.cyan`    | `oklch(0.78 0.13 220)` | `#73B8E0` | Flat (cents < −5), minor chord quality          |
| `signal.amber`   | `oklch(0.80 0.14 60)`  | `#E1B065` | Sharp (cents > +5), diminished chord quality    |
| `signal.violet`  | `oklch(0.72 0.13 290)` | `#B6A0E0` | Augmented chord quality                         |

### 2.2 Light theme (fallback)

Same token names; values shift.

| Token          | OKLCH                  | Hex       |
|----------------|------------------------|-----------|
| `bg.base`      | `oklch(0.985 0.003 220)`| `#F8F9FA`|
| `bg.elev1`     | `oklch(0.97 0.004 220)` | `#F2F4F5`|
| `bg.elev2`     | `oklch(0.945 0.005 220)`| `#EAEDEE`|
| `bg.elev3`     | `oklch(0.92 0.005 220)` | `#E1E5E7`|
| `bg.inset`     | `oklch(0.99 0.002 220)` | `#FAFBFC`|
| `line.faint`   | `oklch(0.92 0.005 220)` | `#E1E5E7`|
| `line`         | `oklch(0.86 0.005 220)` | `#CDD2D5`|
| `line.strong`  | `oklch(0.70 0.005 220)` | `#A5ABAE`|
| `fg.primary`   | `oklch(0.20 0.005 220)` | `#262C2F`|
| `fg.secondary` | `oklch(0.42 0.006 220)` | `#5E6468`|
| `fg.tertiary`  | `oklch(0.58 0.006 220)` | `#888E92`|
| `fg.quaternary`| `oklch(0.70 0.006 220)` | `#A5ABAE`|
| `signal.mint`  | `oklch(0.62 0.18 140)`  | `#37A85F`|
| `signal.cyan`  | `oklch(0.55 0.16 220)`  | `#3A86C9`|
| `signal.amber` | `oklch(0.62 0.16 60)`   | `#B68038`|
| `signal.violet`| `oklch(0.55 0.16 290)`  | `#7A5AE0`|

### 2.3 Material You / dynamic color
**Off.** Use the static palette above. The brand is the brand. Dynamic colour can ship later as an explicit toggle; not for v1.

### 2.4 Semantic colour mappings

These are the canonical rules for "which signal hue means what." Implement them once in a small mapping function (`ChordQuality.toSignalColor()`, `TuningStatus.toSignalColor()`) and reuse it; never hardcode the colour at the call site.

**Tuner — cents to colour (single source of truth):**
```
abs(cents) ≤ 5    → signal.mint        ("In tune")
cents < −5        → signal.cyan        ("Flat")
cents > +5        → signal.amber       ("Sharp")
idle (no signal)  → fg.quaternary      ("Listening")
```

**Chord Finder — quality to colour:**
```
Major (and major 7th)        → signal.mint
Minor (and minor 7th, m7♭5)  → signal.cyan
Diminished                   → signal.amber
Augmented                    → signal.violet
```

---

## 3. Typography

Two families. Both Google Fonts; bundle locally as Compose `FontFamily` assets — do **not** depend on Play Services Downloadable Fonts.

| Family             | Use                                                              | Weights bundled |
|--------------------|------------------------------------------------------------------|-----------------|
| **Space Grotesk**  | UI chrome, headers, labels, buttons                              | 400, 500, 600, 700 |
| **JetBrains Mono** | Numerals, frequencies, scale degrees, kickers, monospace anywhere| 300, 400, 500   |

`font-feature-settings` everywhere mono is used: **`"tnum", "ss01"`**.

### Type scale

| Token           | Family          | Size (sp/dp — see note) | Line | Weight | Tracking      | Use                                  |
|-----------------|-----------------|-------------------------|------|--------|---------------|--------------------------------------|
| `display.xl`    | JetBrains Mono  | **96 dp**               | 96   | 300    | −0.05 em      | Metronome BPM                        |
| `display.l`     | JetBrains Mono  | **64 dp**               | 64   | 300    | −0.04 em      | Tuner detected note                  |
| `display.s`     | Space Grotesk   | 32 sp                   | 36   | 600    | −0.018 em     | Brand artboard headlines             |
| `h1`            | Space Grotesk   | 22 sp                   | 28   | 600    | −0.023 em     | Screen titles                        |
| `h2`            | Space Grotesk   | 17 sp                   | 22   | 600    | −0.018 em     | Card headers, chord names            |
| `body`          | Space Grotesk   | 14 sp                   | 20   | 400    | −0.005 em     | Default body                         |
| `body.strong`   | Space Grotesk   | 14 sp                   | 20   | 500    | −0.005 em     | Active rows                          |
| `caption`       | Space Grotesk   | 11 sp                   | 14   | 500    | 0             | Secondary labels                     |
| `kicker`        | JetBrains Mono  | 10 sp                   | 14   | 500    | +0.16 em      | All-caps section kickers             |
| `kicker.s`      | JetBrains Mono  | 9.5 sp                  | 12   | 500    | +0.14 em      | Card kickers, list headers           |
| `numeric.m`     | JetBrains Mono  | 15 sp                   | 18   | 500    | −0.015 em     | String pills, BPM input              |
| `numeric.s`     | JetBrains Mono  | 12 sp                   | 16   | 400    | −0.003 em     | Detected / target frequencies        |
| `mono.micro`    | JetBrains Mono  | 9 sp                    | 12   | 400    | +0.05 em      | Smallest readout labels              |

All kicker and `mono.micro` tokens are **uppercase**.

> **Tracking values are in `em`.** Compose lets you express letter spacing as either `.sp` (absolute) or `.em` (proportional to font size). We use `.em` here so that if a token's font size changes, tracking scales correctly without re-doing the math.

> **`dp` vs. `sp` for display tokens.** `display.xl` and `display.l` are **layout-critical readouts** (the BPM numeral, the tuner note letter) that anchor their containing cards. They use `dp` so they do **not** scale with the user's font-size accessibility setting. All other tokens use `sp` and scale normally. This is a deliberate accessibility trade — see §13.

---

## 4. Spacing scale

8-pt base, with a 4-pt half-step. **Do not introduce intermediate values.**

| Token | dp |
|-------|----|
| `sp.0` | 0  |
| `sp.1` | 4  |
| `sp.2` | 8  |
| `sp.3` | 12 |
| `sp.4` | 16 |
| `sp.5` | 20 |
| `sp.6` | 24 |
| `sp.8` | 32 |
| `sp.10`| 40 |
| `sp.12`| 48 |

### Layout grid
- Screen horizontal padding: **`sp.5` (20dp)**.
- Vertical rhythm between cards within a screen: **`sp.4` (16dp)** by default; **`sp.5` (20dp)** for the readout-to-string-selector gap on the Tuner.
- Card internal padding: **`sp.4` (16dp)** for list rows, **`sp.5` (20dp)** for hero readouts.
- Inter-element gap in horizontal rows (chips, buttons): **`sp.2` (8dp)**.

---

## 5. Border radii

| Token    | dp  | Use                              |
|----------|-----|----------------------------------|
| `r.xs`   | 4   | Inline note tags (chord notes)   |
| `r.sm`   | 8   | Chips, segment children          |
| `r.md`   | 12  | List rows, secondary buttons     |
| `r.lg`   | 16  | Cards                            |
| `r.xl`   | 18  | Readout wells                    |
| `r.pill` | 999 | Pill buttons, primary action, segmented control track |

Phone screen mask radius (for design previews only, not the app): 30dp.

---

## 6. Components

Dimensions are **dp**. Where two values appear, the lower is dense / minimum and the higher is the default.

### 6.1 Buttons

| Variant         | Height | H-pad  | Radius | BG                 | FG                      | Border           |
|-----------------|--------|--------|--------|--------------------|-------------------------|------------------|
| `btn.primary`   | 40 / 52| 16 / 22| pill   | `signal.mint`      | `bg.base` (dark text)   | mint @ 50% alpha |
| `btn.default`   | 40     | 16     | pill   | `bg.elev2`         | `fg.primary`            | `line.faint` 1dp |
| `btn.ghost`     | 30     | 12     | pill   | transparent        | `fg.secondary`          | `line.faint` 1dp |
| `btn.icon-round`| 36     | n/a    | 50%    | `bg.elev2`         | `fg.secondary`          | `line.faint` 1dp |

The primary button at its 52dp variant (Metronome Start, when stopped) gets a 24dp mint glow at ~30% mint alpha. This is one of two specific glows allowed in the otherwise flat design language (see §10).

Minimum tap target: **44×44dp** — applies to every interactive element regardless of visual size.

### 6.2 Chips

- Height **26dp**, h-padding **10dp**, radius `pill`.
- Type: `kicker.s` (JetBrains Mono 9.5sp, +0.14em tracking, uppercase).
- Active: BG `signal.mint`, FG `bg.base`, border mint @ 40% alpha.
- Inactive: BG `bg.elev2`, FG `fg.secondary`, border `line.faint`.

### 6.3 Note pills (Tuner string selector, Key Finder notes)

- 38×38dp, radius `r.md` (12dp).
- Active border: `signal.mint` (or current semantic colour); plus a 3dp halo at 12% alpha of the semantic colour.

### 6.4 Bottom navigation — `icon-label` style

The shipped design is `icon-label`. Earlier exploration sketches (icon-only, segmented) are future variants and **not part of v1**.

- Total height including gesture pill: **48dp** content + **18dp** gesture pill.
- Top border on the rail: `line.faint` 1dp.
- Active colour: `signal.mint`. Inactive: `fg.tertiary`.
- Icon **20dp** + label `mono.micro` 9.5sp uppercase, with a 3dp gap between icon and label.
- Active indicator: an 18×2dp mint pill at the top of the active cell, with a 6dp mint glow. This is the second of the two specific glows in the design language.

### 6.5 Status bar (in-app, behind the system bar)
- Height 32dp, h-padding 18dp.
- Clock: Space Grotesk 13sp / 500, tabular nums. Status icons: 15dp signal, 14dp wifi, 22dp battery.

### 6.6 Cards
- Background `bg.elev1`, border `line.faint` 1dp, radius `r.lg` (16dp), padding `sp.4` (16dp).
- Readout wells: background `bg.inset`, radius `r.xl` (18dp), padding 20dp / 12dp.

### 6.7 Sliders (BPM)
- Track height **4dp**, full-width inside the card.
- Track background: `line.faint`.
- Fill: `signal.mint`. (No fill glow — see §10 surface rules.)
- Thumb: 12dp circle, `fg.primary` fill, `line.strong` 1dp border, with a soft drop shadow (Compose `Modifier.shadow(2.dp)` with 40% black at most).

### 6.8 Toggles and segmented controls
- Pill track padding 3dp. Active segment: radius 999, BG `bg.elev3`, FG `fg.primary`, border `line` 1dp. Inactive FG `fg.tertiary`.

### 6.9 Fretboard Diagram

Stateless `Canvas` composable. Accepts a `FretboardRenderModel`; never a raw domain `Voicing`.

#### Anatomy

```
[ pos-label area  ][ side-pad ][ --- string 0 --- ... --- string N-1 --- ][ side-pad ]
                              ^                                           ^
                           gridLeft                                    gridRight
```

- **Marker area** (top band, above fret row 1): 20dp height; ○ / × symbols drawn here.
- **Fret grid**: `fretWindow × fretSpacing` (5 rows × 26dp = 130dp).
- **Position-label area**: 20dp wide strip left of the grid; holds `"Xfr"` text (MonoMicro 9sp, `fg.tertiary`) when the voicing is not at the nut. Empty when nut is shown.
- **Side padding**: 10dp on each side of the grid (inside the total width).
- **String spacing**: 18dp between adjacent strings.

#### Total dimensions

| String count | Width  | Height  |
|--------------|--------|---------|
| 6            | 130 dp | 150 dp  |
| 7            | 148 dp | 150 dp  |
| 8            | 166 dp | 150 dp  |

Width formula: `posLabelWidth(20) + sidePad(10) + (stringCount−1)×stringSpacing(18) + sidePad(10)`

#### Visual layers (bottom → top)

1. **String lines** — `line` colour, 1dp stroke, full height.
2. **Fret lines** — `line` colour, 1dp stroke, full width. Row 0 is the nut (4dp, `fg.secondary`, round caps) when `showNut=true`; otherwise a regular 1dp line.
3. **Position label** — MonoMicro 9sp `fg.tertiary`, centred in the pos-label area at row 1 centre-height. Omitted when `showNut=true`.
4. **Barre** — Rounded rect (`r.pill` corner radius), `bg.elev3` fill, spanning `fromString`..`toString` at the barre fret's centre height. Drawn before dots so dots appear on top.
5. **Finger dots** — Filled circles, radius 9dp.
   - Root strings: fill `signal.mint`.
   - Other strings: fill `bg.elev3`.
6. **Finger numerals** — MonoMicro 9sp inside each dot; `bg.base` text on mint, `fg.primary` text on neutral. Omitted when `finger == null`.
7. **Open (○) markers** — Circle outline, radius 7dp, 1.5dp stroke, `fg.secondary`, drawn in the marker area.
8. **Muted (×) markers** — Two crossing lines, ±65% of marker radius, 1.5dp stroke, `fg.secondary`, `StrokeCap.Round`, drawn in the marker area.

#### Notes

- CAGED shape names **must not appear** anywhere on the voicings screen or in diagram labels. This is a hard product decision (Phase 8.5).
- The component is entirely stateless and carries no animation. Tapping a diagram card navigates to an audio-playback future feature (FP-4); do not add tap handling now.
- For Compose previews and tests, construct `Voicing` directly via its data-class constructor (bypassing `Voicing.validated`) to avoid chord-validation setup.

---

## 7. Iconography

- **Custom hairline outline set.** 24×24 grid, **1.25dp stroke**, round caps and joins.
- **Default state:** outline only.
- **Filled** glyphs reserved for: active nav item, success indicator, play/pause primary control.
- Sizes used in app: **14, 18, 20, 22, 28** dp. Don't scale beyond ±20% of these.
- Colour: inherits `currentColor` from parent. Active states use the parent's semantic colour, not a hard-coded value.

The set ships these names; ask before adding any new ones:
`tuner, metronome, key, chord, more, settings, info, play, pause, plus, minus, check, chevron-right, chevron-down, tap, mic, search`

> **Note (Phase 5.4).** No `sun` glyph ships in v1. The tuner uses the `settings` icon for the top-right button that opens the settings sheet. If a dedicated sun icon is desired in a future release, add a new §14 question rather than introducing a one-off SVG.

---

## 8. Module-specific specs

### 8.1 Tuner

**Readout style.** Ship the **needle gauge** as the v1 readout. Linear strip, radial arc, and dot matrix variants from design exploration are not in v1; do not build them.

**Cents → colour mapping.** See §2.4.

**Needle gauge geometry.**
- Canvas 280×150dp, anchored at the bottom centre, radius 120dp.
- ±50 cents maps to ±60° needle rotation (clamped).
- Sweet-spot arc spans −6° to +6° (±5 cents), rendered in `signal.mint` at 2dp stroke with a 4dp glow when in tune.
- Tick marks every 10 cents; major ticks every 30 cents; centre tick is mint.
- Needle: 2dp stroke, semantic colour, 6dp drop-shadow glow when not idle.
- Pivot cap: 5dp `bg.elev3` circle with 0.8dp `line` border; 2dp inner dot in semantic colour.
- Needle transition: 200ms, `cubic-bezier(0.4, 1.2, 0.5, 1)`. No spring physics — the needle settles, it does not bounce.

**Detected-note hero** (above the gauge): 64dp JetBrains Mono Light note letter, 18dp octave subscript. Both are rendered in `fg.primary` with no glow. The semantic colour is conveyed by the needle and the status line.

**Status line:** kicker-style mono, 11sp, +0.16em tracking, uppercase. Status word and cents value side-by-side.

**String selector** (between the readout well and the bottom nav):
- Row of N pills (one per string), 54dp tall, equal flex, 6dp gap between.
- Already-tuned strings show a 9dp `check` glyph in `signal.mint` at the top-right corner of their pill.
- The current string is outlined in the current semantic colour with a 3dp halo (see §6.3).

**Preset chip row** (above the readout well):
- A category chip on the left, e.g. "6-STRING · DROP" in `mono.micro` kicker style.
- A right-aligned `MIC LIVE` indicator: mint dot plus uppercase mono label.

**Reference pitch chip:** the screen header kicker line displays `TUNER · A4 = 440 HZ` (or `432 HZ`). The toggle to change it is in the **tuner settings sheet**, opened by tapping the `settings`-icon button in the top-right corner of the screen.

**Idle state** (the user has opened the tuner but no audio is detected yet):
- Needle sits at the centre (0 cents position) in `fg.quaternary`.
- Detected-note hero shows a single `—` (em dash) in `fg.quaternary`, no octave subscript.
- Status line reads `LISTENING` in `fg.quaternary`, no cents value.
- `MIC LIVE` indicator shows the mint dot at full saturation; this is what tells the user the mic is working even when no sound is detected.

**Hz readout pair** (inside the readout well, below the needle gauge): two columns side-by-side. Left column: kicker label `"DETECTED"` in `fg.tertiary` and the detected frequency value (e.g. `"108.86 Hz"`) in `Tq.Type.Body` / `fg.primary`. Right column: kicker label `"TARGET"` and the target frequency value (e.g. `"110.00 Hz"`). When the detected value is unavailable, render `"— Hz"` (em-dash) in place of the numeric value. Both labels are always shown even when values are null.

**Settings sheet** (opened via the `settings`-icon button at top-right): a `ModalBottomSheet` approximately 280dp tall containing:
- **Reference pitch** row: a `Tq.Type.Body` label on the left, the current value (`A4 = 440 Hz` or `A4 = 432 Hz`) on the right, and a segmented control (`[ 440 | 432 ]`) below the row.
- **Auto-advance strings** row: a Material 3 `Switch` on the right, `Tq.Type.Body` description below (`"Advance automatically when a string is in tune."`).

**Permission-denied state** (shown when `RECORD_AUDIO` is not granted, replacing the readout well): a `ToniqoCard` (`bg.elev1`, `r.lg`, `sp.4` padding) centred in the well's vertical position. Contents: a 28dp `mic` icon with a diagonal slash overlay (until a dedicated icon exists), an `H2` heading (`"Microphone access needed"`), a `body` description centred (max 3 lines), and a `btn.primary` 40dp variant with the label `"Grant access"`.

**Success state — "all strings tuned":**
- A `signal.mint` border ring fades in around the readout well over 320ms (ease-out), holds for 1.2s, then fades out.
- One tactile pulse via the standard system haptic. No confetti, no celebratory animation.

### 8.2 Metronome

**Page status kicker.** A `mono.micro` kicker line sits above the H1 "Metronome" title.
- Playing: `● METRONOME · RUNNING` — leading mint pulsing dot (~1 s alpha cycle, 100% → 30%).
- Stopped: `METRONOME · STOPPED` — no dot.

**Tempo card.** BPM display, tempo descriptor, slider, and ±1 buttons are grouped inside a single visually-bounded card. Background `bg.inset`, radius `r.xl` (18dp), 1dp `line.faint` border, `sp.5` × `sp.4` padding (matching readout-well spec §6.6). A `mono.micro` kicker "TEMPO" sits above the BPM numeral inside the card.

- BPM range 1–300, default 120. Display BPM at `display.xl` (96dp mono, fixed — does not scale with user font-size setting). Tapping the BPM number opens an inline number-pad dialog.
- Tempo descriptor (Adagio / Andante / Moderato / Allegro / Presto) directly below the BPM in `mono.micro`, derived from BPM ranges. Read-only.
- Slider thumb at the current BPM position, `−` / `+` buttons either side at 36dp.

**Beat indicator header.** A `mono.micro` row sits between the tempo card and the beat segments, split:
- Left: `BEAT · X / N` where X is the 1-indexed current beat (1 when stopped), N is the numerator.
- Right: `QUARTER NOTES` for /4 signatures; `EIGHTH NOTES` for /8 signatures.

**Beat indicator segments.** A row of N segments (one per beat of the signature), each 44dp tall.
- Beat 1 lit: `signal.mint` fill + 12dp concentric glow behind the segment.
- Beats 2..N lit: mint at 35% composited over `bg.elev2`.
- Unlit: `bg.elev1` fill, `line.faint` 1dp border.
- Beat-1 marker: 4dp mint dot centred inside the unlit beat-1 cell.
- Colour transition: 80ms `LinearEasing`. **Intentionally overrides reduced-motion** — this is the primary temporal indicator; disabling it would break usability.

- Time signature and subdivision are pill-style 44dp dropdowns, side-by-side at 1fr each, with a `mono.micro` kicker label above each: "SIGNATURE" and "SUBDIVIDE" (verb form).
- Tap-tempo button: 60dp circle, `bg.elev2`, with `tap` icon plus uppercase "TAP" in `mono.micro`.
- **Start/Stop: pill button, 60dp tall, flex: 1. Icon + text label (revised Item 18).**
  - Stopped: ▶ play icon + "Start" text. Mint primary background with the §6.1 24dp glow.
  - Running: ⏸ pause icon + "Stop" text. `bg.elev3` neutral background, no glow.

### 8.3 Key Finder

- Input area: padded chip rail, `bg.inset` background, `r.md` radius, `min-height: 56dp`.
- Note chip: 30dp tall, kicker-mono type at 12sp. The **tonic note** chip gets a mint-mixed fill (22% mint over `bg.elev2`) plus a `· TONIC` mono suffix in mint.
- "Add note" button: 30×30dp dashed-border circle with a `+` icon.
- Results list: 12dp-padded rows, `r.md`. The **first result** gets a mint-mixed background (6% mint over `bg.elev1`) and a mint-mixed border.
- Each row contains: rank number (mono, 2-digit zero-padded), mode title, badges (`TONIC` mint-outlined, `FULL` neutral-outlined), subtitle in `mono.micro`, and a match-% on the right at `h2` size in mint (for the top match) or `fg.primary` otherwise.

> **Note for the agent:** there is no Key Finder screenshot in the design assets. The above prose is the spec. Before implementing, request a sketch or confirm interpretation with the user.

### 8.4 Chord Finder

- Root and Mode dropdowns side-by-side, 42dp tall. Mode dropdown `flex: 1.4`, Root `flex: 1`.
- Sevenths toggle: pill segmented control with two cells (`TRIADS` / `7THS`).
- Chord row: 12dp padding, `bg.elev1`, `r.md` radius.
- Each row contains:
  - Roman numeral in `h2` mono size, coloured per §2.4 (mint major, cyan minor, amber diminished, violet augmented), with quality abbreviation below.
  - Chord name in `h2` Space Grotesk SemiBold.
  - Note pills as 10sp mono in 4dp-radius mini-tags.

### 8.5 Info / About section

Not yet designed. The Info screens follow the standard card-list pattern from §6.6 — `bg.elev1` cards with `r.lg` radius, `sp.4` padding, separated by `sp.4` vertical rhythm. Each item is a row with leading icon (20dp), title in `body.strong`, and trailing `chevron-right` icon.

The agent should produce the screens to this pattern; if a richer design is needed for any sub-screen (Privacy Policy in particular, since it carries long-form text), stop and ask.

---

## 9. Motion

**Principle: tools, not toys.** No springs, no bounces beyond the tuner-needle settle. No celebratory animations. No streaks.

| Where                          | Duration | Easing                              |
|--------------------------------|----------|-------------------------------------|
| Needle settle                  | 200ms    | `cubic-bezier(0.4, 1.2, 0.5, 1)`    |
| Beat indicator (metronome)     | 80ms     | linear (must feel mechanical)       |
| Nav transition                 | 220ms    | `cubic-bezier(0.2, 0, 0, 1)` (M3 standard) |
| Card press                     | 120ms    | ease-out                            |
| Sheet / dialog enter           | 280ms    | `cubic-bezier(0.2, 0, 0, 1)`        |
| "All strings tuned" success ring | 320ms in + 1200ms hold + 320ms out | ease-out         |

**Reduced motion.** When the system's `Settings.Global.TRANSITION_ANIMATION_SCALE` is 0, or the accessibility "Remove animations" setting is on:
- The **tuner needle** still moves — it conveys state and cannot be removed. Reduce its transition to 80ms linear.
- The **beat indicator** still flashes — it is the entire point of the metronome. Keep at 80ms.
- The **success ring** is replaced with an instant state change: ring appears, holds 1.2s, disappears. No fade.
- All **other** motion (nav transitions, card press, sheet enter) is replaced with an instant state change.

---

## 10. Surfaces

Default is **flat**. Depth comes from hairline borders and elevation tokens, **except** for two specific glows that are part of the design language:

1. The 24dp mint glow on the `btn.primary` 52dp variant when stopped (§6.1).
2. The 6dp mint glow on the active bottom-nav indicator pill (§6.4).

No other glows, no drop shadows on cards, no gradient backgrounds as decoration. The brushed and noise texture variants from design exploration are **not** enabled in v1; they can be added later as a `Modifier.drawBehind { }` overlay without rewriting layouts.

---

## 11. Compose code stub

Drop this in as `ui/theme/ToniqoTheme.kt` to anchor the system. Names mirror the tokens above. Letter spacing uses `.em` so it scales correctly if a token's font size changes.

```kotlin
// ui/theme/ToniqoTheme.kt — sketched. Fill in remaining tokens following §2 and §3.
object Tq {
  object Color {
    val BgBase       = Color(0xFF1A1F22)
    val BgElev1      = Color(0xFF222729)
    val BgElev2      = Color(0xFF292E30)
    val BgElev3      = Color(0xFF30353A)
    val BgInset      = Color(0xFF161A1C)
    val LineFaint    = Color(0xFF30353A)
    val Line         = Color(0xFF3F4548)
    val LineStrong   = Color(0xFF585F62)
    val FgPrimary    = Color(0xFFF2F4F5)
    val FgSecondary  = Color(0xFFA8AFB2)
    val FgTertiary   = Color(0xFF7A8084)
    val FgQuaternary = Color(0xFF5E6468)
    val SignalMint   = Color(0xFF9CFF8B)
    val SignalCyan   = Color(0xFF73B8E0)
    val SignalAmber  = Color(0xFFE1B065)
    val SignalViolet = Color(0xFFB6A0E0)
  }
  object Sp {  // dp
    val s1 = 4.dp;  val s2 = 8.dp;  val s3 = 12.dp; val s4 = 16.dp
    val s5 = 20.dp; val s6 = 24.dp; val s8 = 32.dp; val s10 = 40.dp; val s12 = 48.dp
  }
  object Radius {
    val Xs = 4.dp;  val Sm = 8.dp;  val Md = 12.dp
    val Lg = 16.dp; val Xl = 18.dp; val Pill = 999.dp
  }
  object Type {
    private val Grotesk = FontFamily(/* Space Grotesk 400/500/600/700 */)
    private val Mono    = FontFamily(/* JetBrains Mono 300/400/500 */)

    // display.xl and display.l use dp-equivalent for the readouts (do not scale with user font size).
    // All others use sp.
    val DisplayXl  = TextStyle(fontFamily = Mono,    fontSize = 96.sp, fontWeight = FontWeight.Light,    letterSpacing = (-0.05).em)
    val DisplayL   = TextStyle(fontFamily = Mono,    fontSize = 64.sp, fontWeight = FontWeight.Light,    letterSpacing = (-0.04).em)
    val H1         = TextStyle(fontFamily = Grotesk, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.023).em)
    val H2         = TextStyle(fontFamily = Grotesk, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.018).em)
    val Body       = TextStyle(fontFamily = Grotesk, fontSize = 14.sp, fontWeight = FontWeight.Normal,   letterSpacing = (-0.005).em)
    val Kicker     = TextStyle(fontFamily = Mono,    fontSize = 10.sp, fontWeight = FontWeight.Medium,   letterSpacing = 0.16.em)
    val NumericM   = TextStyle(fontFamily = Mono,    fontSize = 15.sp, fontWeight = FontWeight.Medium,   letterSpacing = (-0.015).em)
    // Add the rest from §3.
  }
}
```

> **Important:** `DisplayXl` and `DisplayL` are declared in `sp` for source consistency, but the composables that consume them must apply them inside a `CompositionLocalProvider(LocalDensity provides ...)` block that disables font scaling, or wrap them with `withStyle` that pins font size. See §13 for the rationale and the helper composable to use.

---

## 12. Don'ts

- **No emoji.** Anywhere. The 1.25dp icon set covers everything.
- **No gradient backgrounds** as decoration. The only gradient that exists is in design previews — never in the app.
- **No drop shadows on cards.** Hairline borders and elevation tokens carry depth.
- **No SVG illustrations.** If you need one, ask for a placeholder; don't generate one.
- **No saturated neutrals.** Anything that isn't a semantic state stays at chroma ≤ 0.006.
- **No corner radii outside the scale.** Especially not 12.5dp, 13dp, etc.
- **No type sizes outside the scale.** Especially not 13sp body, 15sp captions.
- **No streaks, XP, badges, confetti, or celebratory animations.** This is a tool.
- **No spring physics on the tuner needle.** It is a precision instrument — it settles, it does not bounce.
- **No Material You dynamic colour in v1.** Static palette is the brand.
- **No glows other than the two listed in §10.**

---

## 13. Accessibility

### 13.1 Font scaling

Most text uses `sp`, which scales with the user's system font-size setting up to 200%. The two display readouts (`display.xl` for the BPM, `display.l` for the tuner note letter) are layout-critical anchors — if they scaled to 200%, they would overflow their cards and break the readout layout.

**Decision:** `display.xl` and `display.l` are pinned to their visual size regardless of the user's font-scale setting. All other text scales normally.

Implementation pattern (helper composable to define in `ui/components/`):

```kotlin
@Composable
fun NonScalingText(text: String, style: TextStyle, modifier: Modifier = Modifier) {
    val baseDensity = LocalDensity.current
    val pinnedDensity = Density(density = baseDensity.density, fontScale = 1f)
    CompositionLocalProvider(LocalDensity provides pinnedDensity) {
        Text(text = text, style = style, modifier = modifier)
    }
}
```

Use `NonScalingText` for the BPM numeral and the tuner detected-note letter. All other text uses normal `Text(...)`.

### 13.2 Reduced motion

See §9.

### 13.3 Contrast

The dark palette has been chosen for >= 7:1 contrast on `fg.primary` over `bg.base`, and >= 4.5:1 for `fg.secondary` and `fg.tertiary` over the same surface. The signal colours (mint, cyan, amber, violet) all clear 4.5:1 over `bg.base` and `bg.inset`. Do not use `fg.quaternary` for any text the user must read — it is for hint/disabled states only.

### 13.4 Tap targets

Minimum 44×44dp on every interactive element, regardless of visual size. Already enforced in §6.

---

## 14. Open questions for product

These need answers before the relevant module is built. The agent must stop and ask rather than improvise.

1. **A4 = 432 Hz UI placement.** ~~Where does the user tap to change the reference pitch?~~ **Resolved (2026-05-20):** A `settings`-icon button in the top-right corner of the Tuner screen opens the tuner settings sheet, which contains the 432 Hz toggle alongside the auto-advance toggle.
2. **Permission-denied state for the microphone.** ~~What does the Tuner screen look like when the user has denied `RECORD_AUDIO`?~~ **Resolved (2026-05-20):** A single `ToniqoCard` with a 28dp mic icon (slash-overlaid), an explanatory heading and body, and a primary "Grant access" button. The button requests permission on first tap; after permanent denial, opens system app settings.
3. **Key Finder mockup.** §8.3 is specified in prose but not shown. Confirm interpretation or add a screenshot before Phase 5+.
4. **Info section content.** ~~§8.5 describes the layout but not the specific cards on the Info home screen. Confirm the list (Help, Privacy Policy, Licenses, Rate the App, Share the App, plus any others) before building.~~ **Resolved (2026-07-19):** Initial menu is Help, Open Source Licenses, Support the Project (GitHub Sponsors link). Privacy Policy and Rate/Share deferred — see `DECISIONS.md`.
5. **Light mode trigger.** ~~System default, manual toggle in settings, or both? Default to system; confirm before building the settings sheet.~~ **Resolved (2026-08-09):** Manual toggle only ("Dark Theme" row in the Info menu) — no system-default following. The app always starts dark regardless of the device's system theme; the user must explicitly opt into light mode, and that choice is persisted. See `DECISIONS.md`, "Runtime light/dark theme toggle".
