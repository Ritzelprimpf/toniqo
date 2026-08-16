package de.ritzelprimpf.toniqo.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import de.ritzelprimpf.toniqo.R
import de.ritzelprimpf.toniqo.ui.theme.Tq

/**
 * Shared top-of-screen header: a kicker line, [Tq.Sp.s2] below it an [Tq.Type.H1] title (with an
 * optional back arrow inline on the same line, leading it), and an optional trailing action (e.g.
 * an info or settings icon button) floating independently at the header's top-end corner.
 *
 * Used both by the four top-level tab screens (no [onBack]) and by every back-navigable sub-page
 * (Chord Voicings, and the Info section's Help/Licenses/Bug Report/Feature Request screens) — see
 * the 2026-08-15 DECISIONS.md entries.
 *
 * The trailing action is deliberately a slot the caller positions itself (typically via
 * `Modifier.align(Alignment.TopEnd)` inside [trailingAction]'s [BoxScope] receiver), rather than
 * a fixed icon this composable renders — every screen's action button differs in icon, size, and
 * tap-target (e.g. the Tuner's 40dp icon-round settings button vs. Key Finder/Chord Finder's
 * default-sized info button per DESIGN.md), so only the *structural* skeleton (kicker above
 * title, action floated independently so its touch target never inflates the kicker line and
 * pushes the title down) is shared.
 *
 * [onBack], unlike the trailing action, is not a slot: every back button in the app renders
 * identically (same icon, tint, content description), so standardizing it here removes real
 * duplication rather than forcing an artificial one-size-fits-all look the way a hardcoded
 * trailing action would.
 *
 * The kicker itself is also a slot, not a plain string: Metronome's kicker carries a pulsing dot
 * plus dynamic running/stopped text, and the Tuner's carries a mic-active dot plus the reference
 * pitch label, neither of which is a bare [Tq.Type.Kicker] [Text] like Key Finder/Chord Finder's.
 *
 * @param title The H1 title text.
 * @param kicker The kicker line's content — usually a single [Tq.Type.Kicker]-styled [Text], but
 *   free to render leading indicators/dots as several screens do.
 * @param onBack When non-null, renders a standard back arrow inline before [title], on the same
 *   line, and invokes this when tapped. `null` renders no back arrow (the four tab screens).
 * @param trailingAction Optional trailing action (e.g. an icon button), floated independently of
 *   the kicker/title stack. `null` renders no trailing action (e.g. Metronome).
 */
@Composable
fun ScreenHeader(
    title: String,
    kicker: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    trailingAction: (@Composable BoxScope.() -> Unit)? = null,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Column {
            kicker()
            Spacer(Modifier.height(Tq.Sp.s2))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.common_cd_back),
                            tint = Tq.Color.FgSecondary,
                        )
                    }
                    Spacer(Modifier.width(Tq.Sp.s1))
                }
                Text(
                    text = title,
                    style = Tq.Type.H1,
                    color = Tq.Color.FgPrimary,
                )
            }
        }
        trailingAction?.invoke(this)
    }
}
