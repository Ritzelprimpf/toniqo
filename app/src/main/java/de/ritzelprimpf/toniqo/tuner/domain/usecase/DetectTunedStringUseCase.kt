package de.ritzelprimpf.toniqo.tuner.domain.usecase

import de.ritzelprimpf.toniqo.common.model.Note
import de.ritzelprimpf.toniqo.tuner.domain.model.TuningStatus
import de.ritzelprimpf.toniqo.common.util.PitchDetector
import javax.inject.Inject

/**
 * Coordinates pitch detection with target-frequency comparison to produce a [Result] describing
 * how close the player is to the target note.
 *
 * The use case is mode-agnostic in its public shape — the caller passes whichever target note it
 * has chosen (the next string in sequential mode, or the nearest string in chromatic mode). The
 * mode-selection logic itself lives in the ViewModel.
 *
 * @property pitchDetector The pitch-detection algorithm. Injected by Hilt; the binding is locked
 *   in `DECISIONS.md` during Phase 5.1.
 */
class DetectTunedStringUseCase @Inject constructor(
    private val pitchDetector: PitchDetector,
) {

    /**
     * Analyses [audioBuffer] against [targetNote] and returns the resulting [Result].
     *
     * @param audioBuffer The captured audio samples, mono, normalised floats.
     * @param sampleRateHz The sample rate of [audioBuffer] in Hertz.
     * @param targetNote The note the user is trying to match.
     * @param referencePitchHz The reference frequency of A4. Defaults to the international
     *   standard (440 Hz); pass 432 Hz for the alternative tuning.
     * @return A [Result] containing the detected frequency, the cents offset from [targetNote],
     *   and the resulting [TuningStatus]; or `null` if no usable pitch was detected.
     *
     * Throws [NotImplementedError] in Phase 2.
     */
    operator fun invoke(
        audioBuffer: FloatArray,
        sampleRateHz: Int,
        targetNote: Note,
        referencePitchHz: Double = Note.DEFAULT_REFERENCE_PITCH_HZ,
    ): Result? = TODO("Not yet implemented")

    /**
     * The outcome of a single pitch-detection cycle.
     *
     * @property detectedFrequencyHz The fundamental frequency the detector returned.
     * @property centsOffTarget Signed cents offset from the target note. Negative is flat;
     *   positive is sharp.
     * @property status The corresponding [TuningStatus].
     */
    data class Result(
        val detectedFrequencyHz: Double,
        val centsOffTarget: Float,
        val status: TuningStatus,
    )
}
