package de.ritzelprimpf.toniqo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import de.ritzelprimpf.toniqo.ui.navigation.BottomNavDestination
import de.ritzelprimpf.toniqo.ui.navigation.bottomNavDestinations
import de.ritzelprimpf.toniqo.ui.theme.Tq
import de.ritzelprimpf.toniqo.ui.theme.ToniqoTheme

/**
 * Toniqo bottom navigation bar — `icon-label` style.
 *
 * Specification (DESIGN.md §6.4):
 * - 48dp content row + system navigation bar inset below
 * - 1dp `line.faint` top border
 * - Active colour: `signal.mint`; inactive: `fg.tertiary`
 * - 20dp icon + `MonoMicro` uppercase label, 3dp gap between them
 * - Active indicator: 18×2dp mint pill at the top of the active cell,
 *   with a 6dp mint glow (one of the two design-language glows, §10)
 *
 * Each item is a semantics-annotated `Role.Tab` with a minimum 44×44dp tap
 * target (the 48dp row height satisfies the vertical requirement; widths on
 * typical phones are ≥ 60dp per item).
 *
 * @param destinations The five bottom-nav destinations, in display order.
 * @param currentDestination The active [NavDestination] from the nav back stack.
 * @param onNavigate Called with the destination's route when an item is tapped.
 */
@Composable
fun AppNavigationBar(
    destinations: List<BottomNavDestination>,
    currentDestination: NavDestination?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Tq.Color.BgBase),
    ) {
        HorizontalDivider(
            color = Tq.Color.LineFaint,
            thickness = 1.dp,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            destinations.forEach { destination ->
                val isSelected = currentDestination?.hierarchy?.any {
                    it.route == destination.route
                } == true

                ToniqoNavItem(
                    destination = destination,
                    isSelected = isSelected,
                    onClick = { onNavigate(destination.route) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }

        // Consume system navigation bar insets so Scaffold measures the
        // total height correctly and content doesn't render behind the gesture area.
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.navigationBars)
                .background(Tq.Color.BgBase),
        )
    }
}

// ─── Single nav item ──────────────────────────────────────────────────────────

@Composable
private fun ToniqoNavItem(
    destination: BottomNavDestination,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(destination.labelResId)
    val contentDesc = stringResource(destination.contentDescriptionResId)
    val iconColor = if (isSelected) Tq.Color.SignalMint else Tq.Color.FgTertiary

    Box(
        modifier = modifier
            .clickable(
                onClick = onClick,
                indication = ripple(bounded = true),
                interactionSource = remember { MutableInteractionSource() },
            )
            .semantics(mergeDescendants = true) {
                this.contentDescription = contentDesc
                this.role = Role.Tab
                this.selected = isSelected
            },
        contentAlignment = Alignment.Center,
    ) {
        // Active indicator pill + glow at the top of the cell.
        // The 6dp mint glow is one of two glows permitted in the design (DESIGN.md §10).
        if (isSelected) {
            NavIndicator(
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }

        // Icon + label column, vertically centred in the 48dp cell.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp), // per DESIGN.md §6.4
        ) {
            Icon(
                imageVector = if (isSelected) destination.filledIcon else destination.outlinedIcon,
                contentDescription = null, // semantics on the parent Box
                tint = iconColor,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = label,
                style = Tq.Type.MonoMicro,
                color = iconColor,
            )
        }
    }
}

// ─── Active indicator pill with 6dp mint glow ─────────────────────────────────

/**
 * Draws the 18×2dp mint indicator pill with a layered glow simulating 6dp of
 * mint blur. Uses concentric semi-transparent boxes — hardware-accelerated and
 * reliable across API levels.
 *
 * Positioned at the top of the active nav cell per DESIGN.md §6.4.
 */
@Composable
private fun NavIndicator(modifier: Modifier = Modifier) {
    // The container is wider/taller than the pill to give the glow room to breathe.
    Box(
        modifier = modifier
            .size(width = 30.dp, height = 14.dp),
    ) {
        // Outer glow (widest, most transparent)
        Box(
            modifier = Modifier
                .size(width = 30.dp, height = 10.dp)
                .align(Alignment.TopCenter)
                .clip(RoundedCornerShape(Tq.Radius.Pill))
                .background(Tq.Color.SignalMint.copy(alpha = 0.07f)),
        )
        // Middle glow
        Box(
            modifier = Modifier
                .size(width = 24.dp, height = 7.dp)
                .align(Alignment.TopCenter)
                .clip(RoundedCornerShape(Tq.Radius.Pill))
                .background(Tq.Color.SignalMint.copy(alpha = 0.14f)),
        )
        // Inner glow
        Box(
            modifier = Modifier
                .size(width = 20.dp, height = 4.dp)
                .align(Alignment.TopCenter)
                .clip(RoundedCornerShape(Tq.Radius.Pill))
                .background(Tq.Color.SignalMint.copy(alpha = 0.30f)),
        )
        // Solid indicator pill — 18×2dp per spec
        Box(
            modifier = Modifier
                .size(width = 18.dp, height = 2.dp)
                .align(Alignment.TopCenter)
                .clip(RoundedCornerShape(Tq.Radius.Pill))
                .background(Tq.Color.SignalMint),
        )
    }
}

// ─── Previews ─────────────────────────────────────────────────────────────────

@Preview(name = "AppNavigationBar — Dark", showBackground = true, backgroundColor = 0xFF1A1F22)
@Composable
private fun AppNavigationBarPreviewDark() {
    ToniqoTheme(useDarkTheme = true) {
        AppNavigationBar(
            destinations = bottomNavDestinations,
            currentDestination = null,
            onNavigate = {},
        )
    }
}

@Preview(name = "AppNavigationBar — Light", showBackground = true, backgroundColor = 0xFFF8F9FA)
@Composable
private fun AppNavigationBarPreviewLight() {
    ToniqoTheme(useDarkTheme = false) {
        AppNavigationBar(
            destinations = bottomNavDestinations,
            currentDestination = null,
            onNavigate = {},
        )
    }
}
