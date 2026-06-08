package de.ritzelprimpf.toniqo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.ritzelprimpf.toniqo.chordfinder.domain.model.Barre
import de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordToneRole
import de.ritzelprimpf.toniqo.chordfinder.domain.model.FretMark
import de.ritzelprimpf.toniqo.chordfinder.domain.model.Voicing
import de.ritzelprimpf.toniqo.chordfinder.presentation.ui.FretboardRenderModel
import de.ritzelprimpf.toniqo.chordfinder.presentation.ui.toRenderModel
import de.ritzelprimpf.toniqo.ui.theme.Tq
import de.ritzelprimpf.toniqo.ui.theme.ToniqoTheme

// ── Layout constants (not design-spec dp values; diagram-internal geometry only) ──────────────
private val STRING_SPACING: Dp   = 18.dp
private val SIDE_PADDING: Dp     = 10.dp
private val POS_LABEL_WIDTH: Dp  = 20.dp
private val MARKER_AREA_HEIGHT: Dp = 20.dp
private val FRET_SPACING: Dp     = 26.dp
private val NUT_STROKE: Dp       = 4.dp
private val LINE_STROKE: Dp      = 1.dp
private val DOT_RADIUS: Dp       = 9.dp
private val MARKER_RADIUS: Dp    = 7.dp
private val MARKER_STROKE: Dp    = 1.5.dp
private val BARRE_RADIUS: Dp     = 9.dp

/**
 * Stateless fretboard diagram rendered onto a Compose [Canvas].
 *
 * The only input is a [FretboardRenderModel]; never a raw domain [Voicing].
 *
 * ### Layout geometry
 * - Width  = [POS_LABEL_WIDTH] + [SIDE_PADDING] + `(stringCount-1)` × [STRING_SPACING] + [SIDE_PADDING]
 * - Height = [MARKER_AREA_HEIGHT] + `fretWindow` × [FRET_SPACING]
 *
 * Typical widths: 6-string = 130dp, 7-string = 148dp, 8-string = 166dp.
 *
 * ### Visual layers (bottom to top)
 * 1. String lines (vertical, [Tq.Color.Line])
 * 2. Fret lines (horizontal); top line is nut (thick) or thin
 * 3. Position label `"Xfr"` when not at nut ([Tq.Color.FgTertiary])
 * 4. Barre rounded rectangle ([Tq.Color.BgElev3])
 * 5. Finger dots — root = [Tq.Color.SignalMint], others = [Tq.Color.BgElev3]
 * 6. Finger numeral text inside each dot
 * 7. ○ / × markers above the nut area
 *
 * @param model Fully resolved render model. See [de.ritzelprimpf.toniqo.chordfinder.presentation.ui.toRenderModel].
 */
@Composable
fun FretboardDiagram(
    model: FretboardRenderModel,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()

    // Colours — captured here to avoid referencing Tq inside DrawScope
    val colorLine       = Tq.Color.Line
    val colorNut        = Tq.Color.FgSecondary
    val colorPosLabel   = Tq.Color.FgTertiary
    val colorMarker     = Tq.Color.FgSecondary
    val colorRootDot    = Tq.Color.SignalMint
    val colorNeutralDot = Tq.Color.BgElev3
    val colorRootText   = Tq.Color.BgBase
    val colorNeutralText = Tq.Color.FgPrimary

    val posLabelStyle = Tq.Type.MonoMicro
    val fingerStyle   = Tq.Type.MonoMicro

    val totalWidth: Dp  = POS_LABEL_WIDTH + SIDE_PADDING +
            (model.stringCount - 1) * STRING_SPACING + SIDE_PADDING
    val totalHeight: Dp = MARKER_AREA_HEIGHT + model.fretWindow * FRET_SPACING

    Canvas(modifier = modifier.size(width = totalWidth, height = totalHeight)) {
        val stringSpacing   = STRING_SPACING.toPx()
        val sidePad         = SIDE_PADDING.toPx()
        val posLabelWidth   = POS_LABEL_WIDTH.toPx()
        val markerAreaH     = MARKER_AREA_HEIGHT.toPx()
        val fretSpacing     = FRET_SPACING.toPx()
        val nutStroke       = NUT_STROKE.toPx()
        val lineStroke      = LINE_STROKE.toPx()
        val dotRadius       = DOT_RADIUS.toPx()
        val markerRadius    = MARKER_RADIUS.toPx()
        val markerStroke    = MARKER_STROKE.toPx()
        val barreCorner     = CornerRadius(BARRE_RADIUS.toPx())

        val gridLeft  = posLabelWidth + sidePad
        val gridTop   = markerAreaH
        val gridRight = gridLeft + (model.stringCount - 1) * stringSpacing

        fun stringX(s: Int): Float = gridLeft + s * stringSpacing
        fun fretY(row: Int): Float = gridTop + row * fretSpacing
        fun fretCenterY(fretInWindow: Int): Float = gridTop + (fretInWindow - 0.5f) * fretSpacing

        // 1. String lines (vertical)
        for (s in 0 until model.stringCount) {
            drawLine(
                color = colorLine,
                start = Offset(stringX(s), gridTop),
                end   = Offset(stringX(s), fretY(model.fretWindow)),
                strokeWidth = lineStroke,
            )
        }

        // 2. Fret lines (horizontal); row 0 = nut or thin top edge
        for (row in 0..model.fretWindow) {
            val y = fretY(row)
            if (row == 0 && model.showNut) {
                drawLine(
                    color = colorNut,
                    start = Offset(gridLeft, y),
                    end   = Offset(gridRight, y),
                    strokeWidth = nutStroke,
                    cap = StrokeCap.Round,
                )
            } else {
                drawLine(
                    color = colorLine,
                    start = Offset(gridLeft, y),
                    end   = Offset(gridRight, y),
                    strokeWidth = lineStroke,
                )
            }
        }

        // 3. Position label (when not at nut)
        if (model.positionLabel != null) {
            val layout = textMeasurer.measure(
                text  = model.positionLabel,
                style = posLabelStyle,
            )
            val lx = (posLabelWidth - layout.size.width) / 2f
            val ly = fretCenterY(1) - layout.size.height / 2f
            drawText(
                textLayoutResult = layout,
                color    = colorPosLabel,
                topLeft  = Offset(lx.coerceAtLeast(0f), ly),
            )
        }

        // 4. Barre rounded rectangle
        model.barre?.let { barre ->
            val cy   = fretCenterY(barre.fretWithinWindow)
            val left  = stringX(barre.fromString) - dotRadius * 0.85f
            val right = stringX(barre.toString)   + dotRadius * 0.85f
            drawRoundRect(
                color       = colorNeutralDot,
                topLeft     = Offset(left, cy - dotRadius),
                size        = Size(right - left, dotRadius * 2),
                cornerRadius = barreCorner,
            )
        }

        // 5 + 6. Finger dots and numerals
        for (dot in model.dots) {
            val cx      = stringX(dot.stringIndex)
            val cy      = fretCenterY(dot.fretWithinWindow)
            val dotFill = if (dot.isRoot) colorRootDot else colorNeutralDot
            drawCircle(color = dotFill, radius = dotRadius, center = Offset(cx, cy))

            if (dot.finger != null) {
                val textColor  = if (dot.isRoot) colorRootText else colorNeutralText
                val numStyle   = fingerStyle.merge(TextStyle(color = textColor))
                val layout     = textMeasurer.measure(text = dot.finger.toString(), style = numStyle)
                val tx         = cx - layout.size.width  / 2f
                val ty         = cy - layout.size.height / 2f
                drawText(textLayoutResult = layout, topLeft = Offset(tx, ty))
            }
        }

        // 7. ○ / × markers above the nut area
        val markerCY = markerAreaH * 0.50f
        for (s in model.openStrings) {
            drawCircle(
                color  = colorMarker,
                radius = markerRadius,
                center = Offset(stringX(s), markerCY),
                style  = Stroke(width = markerStroke),
            )
        }
        for (s in model.mutedStrings) {
            val cx = stringX(s)
            val hs = markerRadius * 0.65f
            drawLine(
                color = colorMarker,
                start = Offset(cx - hs, markerCY - hs),
                end   = Offset(cx + hs, markerCY + hs),
                strokeWidth = markerStroke,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = colorMarker,
                start = Offset(cx + hs, markerCY - hs),
                end   = Offset(cx - hs, markerCY + hs),
                strokeWidth = markerStroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

/** Preview: open C major chord (standard 6-string at nut). */
@Preview(name = "FretboardDiagram – Open C Major", showBackground = true, backgroundColor = 0xFF1A1F22)
@Composable
private fun PreviewOpenChord() {
    ToniqoTheme {
        val voicing = Voicing(
            labelKey          = 1,
            marks             = listOf(FretMark.Muted, FretMark.Fretted(3), FretMark.Fretted(2), FretMark.Open, FretMark.Fretted(1), FretMark.Open),
            fingers           = listOf(0, 3, 2, 0, 1, 0),
            barre             = null,
            rootStringIndices = setOf(1, 4),
            bassDegree        = ChordToneRole.ROOT,
        )
        FretboardDiagram(model = voicing.toRenderModel())
    }
}

/** Preview: barre chord at fret 5 (position label shown). */
@Preview(name = "FretboardDiagram – Barre F5", showBackground = true, backgroundColor = 0xFF1A1F22)
@Composable
private fun PreviewBarreChord() {
    ToniqoTheme {
        val voicing = Voicing(
            labelKey          = 2,
            marks             = listOf(
                FretMark.Fretted(5), FretMark.Fretted(7), FretMark.Fretted(7),
                FretMark.Fretted(7), FretMark.Fretted(5), FretMark.Fretted(5),
            ),
            fingers           = listOf(1, 3, 4, 4, 1, 1),
            barre             = Barre(fret = 5, fromString = 0, toString = 5),
            rootStringIndices = setOf(0, 5),
            bassDegree        = ChordToneRole.ROOT,
        )
        FretboardDiagram(model = voicing.toRenderModel())
    }
}

/** Preview: high-position chord (baseFret = 9, shows "9fr" label). */
@Preview(name = "FretboardDiagram – High Position", showBackground = true, backgroundColor = 0xFF1A1F22)
@Composable
private fun PreviewHighPosition() {
    ToniqoTheme {
        val voicing = Voicing(
            labelKey          = 3,
            marks             = listOf(
                FretMark.Fretted(9), FretMark.Fretted(11), FretMark.Fretted(11),
                FretMark.Fretted(10), FretMark.Fretted(9), FretMark.Fretted(9),
            ),
            fingers           = listOf(1, 3, 4, 2, 1, 1),
            barre             = Barre(fret = 9, fromString = 0, toString = 5),
            rootStringIndices = setOf(0, 5),
            bassDegree        = ChordToneRole.ROOT,
        )
        FretboardDiagram(model = voicing.toRenderModel())
    }
}

/** Preview: 7-string diagram. */
@Preview(name = "FretboardDiagram – 7-string", showBackground = true, backgroundColor = 0xFF1A1F22)
@Composable
private fun Preview7String() {
    ToniqoTheme {
        val voicing = Voicing(
            labelKey          = 4,
            marks             = listOf(
                FretMark.Fretted(1), FretMark.Open, FretMark.Fretted(2),
                FretMark.Open, FretMark.Fretted(1), FretMark.Open, FretMark.Muted,
            ),
            fingers           = listOf(1, 0, 2, 0, 1, 0, 0),
            barre             = null,
            rootStringIndices = setOf(0, 4),
            bassDegree        = ChordToneRole.ROOT,
        )
        FretboardDiagram(model = voicing.toRenderModel())
    }
}
