package de.ritzelprimpf.toniqo.tuner.data

import de.ritzelprimpf.toniqo.tuner.domain.repository.PitchDetector
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stub implementation of [PitchDetector] backed by the YIN algorithm.
 *
 * The interface exists in Phase 2 so the rest of the tuner stack can compile; the actual YIN
 * implementation lands in Phase 5.2 and the choice of algorithm is recorded in `DECISIONS.md`
 * at that point.
 */
@Singleton
class YinPitchDetector @Inject constructor() : PitchDetector {

    /**
     * Stub. Phase 5.2 fills in the YIN-based fundamental-frequency estimate.
     *
     * Throws [NotImplementedError] in Phase 2.
     */
    override fun detectPitch(audioBuffer: FloatArray, sampleRateHz: Int): Double? =
        TODO("Not yet implemented")
}
