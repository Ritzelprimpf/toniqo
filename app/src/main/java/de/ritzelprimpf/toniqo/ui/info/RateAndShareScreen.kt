package de.ritzelprimpf.toniqo.ui.info

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.ritzelprimpf.toniqo.R
import de.ritzelprimpf.toniqo.ui.theme.Tq
import de.ritzelprimpf.toniqo.ui.theme.ToniqoTheme
import kotlinx.coroutines.launch

/**
 * Rate & Share screen.
 *
 * Phase 4: stub implementation. Both buttons show a [SnackbarHostState] message
 * describing what the action will do when fully wired (Phase 5+).
 *
 * Buttons use inline `btn.default` styling per DESIGN.md §6.1 — a full
 * `ToniqoButton` component is not built this phase.
 */
@Composable
fun RateAndShareScreen(
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val rateMessage = stringResource(R.string.snackbar_rate)
    val shareMessage = stringResource(R.string.snackbar_share)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Tq.Color.BgBase)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Tq.Sp.s5),
    ) {
        Spacer(modifier = Modifier.height(Tq.Sp.s5))

        Text(
            text = stringResource(R.string.rate_share_title),
            style = Tq.Type.H1,
            color = Tq.Color.FgPrimary,
        )

        Spacer(modifier = Modifier.height(Tq.Sp.s5))

        Column(verticalArrangement = Arrangement.spacedBy(Tq.Sp.s4)) {
            // btn.default style — inline per DESIGN.md §6.1 (ToniqoButton not built yet)
            DefaultStyledButton(
                label = stringResource(R.string.rate_app_label),
                onClick = {
                    scope.launch { snackbarHostState.showSnackbar(rateMessage) }
                },
            )
            DefaultStyledButton(
                label = stringResource(R.string.share_app_label),
                onClick = {
                    scope.launch { snackbarHostState.showSnackbar(shareMessage) }
                },
            )
        }

        Spacer(modifier = Modifier.height(Tq.Sp.s5))
    }
}

// ─── Inline btn.default style ─────────────────────────────────────────────────

/** Temporary inline `btn.default` per DESIGN.md §6.1. Replace with ToniqoButton when built. */
@Composable
private fun DefaultStyledButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(Tq.Sp.s10) // 40dp per spec
            .border(
                width = 1.dp,
                color = Tq.Color.LineFaint,
                shape = RoundedCornerShape(Tq.Radius.Pill),
            ),
        shape = RoundedCornerShape(Tq.Radius.Pill),
        colors = ButtonDefaults.buttonColors(
            containerColor = Tq.Color.BgElev2,
            contentColor = Tq.Color.FgPrimary,
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
        ),
    ) {
        Text(
            text = label,
            style = Tq.Type.BodyStrong,
            color = Tq.Color.FgPrimary,
        )
    }
}

// ─── Previews ─────────────────────────────────────────────────────────────────

@Preview(name = "RateAndShareScreen — Dark", showBackground = true, backgroundColor = 0xFF1A1F22)
@Composable
private fun RateAndShareScreenPreviewDark() {
    ToniqoTheme(useDarkTheme = true) {
        RateAndShareScreen(snackbarHostState = remember { SnackbarHostState() })
    }
}

@Preview(name = "RateAndShareScreen — Light", showBackground = true, backgroundColor = 0xFFF8F9FA)
@Composable
private fun RateAndShareScreenPreviewLight() {
    ToniqoTheme(useDarkTheme = false) {
        RateAndShareScreen(snackbarHostState = remember { SnackbarHostState() })
    }
}
