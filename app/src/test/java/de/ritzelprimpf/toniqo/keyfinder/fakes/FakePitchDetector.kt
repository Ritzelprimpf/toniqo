package de.ritzelprimpf.toniqo.keyfinder.fakes

import de.ritzelprimpf.toniqo.audio.PitchDetector

/**
 * Test double for [PitchDetector] used in Key Finder tests.
 *
 * Returns frequencies from a pre-programmed list in order. Once exhausted every subsequent
 * call returns `null`. Supply `null` entries explicitly to simulate silence frames.
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
