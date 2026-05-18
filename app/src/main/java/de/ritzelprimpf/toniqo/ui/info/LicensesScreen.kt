package de.ritzelprimpf.toniqo.ui.info

import androidx.compose.foundation.background
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
import de.ritzelprimpf.toniqo.ui.components.ToniqoCard
import de.ritzelprimpf.toniqo.ui.theme.Tq
import de.ritzelprimpf.toniqo.ui.theme.ToniqoTheme

/**
 * Open Source Licenses screen — static placeholder.
 *
 * License collection from Gradle dependencies will be wired in a later phase
 * (e.g., using the Play Services OSS Licenses plugin or manual attribution).
 */
@Composable
fun LicensesScreen(
    onBack: () -> Unit = {},
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
            text = stringResource(R.string.licenses_title),
            style = Tq.Type.H1,
            color = Tq.Color.FgPrimary,
        )

        Spacer(modifier = Modifier.height(Tq.Sp.s5))

        ToniqoCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.licenses_placeholder),
                style = Tq.Type.Body,
                color = Tq.Color.FgSecondary,
            )
        }

        Spacer(modifier = Modifier.height(Tq.Sp.s5))
    }
}

// ─── Previews ─────────────────────────────────────────────────────────────────

@Preview(name = "LicensesScreen — Dark", showBackground = true, backgroundColor = 0xFF1A1F22)
@Composable
private fun LicensesScreenPreviewDark() {
    ToniqoTheme(useDarkTheme = true) { LicensesScreen() }
}

@Preview(name = "LicensesScreen — Light", showBackground = true, backgroundColor = 0xFFF8F9FA)
@Composable
private fun LicensesScreenPreviewLight() {
    ToniqoTheme(useDarkTheme = false) { LicensesScreen() }
}
