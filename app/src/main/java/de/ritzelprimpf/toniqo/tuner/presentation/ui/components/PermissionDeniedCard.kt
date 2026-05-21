package de.ritzelprimpf.toniqo.tuner.presentation.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.ritzelprimpf.toniqo.R
import de.ritzelprimpf.toniqo.ui.components.ToniqoCard
import de.ritzelprimpf.toniqo.ui.theme.Tq

/**
 * Card shown in place of the ReadoutWell when `RECORD_AUDIO` is not granted.
 *
 * Specification (DESIGN.md §8.1 "Permission-denied state"):
 * - Card: `bg.elev1`, hairline border, `r.lg`, `sp.4` padding — provided by [ToniqoCard].
 * - 28dp mic icon with a diagonal slash overlay (§7: no dedicated mic-slash icon in the set).
 * - `H2` heading, `body` explanation (centred, max 3 lines), `btn.primary` 40dp button.
 *
 * The caller decides whether "Grant access" triggers a permission request or opens system
 * app settings — the card's copy stays the same in both cases (DESIGN.md §8.1).
 *
 * @param onGrantAccess Invoked when the user taps "Grant access".
 */
@Composable
fun PermissionDeniedCard(
    onGrantAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val slashColor: Color = Tq.Color.FgSecondary

    ToniqoCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Tq.Sp.s3),
        ) {
            Spacer(Modifier.height(Tq.Sp.s2))

            Box(modifier = Modifier.size(28.dp)) {  // 28dp per §7 icon sizes
                Icon(
                    imageVector = Icons.Outlined.Mic,
                    contentDescription = null,
                    tint = Tq.Color.FgSecondary,
                    modifier = Modifier.size(28.dp),
                )
                Canvas(modifier = Modifier.size(28.dp)) {
                    drawLine(
                        color = slashColor,
                        start = Offset(size.width * 0.75f, size.height * 0.1f),
                        end = Offset(size.width * 0.25f, size.height * 0.9f),
                        strokeWidth = 1.25.dp.toPx(),  // §7: 1.25dp stroke weight
                        cap = StrokeCap.Round,
                    )
                }
            }

            Text(
                text = stringResource(R.string.tuner_permission_heading),
                style = Tq.Type.H2,
                color = Tq.Color.FgPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.tuner_permission_body),
                style = Tq.Type.Body,
                color = Tq.Color.FgSecondary,
                textAlign = TextAlign.Center,
                maxLines = 3,
            )

            Button(
                onClick = onGrantAccess,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Tq.Color.SignalMint,
                    contentColor = Tq.Color.BgBase,
                ),
            ) {
                Text(
                    text = stringResource(R.string.tuner_permission_button),
                    style = Tq.Type.BodyStrong,
                )
            }

            Spacer(Modifier.height(Tq.Sp.s2))
        }
    }
}
