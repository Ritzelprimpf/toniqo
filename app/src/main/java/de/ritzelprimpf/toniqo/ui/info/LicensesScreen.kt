package de.ritzelprimpf.toniqo.ui.info

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import de.ritzelprimpf.toniqo.R
import de.ritzelprimpf.toniqo.ui.components.ToniqoCard
import de.ritzelprimpf.toniqo.ui.theme.Tq
import de.ritzelprimpf.toniqo.ui.theme.ToniqoTheme

private const val LICENSE_ASSET_PATH = "LICENSE.txt"

/** Reads the project's MIT license text, bundled at [LICENSE_ASSET_PATH] (mirrors the root `LICENSE` file). */
private fun readLicenseText(context: Context): String =
    context.assets.open(LICENSE_ASSET_PATH).bufferedReader().use { it.readText() }

/**
 * Open Source Licenses screen — displays the project's own MIT license text,
 * bundled as an asset so it stays in sync with the root `LICENSE` file.
 *
 * Third-party dependency license attribution is not yet collected here; see
 * `DECISIONS.md` if that scope is added later (e.g. via the Play Services OSS
 * Licenses plugin).
 */
@Composable
fun LicensesScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val licenseText = remember { readLicenseText(context) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Tq.Color.BgBase)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Tq.Sp.s5),
    ) {
        Spacer(modifier = Modifier.height(Tq.Sp.s2))

        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.info_cd_back),
                tint = Tq.Color.FgSecondary,
            )
        }

        Text(
            text = stringResource(R.string.licenses_title),
            style = Tq.Type.H1,
            color = Tq.Color.FgPrimary,
        )

        Spacer(modifier = Modifier.height(Tq.Sp.s5))

        ToniqoCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = licenseText,
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
