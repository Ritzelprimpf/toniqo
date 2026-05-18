package de.ritzelprimpf.toniqo.tuner.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import de.ritzelprimpf.toniqo.R
import de.ritzelprimpf.toniqo.ui.theme.Tq
import de.ritzelprimpf.toniqo.ui.theme.ToniqoTheme

/**
 * Guitar Tuner placeholder screen.
 *
 * Phase 4: layout shell only — no pitch detection, no ViewModel, no audio.
 * Full implementation in Phase 5.
 */
@Composable
fun TunerScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Tq.Color.BgBase)
            .padding(horizontal = Tq.Sp.s5),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // TODO: Replace with custom `tuner` icon from DESIGN.md §7
        Icon(
            imageVector = Icons.Outlined.GraphicEq,
            contentDescription = null,
            tint = Tq.Color.FgTertiary,
            modifier = Modifier.size(Tq.Sp.s8),
        )
        Spacer(modifier = Modifier.height(Tq.Sp.s4))
        Text(
            text = stringResource(R.string.tuner_title),
            style = Tq.Type.H1,
            color = Tq.Color.FgPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Tq.Sp.s2))
        Text(
            text = stringResource(R.string.tuner_description),
            style = Tq.Type.Body,
            color = Tq.Color.FgSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

// ─── Previews ─────────────────────────────────────────────────────────────────

@Preview(name = "TunerScreen — Dark", showBackground = true, backgroundColor = 0xFF1A1F22)
@Composable
private fun TunerScreenPreviewDark() {
    ToniqoTheme(useDarkTheme = true) { TunerScreen() }
}

@Preview(name = "TunerScreen — Light", showBackground = true, backgroundColor = 0xFFF8F9FA)
@Composable
private fun TunerScreenPreviewLight() {
    ToniqoTheme(useDarkTheme = false) { TunerScreen() }
}
