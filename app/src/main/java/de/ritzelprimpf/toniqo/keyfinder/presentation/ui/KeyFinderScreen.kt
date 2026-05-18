package de.ritzelprimpf.toniqo.keyfinder.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MusicNote
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
 * Key Finder placeholder screen.
 *
 * Phase 4: layout shell only — no note-matching logic, no ViewModel.
 * Full implementation in a later phase.
 */
@Composable
fun KeyFinderScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Tq.Color.BgBase)
            .padding(horizontal = Tq.Sp.s5),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // TODO: Replace with custom `key` icon from DESIGN.md §7
        Icon(
            imageVector = Icons.Outlined.MusicNote,
            contentDescription = null,
            tint = Tq.Color.FgTertiary,
            modifier = Modifier.size(Tq.Sp.s8),
        )
        Spacer(modifier = Modifier.height(Tq.Sp.s4))
        Text(
            text = stringResource(R.string.keyfinder_title),
            style = Tq.Type.H1,
            color = Tq.Color.FgPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Tq.Sp.s2))
        Text(
            text = stringResource(R.string.keyfinder_description),
            style = Tq.Type.Body,
            color = Tq.Color.FgSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

// ─── Previews ─────────────────────────────────────────────────────────────────

@Preview(name = "KeyFinderScreen — Dark", showBackground = true, backgroundColor = 0xFF1A1F22)
@Composable
private fun KeyFinderScreenPreviewDark() {
    ToniqoTheme(useDarkTheme = true) { KeyFinderScreen() }
}

@Preview(name = "KeyFinderScreen — Light", showBackground = true, backgroundColor = 0xFFF8F9FA)
@Composable
private fun KeyFinderScreenPreviewLight() {
    ToniqoTheme(useDarkTheme = false) { KeyFinderScreen() }
}
