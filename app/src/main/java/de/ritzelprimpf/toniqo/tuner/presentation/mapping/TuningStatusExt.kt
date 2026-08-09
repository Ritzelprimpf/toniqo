package de.ritzelprimpf.toniqo.tuner.presentation.mapping

import androidx.compose.ui.graphics.Color
import de.ritzelprimpf.toniqo.tuner.domain.model.TuningStatus
import de.ritzelprimpf.toniqo.ui.theme.TqPalette

/**
 * Maps a [TuningStatus] to its semantic signal colour per `DESIGN.md §2.4`.
 *
 * This is the **single source of truth** for the cents → colour mapping in the tuner UI.
 * Never hardcode a signal colour at the call site; always call this function.
 *
 * Deliberately a plain (non-`@Composable`) function, so it stays covered by fast JVM unit tests
 * ([de.ritzelprimpf.toniqo.tuner.presentation.mapping.TuningStatusExtTest]) instead of requiring
 * a Compose UI test host. [palette] is the theme-reactive value the caller reads via
 * `Tq.Palette` in composable scope and passes in explicitly — see [TqPalette].
 *
 * Mapping (§2.4):
 * ```
 * IN_TUNE, ALL_STRINGS_TUNED → signal.mint
 * FLAT                       → signal.cyan
 * SHARP                      → signal.amber
 * LISTENING, IDLE, PERMISSION_DENIED, CAPTURE_FAILED → fg.quaternary
 * ```
 */
fun TuningStatus.toSignalColor(palette: TqPalette): Color = when (this) {
    TuningStatus.IN_TUNE,
    TuningStatus.ALL_STRINGS_TUNED -> palette.signalMint
    TuningStatus.FLAT -> palette.signalCyan
    TuningStatus.SHARP -> palette.signalAmber
    TuningStatus.LISTENING,
    TuningStatus.IDLE,
    TuningStatus.PERMISSION_DENIED,
    TuningStatus.CAPTURE_FAILED -> palette.fgQuaternary
}
