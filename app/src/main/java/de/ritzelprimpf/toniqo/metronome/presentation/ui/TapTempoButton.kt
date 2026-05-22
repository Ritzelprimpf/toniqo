package de.ritzelprimpf.toniqo.metronome.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import de.ritzelprimpf.toniqo.R
import de.ritzelprimpf.toniqo.ui.theme.Tq
import java.util.Locale

// 60dp diameter per DESIGN.md §8.2
private val TAP_SIZE = 60.dp
private val TAP_ICON_SIZE = 22.dp

/**
 * A 60dp circle button that triggers tap-tempo on each press.
 *
 * Shows a touch-gesture icon above the uppercase "TAP" label in [Tq.Type.MonoMicro].
 * The icon is visual decoration; the text label is what screen readers announce.
 */
@Composable
internal fun TapTempoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .size(TAP_SIZE)
            .clip(CircleShape)
            .background(Tq.Color.BgElev2, CircleShape)
            .clickable(
                onClick = onClick,
                indication = ripple(bounded = true),
                interactionSource = remember { MutableInteractionSource() },
                role = Role.Button,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Tq.Sp.s1, Alignment.CenterVertically),
    ) {
        Icon(
            imageVector = Icons.Outlined.TouchApp,
            contentDescription = null,  // "TAP" label below announces for accessibility
            tint = Tq.Color.FgSecondary,
            modifier = Modifier.size(TAP_ICON_SIZE),
        )
        Text(
            text = stringResource(R.string.metronome_tap_tempo).uppercase(Locale.ROOT),
            style = Tq.Type.MonoMicro,
            color = Tq.Color.FgSecondary,
        )
    }
}
