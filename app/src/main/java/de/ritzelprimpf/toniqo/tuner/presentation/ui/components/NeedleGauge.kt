package de.ritzelprimpf.toniqo.tuner.presentation.ui.components

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.ritzelprimpf.toniqo.ui.theme.Tq
import de.ritzelprimpf.toniqo.ui.util.rememberReducedMotion
import kotlin.math.cos
import kotlin.math.sin

// Canvas physical dimensions (DESIGN.md §8.1)
private val CANVAS_WIDTH = 280.dp
private val CANVAS_HEIGHT = 150.dp
private val NEEDLE_RADIUS = 120.dp

// Needle geometry constants
private const val MAX_CENTS = 50.0
private const val MAX_ANGLE_DEG = 60f          // ±60° corresponds to ±50 cents
private const val SWEET_SPOT_ANGLE_DEG = 6f    // ±5 cents → ±6°
private const val TICK_INTERVAL_DEG = 12f      // one tick every ~10 cents
private const val MAJOR_TICK_INTERVAL_DEG = 36f // major tick every 30 cents

private const val NEEDLE_ANIM_MS = 200
private const val NEEDLE_ANIM_MS_REDUCED = 80

/**
 * Compose Canvas-based needle gauge for the Tuner screen.
 *
 * All elements are drawn per-frame: tick marks, sweet-spot arc, needle, and pivot cap.
 * The needle glow is implemented via [drawIntoCanvas] with [android.graphics.BlurMaskFilter]
 * (chosen over Modifier.shadow because Canvas drawLine shapes are not clipped by a Modifier
 * shape, making the MaskFilter approach the only reliable option for a fine-line glow).
 *
 * Motion (DESIGN.md §9): 200ms cubic-bezier(0.4, 1.2, 0.5, 1) when reduced-motion is off;
 * 80ms linear when reduced-motion is on.
 *
 * @param cents The current cents offset from the target. `null` indicates the idle state.
 * @param semanticColor The signal colour for the needle (mint/cyan/amber/fg.quaternary).
 */
@Composable
fun NeedleGauge(
    cents: Double?,
    semanticColor: Color,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = rememberReducedMotion()

    val targetAngle = if (cents == null) 0f
    else (cents.coerceIn(-MAX_CENTS, MAX_CENTS) / MAX_CENTS * MAX_ANGLE_DEG).toFloat()

    val animSpec = if (reducedMotion) {
        tween<Float>(durationMillis = NEEDLE_ANIM_MS_REDUCED, easing = LinearEasing)
    } else {
        tween<Float>(
            durationMillis = NEEDLE_ANIM_MS,
            easing = androidx.compose.animation.core.CubicBezierEasing(0.4f, 1.2f, 0.5f, 1f),
        )
    }

    val animatedAngle by animateFloatAsState(
        targetValue = targetAngle,
        animationSpec = animSpec,
        label = "needle_angle",
    )

    val isIdle = cents == null

    // Colours — captured here in composable scope to avoid referencing Tq inside DrawScope
    // (Tq.Color properties are theme-reactive composable getters; DrawScope draw* helpers below
    // are plain, non-composable functions, so they receive colours as parameters instead).
    val tickColor = Tq.Color.FgTertiary
    val majorTickColor = Tq.Color.FgSecondary
    val mintColor = Tq.Color.SignalMint
    val idleColor = Tq.Color.FgQuaternary
    val pivotCapFillColor = Tq.Color.BgElev3
    val pivotCapBorderColor = Tq.Color.Line
    val rangeLabelColor = Tq.Color.FgTertiary

    Canvas(
        modifier = modifier.requiredSize(CANVAS_WIDTH, CANVAS_HEIGHT),
    ) {
        val cw = CANVAS_WIDTH.toPx()
        val ch = CANVAS_HEIGHT.toPx()
        val pivotX = cw / 2f
        val pivotY = ch           // pivot at bottom-centre
        val radius = NEEDLE_RADIUS.toPx()

        drawTickMarks(pivotX, pivotY, radius, tickColor, majorTickColor, mintColor)
        drawSweetSpotArc(pivotX, pivotY, radius, semanticColor, isIdle, mintColor)
        drawNeedle(animatedAngle, pivotX, pivotY, radius, semanticColor, isIdle, idleColor)
        drawPivotCap(pivotX, pivotY, semanticColor, isIdle, idleColor, pivotCapFillColor, pivotCapBorderColor)
        drawRangeLabels(pivotX, pivotY, radius, rangeLabelColor)
    }
}

// ── Drawing helpers ───────────────────────────────────────────────────────────

private fun DrawScope.drawTickMarks(
    pivotX: Float,
    pivotY: Float,
    radius: Float,
    tickPaint: Color,
    majorTickPaint: Color,
    centerTickColor: Color,
) {
    var angle = -MAX_ANGLE_DEG
    while (angle <= MAX_ANGLE_DEG + 0.01f) {
        val isMajor = (angle % MAJOR_TICK_INTERVAL_DEG) == 0f
        val isCenter = angle == 0f
        val tickLen = if (isMajor || isCenter) 10.dp.toPx() else 6.dp.toPx()
        val color = when {
            isCenter -> centerTickColor
            isMajor -> majorTickPaint
            else -> tickPaint
        }
        val rad = Math.toRadians((angle - 90).toDouble())
        val outerX = pivotX + cos(rad).toFloat() * radius
        val outerY = pivotY + sin(rad).toFloat() * radius
        val innerX = pivotX + cos(rad).toFloat() * (radius - tickLen)
        val innerY = pivotY + sin(rad).toFloat() * (radius - tickLen)
        drawLine(
            color = color,
            start = Offset(outerX, outerY),
            end = Offset(innerX, innerY),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round,
        )
        angle += TICK_INTERVAL_DEG
    }
}

private fun DrawScope.drawSweetSpotArc(
    pivotX: Float,
    pivotY: Float,
    radius: Float,
    semanticColor: Color,
    isIdle: Boolean,
    mintColor: Color,
) {
    val arcAlpha = if (semanticColor == mintColor && !isIdle) 1f else 0.4f
    val arcColor = mintColor.copy(alpha = arcAlpha)
    val sweepDeg = SWEET_SPOT_ANGLE_DEG * 2f
    val startAngle = -90f - SWEET_SPOT_ANGLE_DEG  // canvas angles are clockwise from 3-o'clock

    // Draw as a pair of short lines approximating the arc (simpler than drawArc path)
    val steps = 20
    val arcRadius = radius - 2.dp.toPx()
    var prev: Offset? = null
    for (i in 0..steps) {
        val t = i.toFloat() / steps
        val r = Math.toRadians((startAngle + sweepDeg * t).toDouble())
        val x = pivotX + cos(r).toFloat() * arcRadius
        val y = pivotY + sin(r).toFloat() * arcRadius
        val current = Offset(x, y)
        if (prev != null) {
            drawLine(
                color = arcColor,
                start = prev!!,
                end = current,
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        prev = current
    }
}

private fun DrawScope.drawNeedle(
    angleDeg: Float,
    pivotX: Float,
    pivotY: Float,
    radius: Float,
    semanticColor: Color,
    isIdle: Boolean,
    idleColor: Color,
) {
    val rad = Math.toRadians((angleDeg - 90).toDouble())
    val tipX = pivotX + cos(rad).toFloat() * (radius - 4.dp.toPx())
    val tipY = pivotY + sin(rad).toFloat() * (radius - 4.dp.toPx())
    val pivot = Offset(pivotX, pivotY)
    val tip = Offset(tipX, tipY)

    if (!isIdle) {
        // Glow layer: draw a blurred version of the needle using nativeCanvas + BlurMaskFilter
        drawIntoCanvas { canvas ->
            val glowPaint = Paint().asFrameworkPaint()
            glowPaint.style = android.graphics.Paint.Style.STROKE
            glowPaint.strokeWidth = 6.dp.toPx()
            glowPaint.strokeCap = android.graphics.Paint.Cap.ROUND
            glowPaint.color = semanticColor.copy(alpha = 0.5f).toArgb()
            glowPaint.maskFilter = BlurMaskFilter(6.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
            canvas.nativeCanvas.drawLine(pivotX, pivotY, tipX, tipY, glowPaint)
        }
    }

    // Needle proper
    drawLine(
        color = if (isIdle) idleColor else semanticColor,
        start = pivot,
        end = tip,
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawPivotCap(
    pivotX: Float,
    pivotY: Float,
    semanticColor: Color,
    isIdle: Boolean,
    idleColor: Color,
    fillColor: Color,
    borderColor: Color,
) {
    val outerR = 5.dp.toPx() / 2f
    val innerR = 2.dp.toPx() / 2f
    val pivot = Offset(pivotX, pivotY)
    // Outer circle: bg.elev3 fill + line border
    drawCircle(color = fillColor, radius = outerR, center = pivot)
    drawCircle(
        color = borderColor,
        radius = outerR,
        center = pivot,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.8.dp.toPx()),
    )
    // Inner dot: semantic colour
    drawCircle(
        color = if (isIdle) idleColor else semanticColor,
        radius = innerR,
        center = pivot,
    )
}

private fun DrawScope.drawRangeLabels(pivotX: Float, pivotY: Float, radius: Float, labelColor: Color) {
    val paint = android.graphics.Paint().apply {
        color = labelColor.copy(alpha = 0.6f).toArgb()
        textSize = 10.sp.toPx()
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
    }
    val labelOffset = radius + 10.dp.toPx()

    val leftRad = Math.toRadians((-MAX_ANGLE_DEG - 90.0))
    val lx = pivotX + cos(leftRad).toFloat() * labelOffset
    val ly = pivotY + sin(leftRad).toFloat() * labelOffset
    drawContext.canvas.nativeCanvas.drawText("−50", lx, ly, paint)

    val rightRad = Math.toRadians((MAX_ANGLE_DEG - 90.0))
    val rx = pivotX + cos(rightRad).toFloat() * labelOffset
    val ry = pivotY + sin(rightRad).toFloat() * labelOffset
    drawContext.canvas.nativeCanvas.drawText("+50", rx, ry, paint)
}
