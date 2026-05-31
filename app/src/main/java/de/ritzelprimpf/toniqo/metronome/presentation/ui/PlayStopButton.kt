package de.ritzelprimpf.toniqo.metronome.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import de.ritzelprimpf.toniqo.R
import de.ritzelprimpf.toniqo.ui.theme.Tq

// 60dp pill height per DESIGN.md §8.2.
private val PILL_HEIGHT = 60.dp

// 24dp glow radius per DESIGN.md §6.1 and §10 (one of two permitted glows in the design).
private val GLOW_RADIUS = Tq.Sp.s6  // 24dp

private val ICON_SIZE = 22.dp

/**
 * Pill-shaped Start / Stop button with icon + text, per DESIGN.md §8.2 and revised Item 18.
 *
 * Stopped state: mint primary background + 24dp layered glow (DESIGN.md §6.1 / §10).
 * Running state: `bg.elev3` neutral background, no glow.
 *
 * The outer [Box] reserves vertical space for the glow on both sides of the pill so
 * the layout stays stable across state transitions. The [modifier] receives the caller's
 * layout constraints (typically [Modifier.weight(1f)] from the parent [Row]).
 */
@Composable
internal fun PlayStopButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor = if (isPlaying) Tq.Color.BgElev3 else Tq.Color.SignalMint
    val contentColor = if (isPlaying) Tq.Color.FgPrimary else Tq.Color.BgBase
    val label = stringResource(if (isPlaying) R.string.metronome_stop else R.string.metronome_start)
    val icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow

    // Container height = pill + glow clearance on each side so sibling items align correctly
    // regardless of whether the glow is visible.
    Box(
        modifier = modifier.height(PILL_HEIGHT + GLOW_RADIUS * 2f),
        contentAlignment = Alignment.Center,
    ) {
        if (!isPlaying) {
            // Glow layers — concentric semi-transparent pill shapes expanding outward.
            // Hardware-accelerated (no BlurMaskFilter). Alpha chosen to approximate 30%
            // mint at the full-radius edge (DESIGN.md §6.1 "~30% mint alpha").
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PILL_HEIGHT + GLOW_RADIUS * 2f)
                    .clip(RoundedCornerShape(Tq.Radius.Pill))
                    .background(Tq.Color.SignalMint.copy(alpha = 0.05f)),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PILL_HEIGHT + GLOW_RADIUS * 1.3f)
                    .clip(RoundedCornerShape(Tq.Radius.Pill))
                    .background(Tq.Color.SignalMint.copy(alpha = 0.09f)),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PILL_HEIGHT + GLOW_RADIUS * 0.5f)
                    .clip(RoundedCornerShape(Tq.Radius.Pill))
                    .background(Tq.Color.SignalMint.copy(alpha = 0.16f)),
            )
        }

        // Pill button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(PILL_HEIGHT)
                .clip(RoundedCornerShape(Tq.Radius.Pill))
                .background(bgColor, RoundedCornerShape(Tq.Radius.Pill))
                .clickable(onClick = onClick)
                .padding(horizontal = Tq.Sp.s6)
                .semantics(mergeDescendants = true) {
                    contentDescription = label
                    role = Role.Button
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,  // label announced by parent semantics
                tint = contentColor,
                modifier = Modifier.size(ICON_SIZE),
            )
            Spacer(Modifier.width(Tq.Sp.s2))
            Text(
                text = label,
                style = Tq.Type.BodyStrong,
                color = contentColor,
            )
        }
    }
}
