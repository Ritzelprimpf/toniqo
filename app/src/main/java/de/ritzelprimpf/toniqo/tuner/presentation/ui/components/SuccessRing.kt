package de.ritzelprimpf.toniqo.tuner.presentation.ui.components

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import de.ritzelprimpf.toniqo.ui.theme.Tq
import de.ritzelprimpf.toniqo.ui.util.rememberReducedMotion

/**
 * Mint border ring that overlays the ReadoutWell when all strings are tuned.
 *
 * Place this as a sibling of the `ReadoutWell` inside a parent `Box`, passing
 * `Modifier.matchParentSize()` as [modifier] so the ring covers the same area.
 *
 * Fade-in and fade-out each take 320 ms (ease-out per DESIGN.md §9). Under reduced motion,
 * transitions are instant. The 1200 ms hold is driven externally: the ViewModel sets
 * [visible] to `true`, waits, then sets it to `false`.
 *
 * @param visible `true` to fade the ring in; `false` to fade it out.
 */
@Composable
fun SuccessRing(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = rememberReducedMotion()
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = if (reducedMotion) snap() else tween(
            durationMillis = 320,
            easing = LinearOutSlowInEasing,
        ),
        label = "successRingAlpha",
    )

    Box(
        modifier = modifier
            .alpha(alpha)
            .border(
                width = 2.dp,
                color = Tq.Color.SignalMint,
                shape = RoundedCornerShape(Tq.Radius.Xl),
            ),
    )
}
