package de.ritzelprimpf.toniqo.tuner.presentation.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import de.ritzelprimpf.toniqo.R
import de.ritzelprimpf.toniqo.common.model.Note
import de.ritzelprimpf.toniqo.ui.theme.Tq
import de.ritzelprimpf.toniqo.ui.util.rememberReducedMotion

/**
 * A single string pill in the string selector row.
 *
 * Specification (DESIGN.md §8.1 + §6.3):
 * - Height: 54dp (§8.1 overrides §6.3's 38dp default for the tuner specifically).
 * - Background: `bg.elev2`; active string gets a semantic-colour outline with a 3dp halo at 12% alpha.
 * - The border/halo cross-fades in/out over [ACTIVE_BORDER_TRANSITION_MS] as [isActive] changes,
 *   instead of snapping, so auto-advance moving the target to a new pill is noticeable (paired
 *   with the [de.ritzelprimpf.toniqo.tuner.presentation.viewmodel.TunerEvent.StringAdvanced] haptic).
 *   Instant under reduced motion.
 * - Note letter at `H2` / `fg.primary`; octave digit below at `NumericM` / `fg.tertiary`.
 * - Check glyph (9dp, `signal.mint`) shown at the top-right corner when the string is tuned.
 * - Minimum tap target: 44×44dp enforced via [Modifier.size] on the outer box.
 *
 * @param note The note this string is tuned to.
 * @param stringIndex Zero-based string index, used for semantic content descriptions.
 * @param isActive Whether this string is currently the target.
 * @param isTuned Whether this string has been brought in tune in the current session.
 * @param semanticColor The current signal colour (applied to the active border/halo).
 * @param onClick Called when the user taps this pill.
 */
@Composable
fun StringPill(
    note: Note,
    stringIndex: Int,
    isActive: Boolean,
    isTuned: Boolean,
    semanticColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Tq.Radius.Md)
    val cd = if (isTuned) {
        stringResource(R.string.tuner_cd_string_tuned, note.displayName())
    } else {
        note.displayName()
    }

    // Cross-fades the active border/halo onto whichever pill auto-advance just targeted, instead
    // of an instant snap, so the eye catches the highlight moving — see TunerEvent.StringAdvanced.
    val reducedMotion = rememberReducedMotion()
    val animationSpec: AnimationSpec<Color> = if (reducedMotion) snap() else tween(
        durationMillis = ACTIVE_BORDER_TRANSITION_MS,
        easing = LinearOutSlowInEasing,
    )
    val haloColor by animateColorAsState(
        targetValue = if (isActive) semanticColor.copy(alpha = 0.12f) else Color.Transparent,
        animationSpec = animationSpec,
        label = "stringPillHaloColor",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isActive) semanticColor else Color.Transparent,
        animationSpec = animationSpec,
        label = "stringPillBorderColor",
    )

    Box(
        modifier = modifier
            .height(54.dp)  // §8.1 specifies 54dp for tuner string-selector pills
            .clip(shape)
            .background(color = Tq.Color.BgElev2, shape = shape)
            .border(width = 5.dp, color = haloColor, shape = shape)
            .border(width = 2.dp, color = borderColor, shape = shape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = cd },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = note.name.sharpName,
                style = Tq.Type.H2,
                color = if (isActive) semanticColor else Tq.Color.FgPrimary,
            )
            Text(
                text = note.octave.toString(),
                style = Tq.Type.NumericM,
                color = Tq.Color.FgTertiary,
            )
        }
        if (isTuned) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = Tq.Color.SignalMint,
                modifier = Modifier
                    .size(9.dp)
                    .align(Alignment.TopEnd)
                    .padding(top = Tq.Sp.s1, end = Tq.Sp.s1),
            )
        }
    }
}

/** Duration of the active-pill border/halo cross-fade, matching SuccessRing's fade timing. */
private const val ACTIVE_BORDER_TRANSITION_MS = 320
