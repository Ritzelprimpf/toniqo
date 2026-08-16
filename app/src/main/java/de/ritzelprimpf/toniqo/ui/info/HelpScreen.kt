package de.ritzelprimpf.toniqo.ui.info

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import de.ritzelprimpf.toniqo.R
import de.ritzelprimpf.toniqo.ui.components.ScreenHeader
import de.ritzelprimpf.toniqo.ui.components.ToniqoCard
import de.ritzelprimpf.toniqo.ui.theme.Tq
import de.ritzelprimpf.toniqo.ui.theme.ToniqoTheme

/**
 * Help screen — static placeholder text per module.
 *
 * Per-module help content will be added when each module reaches its
 * final implementation phase.
 */
@Composable
fun HelpScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Tq.Color.BgBase)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.height(Tq.Sp.s2))

        ScreenHeader(
            title = stringResource(R.string.help_title),
            kicker = {
                Text(
                    text = stringResource(R.string.help_kicker),
                    style = Tq.Type.Kicker,
                    color = Tq.Color.FgTertiary,
                )
            },
            onBack = onBack,
            modifier = Modifier.padding(start = Tq.Sp.s3, end = Tq.Sp.s5),
        )

        Spacer(modifier = Modifier.height(Tq.Sp.s5))

        Column(
            modifier = Modifier.padding(horizontal = Tq.Sp.s5),
            verticalArrangement = Arrangement.spacedBy(Tq.Sp.s4),
        ) {
            HelpModuleCard(
                heading = stringResource(R.string.help_section_tuner),
                body = stringResource(R.string.help_placeholder_tuner),
            )
            HelpModuleCard(
                heading = stringResource(R.string.help_section_metronome),
                body = stringResource(R.string.help_placeholder_metronome),
            )
            HelpModuleCard(
                heading = stringResource(R.string.help_section_keyfinder),
                body = stringResource(R.string.help_placeholder_keyfinder),
            )
            HelpModuleCard(
                heading = stringResource(R.string.help_section_chordfinder),
                body = stringResource(R.string.help_placeholder_chordfinder),
            )
        }

        Spacer(modifier = Modifier.height(Tq.Sp.s5))
    }
}

@Composable
private fun HelpModuleCard(
    heading: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    ToniqoCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Tq.Sp.s2)) {
            Text(
                text = heading,
                style = Tq.Type.H2,
                color = Tq.Color.FgPrimary,
            )
            Text(
                text = body,
                style = Tq.Type.Body,
                color = Tq.Color.FgSecondary,
            )
        }
    }
}

// ─── Previews ─────────────────────────────────────────────────────────────────

@Preview(name = "HelpScreen — Dark", showBackground = true, backgroundColor = 0xFF1A1F22)
@Composable
private fun HelpScreenPreviewDark() {
    ToniqoTheme(useDarkTheme = true) { HelpScreen() }
}

@Preview(name = "HelpScreen — Light", showBackground = true, backgroundColor = 0xFFF8F9FA)
@Composable
private fun HelpScreenPreviewLight() {
    ToniqoTheme(useDarkTheme = false) { HelpScreen() }
}
