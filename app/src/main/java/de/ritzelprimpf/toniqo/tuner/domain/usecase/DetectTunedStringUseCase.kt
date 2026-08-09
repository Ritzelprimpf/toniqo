package de.ritzelprimpf.toniqo.tuner.domain.usecase

import de.ritzelprimpf.toniqo.audio.AudioCaptureSource
import de.ritzelprimpf.toniqo.audio.CaptureEvent
import de.ritzelprimpf.toniqo.audio.PitchDetector
import de.ritzelprimpf.toniqo.common.util.MusicTheory
import de.ritzelprimpf.toniqo.tuner.domain.model.TunerInput
import de.ritzelprimpf.toniqo.tuner.domain.model.TunerMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform
import javax.inject.Inject
import kotlin.math.abs

/**
 * The heart of the tuner pipeline.
 *
 * Collects raw audio from [AudioCaptureSource], runs [PitchDetector] on each buffer,
 * compares the result against the target note, and maintains a **sliding sustained-tone window**
 * to distinguish a genuinely in-tune string from a transient coincidence.
 *
 * ### Sustained-tone window
 *
 * An [ArrayDeque] of booleans of capacity [SUSTAINED_WINDOW_SIZE] (6). Each buffer cycle pushes
 * `true` (in tolerance) or `false` (out of tolerance or null detection). When the deque is full
 * and at least [SUSTAINED_MIN_IN_TOLERANCE] (5) entries are `true`, [DetectionEvent.Detection]
 * is emitted with `isSustainedInTune = true`. This means:
 * - A single glitch in six detections does **not** reset progress.
 * - Two consecutive glitches prevent the next detection from satisfying the threshold (since the
 *   window would have at most 4 of 6 `true` entries), which is the "two nulls reset" behaviour —
 *   it falls out of the math without an explicit reset.
 * - The window must be full before `IN_TUNE` can fire; the very first in-tolerance detection
 *   cannot trigger auto-advance.
 *
 * ### Displayed-value smoothing
 *
 * Very low strings (e.g. low E, ≈82 Hz) only fit ~3-4 wave periods inside a single analysis
 * buffer — right at the minimum [PitchDetector] needs for a reliable estimate (see
 * `DECISIONS.md`, "Phase 5.2: sample rate / buffer size"). That makes the raw per-buffer estimate
 * noticeably noisier for low strings than for higher ones, which reads as a jumpy needle. A
 * 2-reading moving average ([FREQUENCY_SMOOTHING_WINDOW_SIZE]) over the last *valid* detections
 * takes the edge off that jitter for the numeric/needle-facing fields
 * ([DetectionEvent.Detection.detectedFrequencyHz], [DetectionEvent.Detection.centsOff]) without
 * touching the sustained-tone window below, which keeps deciding in-tolerance / auto-advance from
 * the raw, unsmoothed per-frame estimate — so smoothing only calms the display, it never delays
 * or masks an out-of-tune reading. Note *identity* ([DetectionEvent.Detection.detectedNote], and
 * the chromatic-mode target note) is also resolved from the raw frequency, so the note letter
 * itself stays instantly responsive; only the fine numeric offset is damped.
 *
 * Deliberately kept to just 2 readings, not more: a 3-reading average was tried first and felt
 * noticeably laggy on real tone changes (each new string/note took an extra buffer cycle to fully
 * displace the old average) — see `DECISIONS.md`. 2 readings converges to a step change within a
 * single buffer cycle (~93ms) instead of two, while still damping single-frame noise spikes.
 *
 * ### Statefulness
 *
 * Each call to [execute] returns a **fresh** flow with a **fresh** window (and a fresh smoothing
 * buffer). No state leaks between invocations. The ViewModel restarts the use case whenever the
 * target changes (new string, new preset, mode switch).
 *
 * @property audioCaptureSource Emits raw audio buffers (and control events) as a Flow.
 * @property pitchDetector Converts a buffer into a fundamental frequency (or `null`).
 */
class DetectTunedStringUseCase @Inject constructor(
    private val audioCaptureSource: AudioCaptureSource,
    private val pitchDetector: PitchDetector,
) {

    /**
     * Returns a flow of detection events for the given [input].
     *
     * The flow runs the microphone, runs the pitch detector, applies the sustained-tone state
     * machine, and emits events the ViewModel can map to UI state. The flow's lifetime is bound
     * to the collector's scope — cancelling the scope stops audio capture and releases all
     * audio resources.
     *
     * @param input Tuning configuration for this session (mode, target note, reference pitch).
     * @return A cold flow of [DetectionEvent]s.
     */
    fun execute(input: TunerInput): Flow<DetectionEvent> {
        val window = ArrayDeque<Boolean>(SUSTAINED_WINDOW_SIZE)
        val frequencyWindow = ArrayDeque<Double>(FREQUENCY_SMOOTHING_WINDOW_SIZE)

        return audioCaptureSource.samples()
            .transform { captureEvent ->
                when (captureEvent) {
                    is CaptureEvent.PermissionDenied -> {
                        emit(DetectionEvent.PermissionDenied)
                    }
                    is CaptureEvent.Listening -> {
                        emit(DetectionEvent.Listening)
                    }
                    is CaptureEvent.Failed -> {
                        emit(DetectionEvent.Failed(captureEvent.reason))
                    }
                    is CaptureEvent.Samples -> {
                        val detectedHz =
                            pitchDetector.detectPitch(captureEvent.buffer, SAMPLE_RATE_HZ)

                        if (detectedHz == null) {
                            // Null counts as out-of-tolerance in the window; no event emitted.
                            pushToWindow(window, inTolerance = false)
                            return@transform
                        }

                        pushToFrequencyWindow(frequencyWindow, detectedHz)
                        val smoothedHz = frequencyWindow.average()

                        val targetNote = when (input.mode) {
                            TunerMode.PRESET -> {
                                checkNotNull(input.targetNote) {
                                    "targetNote must not be null in PRESET mode"
                                }
                            }
                            TunerMode.CHROMATIC -> {
                                val resolved = MusicTheory.frequencyToNote(
                                    detectedHz,
                                    input.referencePitchHz,
                                )
                                // If out of musical range, skip this frame entirely.
                                resolved ?: return@transform
                            }
                        }

                        val targetHz = targetNote.frequencyHz(input.referencePitchHz)

                        // Raw (unsmoothed) cents drive the sustained-tone window: in-tolerance /
                        // auto-advance decisions must never be delayed or masked by smoothing.
                        val centsOff = MusicTheory.centsBetween(targetHz, detectedHz)
                        val inTolerance = abs(centsOff) <= IN_TUNE_TOLERANCE_CENTS

                        pushToWindow(window, inTolerance)

                        val isSustained = window.size == SUSTAINED_WINDOW_SIZE &&
                            window.count { it } >= SUSTAINED_MIN_IN_TOLERANCE

                        // Note identity stays tied to the raw frequency so the note letter itself
                        // switches instantly; only the numeric offset below is smoothed.
                        val detectedNote = MusicTheory.frequencyToNote(
                            detectedHz,
                            input.referencePitchHz,
                        )
                        val smoothedCentsOff = MusicTheory.centsBetween(targetHz, smoothedHz)

                        emit(
                            DetectionEvent.Detection(
                                detectedFrequencyHz = smoothedHz,
                                detectedNote = detectedNote,
                                targetNote = targetNote,
                                targetFrequencyHz = targetHz,
                                centsOff = smoothedCentsOff,
                                isSustainedInTune = isSustained,
                            ),
                        )
                    }
                }
            }
    }

    private fun pushToWindow(window: ArrayDeque<Boolean>, inTolerance: Boolean) {
        if (window.size == SUSTAINED_WINDOW_SIZE) window.removeFirst()
        window.addLast(inTolerance)
    }

    private fun pushToFrequencyWindow(window: ArrayDeque<Double>, frequencyHz: Double) {
        if (window.size == FREQUENCY_SMOOTHING_WINDOW_SIZE) window.removeFirst()
        window.addLast(frequencyHz)
    }

    companion object {
        /** Number of consecutive pitch detections tracked by the sustained-tone window. */
        const val SUSTAINED_WINDOW_SIZE = 6

        /**
         * Minimum number of in-tolerance detections within the window required to declare a
         * string sustained-in-tune. Allows one glitch per window without resetting progress.
         */
        const val SUSTAINED_MIN_IN_TOLERANCE = 5

        /** Cents range around the target within which a detection is considered in tolerance. */
        const val IN_TUNE_TOLERANCE_CENTS = 5.0

        /**
         * Number of recent *valid* detections averaged into the displayed frequency/cents.
         * Deliberately light-touch (2, not more) — see the class kdoc "Displayed-value smoothing"
         * section for why a larger window was tried and reverted for feeling laggy.
         */
        const val FREQUENCY_SMOOTHING_WINDOW_SIZE = 2

        /** Sample rate to pass to the pitch detector; matches the capture parameters from Phase 5.2. */
        private const val SAMPLE_RATE_HZ = 44_100
    }
}
