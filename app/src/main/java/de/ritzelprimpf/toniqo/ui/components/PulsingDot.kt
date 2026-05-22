package de.ritzelprimpf.toniqo.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A small filled circle that pulses its alpha between full opacity and ~30% on a ~1-second cycle.
 *
 * Used for the "running" state indicator in [MetronomeStatusKicker]. The animation runs
 * continuously and is not gated on reduced-motion — a gentle slow pulse is not considered
 * a vestibular trigger per DESIGN.md §9.
 *
 * @param color Fill colour of the dot.
 * @param dotSize Diameter. Defaults to 6dp.
 */
@Composable
fun PulsingDot(
    color: Color,
    modifier: Modifier = Modifier,
    dotSize: Dp = 6.dp,
) {
    val transition = rememberInfiniteTransition(label = "pulsing-dot")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulsing-dot-alpha",
    )

    Box(
        modifier = modifier
            .size(dotSize)
            .alpha(alpha)
            .background(color = color, shape = CircleShape),
    )
}
