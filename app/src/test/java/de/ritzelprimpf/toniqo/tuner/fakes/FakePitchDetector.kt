package de.ritzelprimpf.toniqo.tuner.fakes

import de.ritzelprimpf.toniqo.common.util.PitchDetector

/**
 * Test double for [PitchDetector].
 *
 * Returns frequencies from a pre-programmed list in order. Once the list is exhausted every
 * subsequent call returns `null`. Supply `null` entries to simulate frames where no pitch
 * was detected.
 */
class FakePitchDetector(
    private val frequencies: List<Double?>,
) : PitchDetector {

    private var callIndex = 0

    override fun detectPitch(audioBuffer: FloatArray, sampleRateHz: Int): Double? {
        if (callIndex >= frequencies.size) return null
        return frequencies[callIndex++]
    }
}
