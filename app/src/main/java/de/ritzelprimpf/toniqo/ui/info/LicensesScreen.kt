package de.ritzelprimpf.toniqo.ui.info

import android.content.Context
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import de.ritzelprimpf.toniqo.R
import de.ritzelprimpf.toniqo.ui.components.ScreenHeader
import de.ritzelprimpf.toniqo.ui.components.ToniqoCard
import de.ritzelprimpf.toniqo.ui.theme.Tq
import de.ritzelprimpf.toniqo.ui.theme.ToniqoTheme

private const val LICENSE_ASSET_PATH = "LICENSE.txt"
private const val THIRD_PARTY_LICENSES_ASSET_PATH = "THIRD_PARTY_LICENSES.txt"

/** Reads the project's MIT license text, bundled at [LICENSE_ASSET_PATH] (mirrors the root `LICENSE` file). */
private fun readLicenseText(context: Context): String =
    context.assets.open(LICENSE_ASSET_PATH).bufferedReader().use { it.readText() }

/** Reads the bundled third-party (Apache-2.0) attribution notice at [THIRD_PARTY_LICENSES_ASSET_PATH]. */
private fun readThirdPartyLicenseText(context: Context): String =
    context.assets.open(THIRD_PARTY_LICENSES_ASSET_PATH).bufferedReader().use { it.readText() }

/**
 * Open Source Licenses screen — displays the project's own MIT license text plus a bundled
 * attribution notice for the third-party (Apache-2.0) libraries the app ships, both read from
 * assets so they stay in sync with the root `LICENSE` file / `THIRD_PARTY_LICENSES.txt`.
 */
@Composable
fun LicensesScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val licenseText = remember { readLicenseText(context) }
    val thirdPartyLicenseText = remember { readThirdPartyLicenseText(context) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Tq.Color.BgBase)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.height(Tq.Sp.s2))

        ScreenHeader(
            title = stringResource(R.string.licenses_title),
            kicker = {
                Text(
                    text = stringResource(R.string.licenses_kicker),
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
            LicenseCard(
                heading = stringResource(R.string.licenses_section_toniqo),
                body = licenseText,
            )
            LicenseCard(
                heading = stringResource(R.string.licenses_section_third_party),
                body = thirdPartyLicenseText,
            )
        }

        Spacer(modifier = Modifier.height(Tq.Sp.s5))
    }
}

@Composable
private fun LicenseCard(
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
