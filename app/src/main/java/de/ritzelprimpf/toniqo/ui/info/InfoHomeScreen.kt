package de.ritzelprimpf.toniqo.ui.info

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import de.ritzelprimpf.toniqo.R
import de.ritzelprimpf.toniqo.ui.components.ToniqoCard
import de.ritzelprimpf.toniqo.ui.navigation.Routes
import de.ritzelprimpf.toniqo.ui.theme.Tq
import de.ritzelprimpf.toniqo.ui.theme.ToniqoTheme

/**
 * Info section home screen.
 *
 * Displays a list of card rows navigating to each sub-screen. Each row has a
 * leading 20dp icon, a title in [Tq.Type.BodyStrong], and a trailing
 * `chevron-right` icon, per DESIGN.md §8.5.
 *
 * The screen header reads "Info" in [Tq.Type.H1] (the bottom-nav label is
 * the short kicker "MORE" — see Phase4-PLAN.md "Naming note").
 */
@Composable
fun InfoHomeScreen(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Tq.Color.BgBase)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Tq.Sp.s5),
    ) {
        Spacer(modifier = Modifier.height(Tq.Sp.s5))

        Text(
            text = stringResource(R.string.info_title),
            style = Tq.Type.H1,
            color = Tq.Color.FgPrimary,
        )

        Spacer(modifier = Modifier.height(Tq.Sp.s5))

        // Card containing all nav rows — separated by the divider-free spacing
        // pattern (single card with stacked rows, per the §8.5 card-list pattern).
        ToniqoCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(Tq.Sp.s0)) {
                InfoNavRow(
                    // TODO: Replace with custom `info` icon from DESIGN.md §7
                    icon = Icons.Outlined.HelpOutline,
                    label = stringResource(R.string.info_item_help),
                    onClick = { onNavigate(Routes.HELP) },
                )
                InfoNavRow(
                    // TODO: Replace with custom `info` icon from DESIGN.md §7
                    icon = Icons.Outlined.Policy,
                    label = stringResource(R.string.info_item_privacy),
                    onClick = { onNavigate(Routes.PRIVACY) },
                )
                InfoNavRow(
                    // TODO: Replace with custom `info` icon from DESIGN.md §7
                    icon = Icons.Outlined.Info,
                    label = stringResource(R.string.info_item_licenses),
                    onClick = { onNavigate(Routes.LICENSES) },
                )
                InfoNavRow(
                    // TODO: Replace with custom `info` icon from DESIGN.md §7
                    icon = Icons.Outlined.Star,
                    label = stringResource(R.string.info_item_rate),
                    onClick = { onNavigate(Routes.RATE_AND_SHARE) },
                )
                InfoNavRow(
                    // TODO: Replace with custom `info` icon from DESIGN.md §7
                    icon = Icons.Outlined.Share,
                    label = stringResource(R.string.info_item_share),
                    onClick = { onNavigate(Routes.RATE_AND_SHARE) },
                    isLast = true,
                )
            }
        }

        Spacer(modifier = Modifier.height(Tq.Sp.s5))
    }
}

// ─── Row component ────────────────────────────────────────────────────────────

@Composable
private fun InfoNavRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isLast: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Tq.Sp.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Tq.Sp.s3),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Tq.Color.FgSecondary,
            modifier = Modifier.size(Tq.Sp.s5), // 20dp icon size per DESIGN.md §8.5
        )
        Text(
            text = label,
            style = Tq.Type.BodyStrong,
            color = Tq.Color.FgPrimary,
            modifier = Modifier.weight(1f),
        )
        Icon(
            // TODO: Replace with custom `chevron-right` icon from DESIGN.md §7
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = Tq.Color.FgTertiary,
            modifier = Modifier.size(Tq.Sp.s4), // 16dp (closest Tq.Sp to the 18dp spec value)
        )
    }
}

// ─── Previews ─────────────────────────────────────────────────────────────────

@Preview(name = "InfoHomeScreen — Dark", showBackground = true, backgroundColor = 0xFF1A1F22)
@Composable
private fun InfoHomeScreenPreviewDark() {
    ToniqoTheme(useDarkTheme = true) {
        InfoHomeScreen(onNavigate = {})
    }
}

@Preview(name = "InfoHomeScreen — Light", showBackground = true, backgroundColor = 0xFFF8F9FA)
@Composable
private fun InfoHomeScreenPreviewLight() {
    ToniqoTheme(useDarkTheme = false) {
        InfoHomeScreen(onNavigate = {})
    }
}
