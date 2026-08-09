package de.ritzelprimpf.toniqo.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * One fully-resolved set of [Tq.Color] values — either [TqPalette.Dark] or [TqPalette.Light].
 * The payload of [LocalTqPalette]; [ToniqoTheme] provides the instance matching the user's
 * current theme choice, and [Tq.Color]'s composable getters read from it.
 *
 * Non-composable functions that need a colour outside of ordinary composable scope (a mapper
 * like `TuningStatus.toSignalColor()`, or a `DrawScope` helper) take a [TqPalette] as an explicit
 * parameter instead of reading [Tq.Color] directly — see [Tq.Palette] for the composable-scope
 * accessor that supplies it.
 */
data class TqPalette(
    val bgBase: ComposeColor,
    val bgElev1: ComposeColor,
    val bgElev2: ComposeColor,
    val bgElev3: ComposeColor,
    val bgInset: ComposeColor,
    val lineFaint: ComposeColor,
    val line: ComposeColor,
    val lineStrong: ComposeColor,
    val fgPrimary: ComposeColor,
    val fgSecondary: ComposeColor,
    val fgTertiary: ComposeColor,
    val fgQuaternary: ComposeColor,
    val signalMint: ComposeColor,
    val signalCyan: ComposeColor,
    val signalAmber: ComposeColor,
    val signalViolet: ComposeColor,
) {
    companion object {
        /** Built from [Tq.DarkColor] — the primary design target (DESIGN.md §2.1). */
        val Dark = TqPalette(
            bgBase = Tq.DarkColor.BgBase,
            bgElev1 = Tq.DarkColor.BgElev1,
            bgElev2 = Tq.DarkColor.BgElev2,
            bgElev3 = Tq.DarkColor.BgElev3,
            bgInset = Tq.DarkColor.BgInset,
            lineFaint = Tq.DarkColor.LineFaint,
            line = Tq.DarkColor.Line,
            lineStrong = Tq.DarkColor.LineStrong,
            fgPrimary = Tq.DarkColor.FgPrimary,
            fgSecondary = Tq.DarkColor.FgSecondary,
            fgTertiary = Tq.DarkColor.FgTertiary,
            fgQuaternary = Tq.DarkColor.FgQuaternary,
            signalMint = Tq.DarkColor.SignalMint,
            signalCyan = Tq.DarkColor.SignalCyan,
            signalAmber = Tq.DarkColor.SignalAmber,
            signalViolet = Tq.DarkColor.SignalViolet,
        )

        /** Built from [Tq.LightColor] — the light-theme fallback (DESIGN.md §2.2). */
        val Light = TqPalette(
            bgBase = Tq.LightColor.BgBase,
            bgElev1 = Tq.LightColor.BgElev1,
            bgElev2 = Tq.LightColor.BgElev2,
            bgElev3 = Tq.LightColor.BgElev3,
            bgInset = Tq.LightColor.BgInset,
            lineFaint = Tq.LightColor.LineFaint,
            line = Tq.LightColor.Line,
            lineStrong = Tq.LightColor.LineStrong,
            fgPrimary = Tq.LightColor.FgPrimary,
            fgSecondary = Tq.LightColor.FgSecondary,
            fgTertiary = Tq.LightColor.FgTertiary,
            fgQuaternary = Tq.LightColor.FgQuaternary,
            signalMint = Tq.LightColor.SignalMint,
            signalCyan = Tq.LightColor.SignalCyan,
            signalAmber = Tq.LightColor.SignalAmber,
            signalViolet = Tq.LightColor.SignalViolet,
        )
    }
}

/**
 * Holds the current [TqPalette]. [ToniqoTheme] is the only provider; every other read goes
 * through [Tq.Color] or [Tq.Palette]. Accessing this before [ToniqoTheme] has run is a
 * programming error — there is no sensible default to fall back to silently.
 */
val LocalTqPalette = staticCompositionLocalOf<TqPalette> {
    error("No TqPalette provided — Tq.Color/Tq.Palette must be read from inside ToniqoTheme { }")
}

/**
 * Toniqo design token object — the single source of truth for every colour,
 * spacing, radius, and type value in the app.
 *
 * No hardcoded values anywhere outside this file. Every colour, font size,
 * spacing, and radius used by the app must come from a property here (or from
 * [MaterialTheme.colorScheme] / [MaterialTheme.typography], which are themselves
 * wired to [Tq] inside [ToniqoTheme]).
 *
 * `Tq` is a pure, stateless top-level object — the documented exception to the
 * no-singletons rule in CLAUDE.md §4. It has no state, no I/O, and no platform
 * dependencies. [Color]'s properties are the one deliberate exception to "stateless": they are
 * `@Composable` getters reading [LocalTqPalette], so the app can switch themes at runtime without
 * every one of Toniqo's ~300 `Tq.Color.*` call sites needing to change (see `DECISIONS.md`,
 * "Runtime light/dark theme toggle"). [DarkColor] and [LightColor] remain plain, stateless raw
 * value objects — they're the actual source of truth [TqPalette.Dark]/[TqPalette.Light] are built
 * from, and are also read directly by [ToniqoTheme] to build the underlying Material `ColorScheme`.
 *
 * ## Namespaces
 *
 * - [Color] — the **theme-reactive** colour tokens; use this everywhere in ordinary composable
 *   scope, exactly as before.
 * - [Palette] — the current [TqPalette] as a value, for the rare non-composable mapper function
 *   that needs a colour outside composable scope (pass it in as an explicit parameter).
 * - [DarkColor] — raw dark-theme colour values (primary target, per DESIGN.md §2.1).
 * - [LightColor] — raw light-theme colour values (fallback, per DESIGN.md §2.2).
 * - [Sp] — spacing scale (per DESIGN.md §4)
 * - [Radius] — border radii (per DESIGN.md §5)
 * - [Type] — full type scale (per DESIGN.md §3)
 */
object Tq {

    /** The current theme's resolved [TqPalette] — see [Color] for the everyday per-token accessor. */
    val Palette: TqPalette
        @Composable get() = LocalTqPalette.current

    /**
     * Theme-reactive colour tokens. Each property reads [LocalTqPalette] under the hood, so it
     * always reflects the user's current dark/light choice — callers use these exactly as they
     * would a plain constant; nothing at the call site changes based on the theme.
     *
     * Do not use [FgQuaternary] for any text the user must read — it is for
     * hint/disabled states only.
     */
    object Color {
        val BgBase: ComposeColor       @Composable get() = LocalTqPalette.current.bgBase
        val BgElev1: ComposeColor      @Composable get() = LocalTqPalette.current.bgElev1
        val BgElev2: ComposeColor      @Composable get() = LocalTqPalette.current.bgElev2
        val BgElev3: ComposeColor      @Composable get() = LocalTqPalette.current.bgElev3
        val BgInset: ComposeColor      @Composable get() = LocalTqPalette.current.bgInset
        val LineFaint: ComposeColor    @Composable get() = LocalTqPalette.current.lineFaint
        val Line: ComposeColor         @Composable get() = LocalTqPalette.current.line
        val LineStrong: ComposeColor   @Composable get() = LocalTqPalette.current.lineStrong
        val FgPrimary: ComposeColor    @Composable get() = LocalTqPalette.current.fgPrimary
        val FgSecondary: ComposeColor  @Composable get() = LocalTqPalette.current.fgSecondary
        val FgTertiary: ComposeColor   @Composable get() = LocalTqPalette.current.fgTertiary
        val FgQuaternary: ComposeColor @Composable get() = LocalTqPalette.current.fgQuaternary
        val SignalMint: ComposeColor   @Composable get() = LocalTqPalette.current.signalMint
        val SignalCyan: ComposeColor   @Composable get() = LocalTqPalette.current.signalCyan
        val SignalAmber: ComposeColor  @Composable get() = LocalTqPalette.current.signalAmber
        val SignalViolet: ComposeColor @Composable get() = LocalTqPalette.current.signalViolet
    }

    // ─── Colours — dark theme (primary) ─────────────────────────────────────
    //
    // Implementation note: the inner object is named `DarkColor`, which would shadow the
    // imported `androidx.compose.ui.graphics.Color` class within this file if it were named
    // `Color` (that name is taken by the reactive accessor above). The import is aliased to
    // `ComposeColor` at the top of this file to preserve access to the Compose Color constructor.

    /**
     * Raw dark-theme colour values. This is the primary design target (DESIGN.md §2.1).
     *
     * Hex values are the source of truth for code; the OKLCH values in DESIGN.md
     * are documentation only. Read this directly only from [ToniqoTheme] (building the Material
     * `ColorScheme`) or [TqPalette.Dark] — everywhere else, use [Color] (theme-reactive) or
     * [Palette] instead.
     */
    object DarkColor {

        // ── Surface tokens ──────────────────────────────────────────────────
        /** `bg.base` — Chassis / screen root. `#1A1F22` */
        val BgBase       = ComposeColor(0xFF1A1F22)
        /** `bg.elev1` — Cards, list rows. `#222729` */
        val BgElev1      = ComposeColor(0xFF222729)
        /** `bg.elev2` — Segments, chips, secondary buttons. `#292E30` */
        val BgElev2      = ComposeColor(0xFF292E30)
        /** `bg.elev3` — Hover/pressed, selected items. `#30353A` */
        val BgElev3      = ComposeColor(0xFF30353A)
        /** `bg.inset` — Readout wells (tuner gauge, BPM display). `#161A1C` */
        val BgInset      = ComposeColor(0xFF161A1C)

        // ── Line tokens ─────────────────────────────────────────────────────
        /** `line.faint` — Card borders at rest. `#30353A` */
        val LineFaint    = ComposeColor(0xFF30353A)
        /** `line` — Default border. `#3F4548` */
        val Line         = ComposeColor(0xFF3F4548)
        /** `line.strong` — Active border, slider track outer. `#585F62` */
        val LineStrong   = ComposeColor(0xFF585F62)

        // ── Text tokens ─────────────────────────────────────────────────────
        /** `fg.primary` — Body copy, hero numerals. `#F2F4F5` */
        val FgPrimary    = ComposeColor(0xFFF2F4F5)
        /** `fg.secondary` — Sub-labels. `#A8AFB2` */
        val FgSecondary  = ComposeColor(0xFFA8AFB2)
        /** `fg.tertiary` — Captions, kickers. `#7A8084` */
        val FgTertiary   = ComposeColor(0xFF7A8084)
        /** `fg.quaternary` — Disabled, hint text, idle tuner needle. Do not use for legible text. `#5E6468` */
        val FgQuaternary = ComposeColor(0xFF5E6468)

        // ── Signal tokens ────────────────────────────────────────────────────
        /** `signal.mint` — In tune, primary action, locked, success. `#9CFF8B` */
        val SignalMint   = ComposeColor(0xFF9CFF8B)
        /** `signal.cyan` — Flat (cents < −5), minor chord quality. `#73B8E0` */
        val SignalCyan   = ComposeColor(0xFF73B8E0)
        /** `signal.amber` — Sharp (cents > +5), diminished chord quality. `#E1B065` */
        val SignalAmber  = ComposeColor(0xFFE1B065)
        /** `signal.violet` — Augmented chord quality. `#B6A0E0` */
        val SignalViolet = ComposeColor(0xFFB6A0E0)
    }

    // ─── Colours — light theme (fallback) ───────────────────────────────────

    /**
     * Raw light-theme colour values. Parallel namespace to [DarkColor]. (DESIGN.md §2.2)
     *
     * Same token names as [DarkColor]; values shift to light-mode equivalents. Read this directly
     * only from [ToniqoTheme] or [TqPalette.Light] — everywhere else, use [Color] or [Palette].
     */
    object LightColor {

        // ── Surface tokens ──────────────────────────────────────────────────
        /** `bg.base` — Screen root — light variant. `#F8F9FA` */
        val BgBase       = ComposeColor(0xFFF8F9FA)
        /** `bg.elev1` — Cards, list rows — light variant. `#F2F4F5` */
        val BgElev1      = ComposeColor(0xFFF2F4F5)
        /** `bg.elev2` — Segments, chips, secondary buttons — light variant. `#EAEDEE` */
        val BgElev2      = ComposeColor(0xFFEAEDEE)
        /** `bg.elev3` — Hover/pressed, selected items — light variant. `#E1E5E7` */
        val BgElev3      = ComposeColor(0xFFE1E5E7)
        /** `bg.inset` — Readout wells — light variant. `#FAFBFC` */
        val BgInset      = ComposeColor(0xFFFAFBFC)

        // ── Line tokens ─────────────────────────────────────────────────────
        /** `line.faint` — Card borders at rest — light variant. `#E1E5E7` */
        val LineFaint    = ComposeColor(0xFFE1E5E7)
        /** `line` — Default border — light variant. `#CDD2D5` */
        val Line         = ComposeColor(0xFFCDD2D5)
        /** `line.strong` — Active border — light variant. `#A5ABAE` */
        val LineStrong   = ComposeColor(0xFFA5ABAE)

        // ── Text tokens ─────────────────────────────────────────────────────
        /** `fg.primary` — Body copy, hero numerals — light variant. `#262C2F` */
        val FgPrimary    = ComposeColor(0xFF262C2F)
        /** `fg.secondary` — Sub-labels — light variant. `#5E6468` */
        val FgSecondary  = ComposeColor(0xFF5E6468)
        /** `fg.tertiary` — Captions, kickers — light variant. `#888E92` */
        val FgTertiary   = ComposeColor(0xFF888E92)
        /** `fg.quaternary` — Disabled, hint text — light variant. Do not use for legible text. `#A5ABAE` */
        val FgQuaternary = ComposeColor(0xFFA5ABAE)

        // ── Signal tokens ────────────────────────────────────────────────────
        /** `signal.mint` — In tune, primary action — light variant. `#37A85F` */
        val SignalMint   = ComposeColor(0xFF37A85F)
        /** `signal.cyan` — Flat / minor quality — light variant. `#3A86C9` */
        val SignalCyan   = ComposeColor(0xFF3A86C9)
        /** `signal.amber` — Sharp / diminished quality — light variant. `#B68038` */
        val SignalAmber  = ComposeColor(0xFFB68038)
        /** `signal.violet` — Augmented chord quality — light variant. `#7A5AE0` */
        val SignalViolet = ComposeColor(0xFF7A5AE0)
    }

    // ─── Spacing ─────────────────────────────────────────────────────────────

    /**
     * Spacing scale — 8-pt base, 4-pt half-step. (DESIGN.md §4)
     *
     * Do not introduce intermediate values. Every layout dimension must be one
     * of these constants.
     *
     * ## Layout grid reference
     * - Screen horizontal padding: [s5] (20dp)
     * - Vertical rhythm between cards: [s4] (16dp); [s5] (20dp) for tuner readout-to-selector gap
     * - Card internal padding (list rows): [s4] (16dp); hero readouts: [s5] (20dp)
     * - Inter-element gap in horizontal rows (chips, buttons): [s2] (8dp)
     */
    object Sp {
        /** `sp.0` — 0dp (no space) */
        val s0  = 0.dp
        /** `sp.1` — 4dp (half-step) */
        val s1  = 4.dp
        /** `sp.2` — 8dp (inter-element gap) */
        val s2  = 8.dp
        /** `sp.3` — 12dp */
        val s3  = 12.dp
        /** `sp.4` — 16dp (card padding list rows, vertical card rhythm) */
        val s4  = 16.dp
        /** `sp.5` — 20dp (screen horizontal padding, hero card padding) */
        val s5  = 20.dp
        /** `sp.6` — 24dp */
        val s6  = 24.dp
        /** `sp.8` — 32dp */
        val s8  = 32.dp
        /** `sp.10` — 40dp */
        val s10 = 40.dp
        /** `sp.12` — 48dp */
        val s12 = 48.dp
    }

    // ─── Border radii ────────────────────────────────────────────────────────

    /**
     * Border radius tokens. (DESIGN.md §5)
     *
     * Use only these values — no 12.5dp, 13dp, or other off-scale radii.
     * [Pill] has no Material Shapes slot; apply it directly as
     * `RoundedCornerShape(Tq.Radius.Pill)` at the call site.
     */
    object Radius {
        /** `r.xs` — 4dp (inline note tags, chord note pills) */
        val Xs   = 4.dp
        /** `r.sm` — 8dp (chips, segment track children) */
        val Sm   = 8.dp
        /** `r.md` — 12dp (list rows, secondary buttons) */
        val Md   = 12.dp
        /** `r.lg` — 16dp (cards) */
        val Lg   = 16.dp
        /** `r.xl` — 18dp (readout wells) */
        val Xl   = 18.dp
        /** `r.pill` — 999dp (pill buttons, primary action, segmented control track) */
        val Pill = 999.dp
    }

    // ─── Type scale ──────────────────────────────────────────────────────────
    //
    // All letter-spacing values use `.em` so they scale correctly if a token's
    // font size ever changes (DESIGN.md §3).
    //
    // ALL-CAPS convention: [Kicker], [KickerS], and [MonoMicro] are uppercase by
    // design. Compose's TextStyle has no textAllCaps property, so callers MUST
    // pass uppercase strings when using these tokens.
    //
    // Display tokens and font scaling: [DisplayXl] and [DisplayL] are declared in
    // sp for source consistency, but MUST only be applied inside
    // `NonScalingText` (ui/components/NonScalingText.kt). That composable pins
    // fontScale = 1f, making sp behave as dp at runtime and preventing these
    // layout-critical readouts from overflowing their cards when the user has a
    // large system font-size setting (DESIGN.md §13.1).

    /**
     * Type scale tokens — Space Grotesk for UI chrome, JetBrains Mono for numerals
     * and kickers. (DESIGN.md §3)
     */
    object Type {

        /**
         * Metronome BPM numeral — 96sp, JetBrains Mono Light, −0.05em tracking.
         *
         * **Must be applied via `NonScalingText`** — never with plain `Text(...)`.
         * See DESIGN.md §13.1 and `ui/components/NonScalingText.kt`.
         */
        val DisplayXl = TextStyle(
            fontFamily          = JetBrainsMonoFamily,
            fontSize            = 96.sp,
            lineHeight          = 96.sp,
            fontWeight          = FontWeight.Light,
            letterSpacing       = (-0.05).em,
            fontFeatureSettings = "\"tnum\", \"ss01\"",
        )

        /**
         * Tuner detected-note letter — 64sp, JetBrains Mono Light, −0.04em tracking.
         *
         * **Must be applied via `NonScalingText`** — never with plain `Text(...)`.
         * See DESIGN.md §13.1 and `ui/components/NonScalingText.kt`.
         */
        val DisplayL = TextStyle(
            fontFamily          = JetBrainsMonoFamily,
            fontSize            = 64.sp,
            lineHeight          = 64.sp,
            fontWeight          = FontWeight.Light,
            letterSpacing       = (-0.04).em,
            fontFeatureSettings = "\"tnum\", \"ss01\"",
        )

        /** Brand artboard headlines — 32sp, Space Grotesk SemiBold, −0.018em tracking. */
        val DisplayS = TextStyle(
            fontFamily    = SpaceGroteskFamily,
            fontSize      = 32.sp,
            lineHeight    = 36.sp,
            fontWeight    = FontWeight.SemiBold,
            letterSpacing = (-0.018).em,
        )

        /** Screen titles — 22sp, Space Grotesk SemiBold, −0.023em tracking. */
        val H1 = TextStyle(
            fontFamily    = SpaceGroteskFamily,
            fontSize      = 22.sp,
            lineHeight    = 28.sp,
            fontWeight    = FontWeight.SemiBold,
            letterSpacing = (-0.023).em,
        )

        /** Card headers, chord names — 17sp, Space Grotesk SemiBold, −0.018em tracking. */
        val H2 = TextStyle(
            fontFamily    = SpaceGroteskFamily,
            fontSize      = 17.sp,
            lineHeight    = 22.sp,
            fontWeight    = FontWeight.SemiBold,
            letterSpacing = (-0.018).em,
        )

        /** Default body copy — 14sp, Space Grotesk Regular, −0.005em tracking. */
        val Body = TextStyle(
            fontFamily    = SpaceGroteskFamily,
            fontSize      = 14.sp,
            lineHeight    = 20.sp,
            fontWeight    = FontWeight.Normal,
            letterSpacing = (-0.005).em,
        )

        /** Active rows — 14sp, Space Grotesk Medium, −0.005em tracking. */
        val BodyStrong = TextStyle(
            fontFamily    = SpaceGroteskFamily,
            fontSize      = 14.sp,
            lineHeight    = 20.sp,
            fontWeight    = FontWeight.Medium,
            letterSpacing = (-0.005).em,
        )

        /** Secondary labels — 11sp, Space Grotesk Medium, 0em tracking. */
        val Caption = TextStyle(
            fontFamily    = SpaceGroteskFamily,
            fontSize      = 11.sp,
            lineHeight    = 14.sp,
            fontWeight    = FontWeight.Medium,
            letterSpacing = 0.em,
        )

        /**
         * All-caps section kickers — 10sp, JetBrains Mono Medium, +0.16em tracking.
         *
         * Callers **must pass uppercase strings**. Compose TextStyle has no textAllCaps.
         */
        val Kicker = TextStyle(
            fontFamily          = JetBrainsMonoFamily,
            fontSize            = 10.sp,
            lineHeight          = 14.sp,
            fontWeight          = FontWeight.Medium,
            letterSpacing       = 0.16.em,
            fontFeatureSettings = "\"tnum\", \"ss01\"",
        )

        /**
         * Card kickers, list headers — 9.5sp, JetBrains Mono Medium, +0.14em tracking.
         *
         * Callers **must pass uppercase strings**. Compose TextStyle has no textAllCaps.
         */
        val KickerS = TextStyle(
            fontFamily          = JetBrainsMonoFamily,
            fontSize            = 9.5.sp,
            lineHeight          = 12.sp,
            fontWeight          = FontWeight.Medium,
            letterSpacing       = 0.14.em,
            fontFeatureSettings = "\"tnum\", \"ss01\"",
        )

        /** String pills, BPM input — 15sp, JetBrains Mono Medium, −0.015em tracking. */
        val NumericM = TextStyle(
            fontFamily          = JetBrainsMonoFamily,
            fontSize            = 15.sp,
            lineHeight          = 18.sp,
            fontWeight          = FontWeight.Medium,
            letterSpacing       = (-0.015).em,
            fontFeatureSettings = "\"tnum\", \"ss01\"",
        )

        /** Detected/target frequencies — 12sp, JetBrains Mono Regular, −0.003em tracking. */
        val NumericS = TextStyle(
            fontFamily          = JetBrainsMonoFamily,
            fontSize            = 12.sp,
            lineHeight          = 16.sp,
            fontWeight          = FontWeight.Normal,
            letterSpacing       = (-0.003).em,
            fontFeatureSettings = "\"tnum\", \"ss01\"",
        )

        /**
         * Smallest readout labels — 9sp, JetBrains Mono Regular, +0.05em tracking.
         *
         * Callers **must pass uppercase strings**. Compose TextStyle has no textAllCaps.
         */
        val MonoMicro = TextStyle(
            fontFamily          = JetBrainsMonoFamily,
            fontSize            = 9.sp,
            lineHeight          = 12.sp,
            fontWeight          = FontWeight.Normal,
            letterSpacing       = 0.05.em,
            fontFeatureSettings = "\"tnum\", \"ss01\"",
        )
    }
}
