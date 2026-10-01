package de.ritzelprimpf.toniqo.metronome.presentation.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
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

// Minimum accessible tap-target width per DESIGN.md §13.4 ("minimum 44×44dp on every interactive
// element"). Segments are also 44dp tall, so this keeps them square at minimum size.
private val MIN_SEGMENT_WIDTH = 44.dp

private const val SEGMENT_HEIGHT_DP = 44

// How many upcoming segments' worth of extra width to request alongside the current beat when
// auto-scrolling a long (scrollable) bar into view. Without this, BringIntoViewRequester only
// scrolls the bare minimum to make the current segment visible — it would snap into view right at
// the edge instead of staying comfortably ahead of it.
private const val LOOKAHEAD_SEGMENT_COUNT = 2
private val LOOKAHEAD_WIDTH = (MIN_SEGMENT_WIDTH + Tq.Sp.s2) * LOOKAHEAD_SEGMENT_COUNT

// Animation override: 80ms linear, ignores reduced-motion per Phase6_4-PLAN.md decision.
private const val BEAT_ANIM_MS = 80

/** Test tag shared by all beat segments — used by BeatIndicatorTest to count nodes. */
internal const val BEAT_SEGMENT_TEST_TAG = "beat_segment"

/**
 * A horizontal row of N equal-width segments representing one bar of beats.
 *
 * Accented-beat lit state: mint fill + 12dp drawBehind glow.
 * Other lit beats: mint at 35% composited over [Tq.Color.BgElev2].
 * Unlit beats: [Tq.Color.BgElev1] with [Tq.Color.LineFaint] 1dp border.
 * Accented-beat unlit marker: 4dp mint dot centred inside the segment.
 *
 * Long-pressing a segment toggles that beat's accent via [onBeatLongPressed] — see
 * `docs/DECISIONS.md` 2026-10-01 "per-beat accent customization" entry. This generalizes what
 * `DESIGN.md` §8.2 originally specified as fixed "beat 1" styling to whichever beats are in
 * [accentedBeats]; the visual language itself (glow, dot, colour) is unchanged.
 *
 * The 80ms linear colour transition is intentional and overrides reduced-motion.
 * Visual beat feedback must fire even when the system animation scale is 0 — it
 * is the primary temporal indicator and disabling it would break usability.
 *
 * ## Layout for large numerators
 *
 * Custom time signatures (see `docs/DECISIONS.md` 2026-10-01 "free time signature input" entry)
 * allow up to 32 beats per bar — far more than a single screen width can show as equal-width,
 * accessibly-sized (44dp minimum, per DESIGN.md §13.4) tap targets. [BoxWithConstraints] measures
 * the available width and picks one of two layouts:
 * - **Fits:** segments spread to fill the row via `weight(1f)`, pixel-identical to the original
 *   fixed-preset behavior (every existing preset, including 12/8, fits this way).
 * - **Doesn't fit:** segments are pinned to [MIN_SEGMENT_WIDTH] (44dp) and the row becomes
 *   horizontally scrollable instead of shrinking below the accessible minimum.
 *
 * In the scrolling case, each segment auto-scrolls itself into view (with [LOOKAHEAD_SEGMENT_COUNT]
 * segments of margin ahead of it) the moment it becomes the lit one — otherwise, once playback
 * reaches a beat past the initially visible window, there would be no way to see which beat is
 * currently playing without the user manually scrolling to chase it. See `docs/DECISIONS.md`,
 * 2026-10-01 "beat indicator auto-scrolls to follow the current beat" entry.
 */
@Composable
internal fun BeatIndicator(
    numerator: Int,
    currentBeat: Int,
    isPlaying: Boolean,
    accentedBeats: Set<Int>,
    onBeatLongPressed: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val totalGapWidth = Tq.Sp.s2 * (numerator - 1).coerceAtLeast(0)
        val naturalSegmentWidth = (maxWidth - totalGapWidth) / numerator.coerceAtLeast(1)
        val fitsWithoutScrolling = naturalSegmentWidth >= MIN_SEGMENT_WIDTH

        if (fitsWithoutScrolling) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Tq.Sp.s2),
            ) {
                repeat(numerator) { index ->
                    BeatSegment(
                        index = index,
                        isAccented = index in accentedBeats,
                        isLit = isPlaying && currentBeat == index,
                        onLongPress = { onBeatLongPressed(index) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Tq.Sp.s2),
            ) {
                repeat(numerator) { index ->
                    BeatSegment(
                        index = index,
                        isAccented = index in accentedBeats,
                        isLit = isPlaying && currentBeat == index,
                        onLongPress = { onBeatLongPressed(index) },
                        modifier = Modifier.width(MIN_SEGMENT_WIDTH),
                    )
                }
            }
        }
    }
}

@Composable
private fun BeatSegment(
    index: Int,
    isAccented: Boolean,
    isLit: Boolean,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mintColor = Tq.Color.SignalMint
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    // No-op when this segment isn't inside a scrollable ancestor (the "fits without scrolling"
    // layout) — BringIntoViewRequester only does something when there's an ancestor to scroll.
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    LaunchedEffect(isLit) {
        if (isLit) {
            val extraWidthPx = with(density) { LOOKAHEAD_WIDTH.toPx() }
            val widthPx = with(density) { MIN_SEGMENT_WIDTH.toPx() }
            val heightPx = with(density) { SEGMENT_HEIGHT_DP.dp.toPx() }
            bringIntoViewRequester.bringIntoView(Rect(0f, 0f, widthPx + extraWidthPx, heightPx))
        }
    }

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isLit && isAccented -> mintColor
            isLit -> mintColor.copy(alpha = 0.35f).compositeOver(Tq.Color.BgElev2)
            else -> Tq.Color.BgElev1
        },
        animationSpec = tween(durationMillis = BEAT_ANIM_MS, easing = LinearEasing),
        label = "beat-segment-bg-$index",
    )

    Box(
        modifier = modifier
            .height(SEGMENT_HEIGHT_DP.dp)
            .bringIntoViewRequester(bringIntoViewRequester)
            .testTag(BEAT_SEGMENT_TEST_TAG)
            .semantics { stateDescription = if (isLit) "active" else "inactive" }
            .pointerInput(onLongPress) {
                detectTapGestures(
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongPress()
                    },
                )
            }
            .then(
                if (isLit && isAccented) {
                    Modifier.drawBehind {
                        drawSegmentGlow(mintColor, BEAT_GLOW_RADIUS.toPx(), SEGMENT_CORNER.toPx())
                    }
                } else Modifier,
            )
            .background(backgroundColor, RoundedCornerShape(SEGMENT_CORNER))
            .border(1.dp, Tq.Color.LineFaint, RoundedCornerShape(SEGMENT_CORNER)),
        contentAlignment = Alignment.Center,
    ) {
        if (isAccented && !isLit) {
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
