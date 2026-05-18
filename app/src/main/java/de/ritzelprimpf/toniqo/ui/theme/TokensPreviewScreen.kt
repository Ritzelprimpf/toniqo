package de.ritzelprimpf.toniqo.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.ritzelprimpf.toniqo.ui.components.NonScalingText

/**
 * Internal visual reference for all [Tq] design tokens. Not registered in any
 * navigation graph — not reachable from the running app via any UI path.
 *
 * Use Android Studio's Preview pane or temporarily wire [MainActivity] to this
 * composable during Phase 3 to verify the theme renders correctly. Replaced by
 * the real nav host in Phase 4.
 */
@Composable
fun TokensPreviewScreen(useDarkTheme: Boolean = true) {
    val bg = if (useDarkTheme) Tq.Color.BgBase else Tq.LightColor.BgBase
    val fg = if (useDarkTheme) Tq.Color.FgPrimary else Tq.LightColor.FgPrimary
    val fgSec = if (useDarkTheme) Tq.Color.FgSecondary else Tq.LightColor.FgSecondary
    val fgTer = if (useDarkTheme) Tq.Color.FgTertiary else Tq.LightColor.FgTertiary
    val fgQuat = if (useDarkTheme) Tq.Color.FgQuaternary else Tq.LightColor.FgQuaternary
    val mint = if (useDarkTheme) Tq.Color.SignalMint else Tq.LightColor.SignalMint
    val cyan = if (useDarkTheme) Tq.Color.SignalCyan else Tq.LightColor.SignalCyan
    val amber = if (useDarkTheme) Tq.Color.SignalAmber else Tq.LightColor.SignalAmber
    val violet = if (useDarkTheme) Tq.Color.SignalViolet else Tq.LightColor.SignalViolet

    LazyColumn(
        modifier = Modifier
            .background(bg)
            .fillMaxWidth()
            .padding(Tq.Sp.s5),
        verticalArrangement = Arrangement.spacedBy(Tq.Sp.s6),
    ) {
        item {
            Text(
                text = "Toniqo — Tokens Preview",
                style = Tq.Type.H1,
                color = fg,
            )
            Spacer(Modifier.height(Tq.Sp.s1))
            Text(
                text = if (useDarkTheme) "DARK THEME" else "LIGHT THEME",
                style = Tq.Type.Kicker,
                color = mint,
            )
        }

        // ── Colours: surfaces ────────────────────────────────────────────────
        item {
            SectionHeader(title = "COLOURS — SURFACES", labelColor = fgTer)
            Spacer(Modifier.height(Tq.Sp.s2))
            Row(horizontalArrangement = Arrangement.spacedBy(Tq.Sp.s2)) {
                val surfaces = if (useDarkTheme) listOf(
                    Tq.Color.BgBase   to "BG.BASE",
                    Tq.Color.BgElev1  to "BG.ELEV1",
                    Tq.Color.BgElev2  to "BG.ELEV2",
                    Tq.Color.BgElev3  to "BG.ELEV3",
                    Tq.Color.BgInset  to "BG.INSET",
                ) else listOf(
                    Tq.LightColor.BgBase   to "BG.BASE",
                    Tq.LightColor.BgElev1  to "BG.ELEV1",
                    Tq.LightColor.BgElev2  to "BG.ELEV2",
                    Tq.LightColor.BgElev3  to "BG.ELEV3",
                    Tq.LightColor.BgInset  to "BG.INSET",
                )
                surfaces.forEach { (color, label) ->
                    ColorSwatch(color = color, label = label, labelColor = fg, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(Tq.Sp.s2))
            Row(horizontalArrangement = Arrangement.spacedBy(Tq.Sp.s2)) {
                val lines = if (useDarkTheme) listOf(
                    Tq.Color.LineFaint  to "LINE.FAINT",
                    Tq.Color.Line       to "LINE",
                    Tq.Color.LineStrong to "LINE.STRONG",
                ) else listOf(
                    Tq.LightColor.LineFaint  to "LINE.FAINT",
                    Tq.LightColor.Line       to "LINE",
                    Tq.LightColor.LineStrong to "LINE.STRONG",
                )
                lines.forEach { (color, label) ->
                    ColorSwatch(color = color, label = label, labelColor = fg, modifier = Modifier.weight(1f))
                }
            }
        }

        // ── Colours: text ────────────────────────────────────────────────────
        item {
            SectionHeader(title = "COLOURS — TEXT", labelColor = fgTer)
            Spacer(Modifier.height(Tq.Sp.s2))
            Column(verticalArrangement = Arrangement.spacedBy(Tq.Sp.s2)) {
                Text("fg.primary — Body, hero numerals", style = Tq.Type.Body, color = fg)
                Text("fg.secondary — Sub-labels", style = Tq.Type.Body, color = fgSec)
                Text("fg.tertiary — Captions, kickers", style = Tq.Type.Body, color = fgTer)
                Text("fg.quaternary — Disabled / hint", style = Tq.Type.Body, color = fgQuat)
            }
        }

        // ── Colours: signal ──────────────────────────────────────────────────
        item {
            SectionHeader(title = "COLOURS — SIGNAL", labelColor = fgTer)
            Spacer(Modifier.height(Tq.Sp.s2))
            Row(horizontalArrangement = Arrangement.spacedBy(Tq.Sp.s2)) {
                ColorSwatch(color = mint,   label = "MINT",   labelColor = fg, modifier = Modifier.weight(1f))
                ColorSwatch(color = cyan,   label = "CYAN",   labelColor = fg, modifier = Modifier.weight(1f))
                ColorSwatch(color = amber,  label = "AMBER",  labelColor = fg, modifier = Modifier.weight(1f))
                ColorSwatch(color = violet, label = "VIOLET", labelColor = fg, modifier = Modifier.weight(1f))
            }
        }

        // ── Typography ───────────────────────────────────────────────────────
        item {
            SectionHeader(title = "TYPOGRAPHY", labelColor = fgTer)
            Spacer(Modifier.height(Tq.Sp.s2))
            Column(verticalArrangement = Arrangement.spacedBy(Tq.Sp.s3)) {
                TypeSample(label = "display.xl", style = Tq.Type.DisplayXl, color = fg, sample = "A", nonScaling = true)
                TypeSample(label = "display.l",  style = Tq.Type.DisplayL,  color = fg, sample = "A4", nonScaling = true)
                TypeSample(label = "display.s",  style = Tq.Type.DisplayS,  color = fg, sample = "Toniqo")
                TypeSample(label = "h1",         style = Tq.Type.H1,        color = fg, sample = "Screen Title")
                TypeSample(label = "h2",         style = Tq.Type.H2,        color = fg, sample = "Card Header")
                TypeSample(label = "body",       style = Tq.Type.Body,      color = fg, sample = "Default body copy")
                TypeSample(label = "body.strong",style = Tq.Type.BodyStrong,color = fg, sample = "Active row text")
                TypeSample(label = "caption",    style = Tq.Type.Caption,   color = fgSec, sample = "Secondary label")
                TypeSample(label = "kicker",     style = Tq.Type.Kicker,    color = fgTer, sample = "TUNER · A4 = 440 HZ")
                TypeSample(label = "kicker.s",   style = Tq.Type.KickerS,   color = fgTer, sample = "6-STRING · STANDARD")
                TypeSample(label = "numeric.m",  style = Tq.Type.NumericM,  color = fg,    sample = "E2")
                TypeSample(label = "numeric.s",  style = Tq.Type.NumericS,  color = fgSec, sample = "82.41 HZ")
                TypeSample(label = "mono.micro", style = Tq.Type.MonoMicro, color = fgTer, sample = "LISTENING")
            }
        }

        // ── Spacing ──────────────────────────────────────────────────────────
        item {
            SectionHeader(title = "SPACING", labelColor = fgTer)
            Spacer(Modifier.height(Tq.Sp.s2))
            val spacings = listOf(
                Tq.Sp.s1  to "s1·4",
                Tq.Sp.s2  to "s2·8",
                Tq.Sp.s3  to "s3·12",
                Tq.Sp.s4  to "s4·16",
                Tq.Sp.s5  to "s5·20",
                Tq.Sp.s6  to "s6·24",
                Tq.Sp.s8  to "s8·32",
                Tq.Sp.s10 to "s10·40",
                Tq.Sp.s12 to "s12·48",
            )
            Column(verticalArrangement = Arrangement.spacedBy(Tq.Sp.s1)) {
                spacings.forEach { (size, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .height(16.dp)
                                .width(size)
                                .background(mint, RoundedCornerShape(2.dp))
                        )
                        Spacer(Modifier.width(Tq.Sp.s2))
                        Text(label, style = Tq.Type.MonoMicro, color = fgTer)
                    }
                }
            }
        }

        // ── Radii ────────────────────────────────────────────────────────────
        item {
            SectionHeader(title = "RADII", labelColor = fgTer)
            Spacer(Modifier.height(Tq.Sp.s2))
            val radii = listOf(
                Tq.Radius.Xs   to "XS·4",
                Tq.Radius.Sm   to "SM·8",
                Tq.Radius.Md   to "MD·12",
                Tq.Radius.Lg   to "LG·16",
                Tq.Radius.Xl   to "XL·18",
                Tq.Radius.Pill to "PILL·999",
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(Tq.Sp.s2),
                modifier = Modifier.fillMaxWidth(),
            ) {
                radii.forEach { (radius, label) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(radius.coerceAtMost(20.dp)))
                                .background(mint.copy(alpha = 0.2f))
                                .border(1.dp, mint, RoundedCornerShape(radius.coerceAtMost(20.dp)))
                        )
                        Spacer(Modifier.height(Tq.Sp.s1))
                        Text(label, style = Tq.Type.MonoMicro, color = fgTer)
                    }
                }
            }
            Spacer(Modifier.height(Tq.Sp.s8))
        }
    }
}

// ─── Private helpers ─────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, labelColor: Color) {
    Text(text = title, style = Tq.Type.Kicker, color = labelColor)
}

@Composable
private fun ColorSwatch(
    color: Color,
    label: String,
    labelColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(Tq.Radius.Sm))
                .background(color)
                .border(1.dp, Tq.Color.LineFaint, RoundedCornerShape(Tq.Radius.Sm))
        )
        Spacer(Modifier.height(Tq.Sp.s1))
        Text(text = label, style = Tq.Type.MonoMicro, color = labelColor)
    }
}

@Composable
private fun TypeSample(
    label: String,
    style: TextStyle,
    color: Color,
    sample: String,
    nonScaling: Boolean = false,
) {
    Column {
        if (nonScaling) {
            NonScalingText(text = sample, style = style.copy(color = color))
        } else {
            Text(text = sample, style = style, color = color)
        }
        Text(
            text = label,
            style = Tq.Type.MonoMicro,
            color = Tq.Color.FgQuaternary,
        )
    }
}

// ─── Previews ─────────────────────────────────────────────────────────────────

/** Dark-theme preview — the primary design target. */
@Preview(name = "Tokens — Dark", showBackground = true, backgroundColor = 0xFF1A1F22)
@Composable
private fun TokensPreviewDark() {
    ToniqoTheme(useDarkTheme = true) {
        TokensPreviewScreen(useDarkTheme = true)
    }
}

/** Light-theme preview — the fallback. */
@Preview(name = "Tokens — Light", showBackground = true, backgroundColor = 0xFFF8F9FA)
@Composable
private fun TokensPreviewLight() {
    ToniqoTheme(useDarkTheme = false) {
        TokensPreviewScreen(useDarkTheme = false)
    }
}
