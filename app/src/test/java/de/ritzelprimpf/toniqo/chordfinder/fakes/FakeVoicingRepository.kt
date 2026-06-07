package de.ritzelprimpf.toniqo.chordfinder.fakes

import de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordKey
import de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordToneRole
import de.ritzelprimpf.toniqo.chordfinder.domain.model.FretMark
import de.ritzelprimpf.toniqo.chordfinder.domain.model.Voicing
import de.ritzelprimpf.toniqo.chordfinder.domain.repository.VoicingLookupResult
import de.ritzelprimpf.toniqo.chordfinder.domain.repository.VoicingRepository
import de.ritzelprimpf.toniqo.common.model.GuitarTuning

/**
 * Configurable test double for [VoicingRepository].
 *
 * [resultsMap] maps a [GuitarTuning] to the [VoicingLookupResult] to return for that tuning.
 * Any tuning not in the map returns [defaultResult] (defaults to `Standard` with an empty list).
 *
 * Tests configure the map via the [on] helper to express per-tuning behaviour concisely.
 */
class FakeVoicingRepository : VoicingRepository {

    private val resultsMap: MutableMap<GuitarTuning, VoicingLookupResult> = mutableMapOf()

    /** The result returned for any tuning not explicitly mapped. */
    var defaultResult: VoicingLookupResult = VoicingLookupResult.Standard(emptyList())

    /** Registers [result] as the response for lookups under [tuning]. */
    fun on(tuning: GuitarTuning, result: VoicingLookupResult): FakeVoicingRepository {
        resultsMap[tuning] = result
        return this
    }

    override suspend fun lookup(chord: ChordKey, tuning: GuitarTuning): VoicingLookupResult =
        resultsMap[tuning] ?: defaultResult
}

/** Convenience factory for a list of [Voicing] stubs (used to distinguish lists in tests). */
fun stubVoicings(count: Int): List<Voicing> = List(count) { i ->
    Voicing(
        labelKey = i + 1,
        marks = List(6) { FretMark.Fretted(i + 2) },
        fingers = List(6) { 1 },
        barre = null,
        rootStringIndices = setOf(0),
        bassDegree = ChordToneRole.ROOT,
    )
}
