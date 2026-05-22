package de.ritzelprimpf.toniqo.metronome.presentation.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import de.ritzelprimpf.toniqo.ui.theme.Tq

// Beat-indicator glow: 12dp radius per DESIGN.md §8.2. Rendered with drawBehind concentric
// semi-transparent rounded rects — hardware-accelerated, no BlurMaskFilter.
private val BEAT_GLOW_RADIUS = 12.dp

// Segment corner radius — r.sm from design token table (§5)
private val SEGMENT_CORNER = Tq.Radius.Sm

// Accent dot inside unlit beat-1 segment: 4dp per DESIGN.md §8.2
private val ACCENT_DOT_SIZE = 4.dp

// Animation override: 80ms linear, ignores reduced-motion per Phase6_4-PLAN.md decision.
private const val BEAT_ANIM_MS = 80

/** Test tag shared by all beat segments — used by BeatIndicatorTest to count nodes. */
internal const val BEAT_SEGMENT_TEST_TAG = "beat_segment"

/**
 * A horizontal row of N equal-width segments representing one bar of beats.
 *
 * Beat 1 lit state: mint fill + 12dp drawBehind glow.
 * Beats 2..N lit state: mint at 35% composited over [Tq.Color.BgElev2].
 * Unlit beats: [Tq.Color.BgElev1] with [Tq.Color.LineFaint] 1dp border.
 * Beat-1 unlit accent: 4dp mint dot centred inside the first segment.
 *
 * The 80ms linear colour transition is intentional and overrides reduced-motion.
 * Visual beat feedback must fire even when the system animation scale is 0 — it
 * is the primary temporal indicator and disabling it would break usability.
 */
@Composable
internal fun BeatIndicator(
    numerator: Int,
    currentBeat: Int,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Tq.Sp.s2),
    ) {
        repeat(numerator) { index ->
            BeatSegment(
                index = index,
                isBeatOne = index == 0,
                isLit = isPlaying && currentBeat == index,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun BeatSegment(
    index: Int,
    isBeatOne: Boolean,
    isLit: Boolean,
    modifier: Modifier = Modifier,
) {
    val mintColor = Tq.Color.SignalMint

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isLit && isBeatOne -> mintColor
            isLit -> mintColor.copy(alpha = 0.35f).compositeOver(Tq.Color.BgElev2)
            else -> Tq.Color.BgElev1
        },
        animationSpec = tween(durationMillis = BEAT_ANIM_MS, easing = LinearEasing),
        label = "beat-segment-bg-$index",
    )

    Box(
        modifier = modifier
            .height(44.dp)
            .testTag(BEAT_SEGMENT_TEST_TAG)
            .semantics { stateDescription = if (isLit) "active" else "inactive" }
            .then(
                if (isLit && isBeatOne) {
                    Modifier.drawBehind {
                        drawSegmentGlow(mintColor, BEAT_GLOW_RADIUS.toPx(), SEGMENT_CORNER.toPx())
                    }
                } else Modifier,
            )
            .background(backgroundColor, RoundedCornerShape(SEGMENT_CORNER))
            .border(1.dp, Tq.Color.LineFaint, RoundedCornerShape(SEGMENT_CORNER)),
        contentAlignment = Alignment.Center,
    ) {
        if (isBeatOne && !isLit) {
            Box(
                Modifier
                    .size(ACCENT_DOT_SIZE)
                    .background(mintColor, CircleShape),
            )
        }
    }
}

/**
 * Draws concentric semi-transparent rounded rects behind the segment to simulate a soft glow.
 *
 * Three layers at increasing expand radii and decreasing opacity create a smooth fade-out
 * without requiring [android.graphics.BlurMaskFilter] (which needs a software render layer).
 */
private fun DrawScope.drawSegmentGlow(color: Color, glowPx: Float, cornerPx: Float) {
    data class Layer(val expand: Float, val alpha: Float)
    listOf(
        Layer(glowPx,        0.07f),
        Layer(glowPx * 0.5f, 0.13f),
        Layer(glowPx * 0.2f, 0.22f),
    ).forEach { (expand, alpha) ->
        drawRoundRect(
            color = color.copy(alpha = alpha),
            topLeft = Offset(-expand, -expand),
            size = Size(size.width + 2 * expand, size.height + 2 * expand),
            cornerRadius = CornerRadius(cornerPx + expand),
        )
    }
}
