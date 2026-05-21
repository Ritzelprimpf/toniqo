package de.ritzelprimpf.toniqo.tuner.presentation.mapping

import androidx.compose.ui.graphics.Color
import de.ritzelprimpf.toniqo.tuner.domain.model.TuningStatus
import de.ritzelprimpf.toniqo.ui.theme.Tq

/**
 * Maps a [TuningStatus] to its semantic signal colour per `DESIGN.md §2.4`.
 *
 * This is the **single source of truth** for the cents → colour mapping in the tuner UI.
 * Never hardcode a signal colour at the call site; always call this function.
 *
 * Mapping (§2.4):
 * ```
 * IN_TUNE, ALL_STRINGS_TUNED → signal.mint
 * FLAT                       → signal.cyan
 * SHARP                      → signal.amber
 * LISTENING, IDLE, PERMISSION_DENIED, CAPTURE_FAILED → fg.quaternary
 * ```
 */
fun TuningStatus.toSignalColor(): Color = when (this) {
    TuningStatus.IN_TUNE,
    TuningStatus.ALL_STRINGS_TUNED -> Tq.Color.SignalMint
    TuningStatus.FLAT -> Tq.Color.SignalCyan
    TuningStatus.SHARP -> Tq.Color.SignalAmber
    TuningStatus.LISTENING,
    TuningStatus.IDLE,
    TuningStatus.PERMISSION_DENIED,
    TuningStatus.CAPTURE_FAILED -> Tq.Color.FgQuaternary
}
