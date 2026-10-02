package de.ritzelprimpf.toniqo.metronome.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import de.ritzelprimpf.toniqo.ui.theme.Tq
import java.util.Locale

// 60dp diameter per DESIGN.md §8.2 (TAP and SONG buttons)
private val CIRCLE_SIZE = 60.dp
private val CIRCLE_ICON_SIZE = 22.dp

/**
 * The Metronome's 60dp circle button: an icon above an uppercase [Tq.Type.MonoMicro] label on
 * `bg.elev2`. Shared by [TapTempoButton] and [SongSearchButton].
 *
 * The icon is decoration; [label] is what screen readers announce.
 */
@Composable
internal fun CircleLabelButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .size(CIRCLE_SIZE)
            .clip(CircleShape)
            .background(Tq.Color.BgElev2, CircleShape)
            .clickable(
                onClick = onClick,
                role = Role.Button,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Tq.Sp.s1, Alignment.CenterVertically),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,  // the label below announces for accessibility
            tint = Tq.Color.FgSecondary,
            modifier = Modifier.size(CIRCLE_ICON_SIZE),
        )
        Text(
            text = label.uppercase(Locale.ROOT),
            style = Tq.Type.MonoMicro,
            color = Tq.Color.FgSecondary,
        )
    }
}
