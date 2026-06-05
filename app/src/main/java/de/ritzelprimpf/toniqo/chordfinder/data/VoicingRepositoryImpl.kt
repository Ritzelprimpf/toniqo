package de.ritzelprimpf.toniqo.chordfinder.data

import android.content.Context
import de.ritzelprimpf.toniqo.chordfinder.domain.VoicingTransposer
import de.ritzelprimpf.toniqo.chordfinder.domain.model.ChordKey
import de.ritzelprimpf.toniqo.chordfinder.domain.model.Voicing
import de.ritzelprimpf.toniqo.chordfinder.domain.repository.VoicingLookupResult
import de.ritzelprimpf.toniqo.chordfinder.domain.repository.VoicingRepository
import de.ritzelprimpf.toniqo.common.model.GuitarTuning
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Loads the curated standard-6 voicing library once from assets and serves all subsequent
 * lookups from an in-memory cache. Thread safety is ensured by a [Mutex] so the asset is
 * loaded at most once even under concurrent suspension.
 *
 * Tier classification:
 * - **Tier 1** (`offset == 0`, i.e. standard 6-string) → [VoicingLookupResult.Standard].
 * - **Tier 2** (`offset != null && offset != 0`, uniform detune) → [VoicingLookupResult.UniformOffset]:
 *   movable voicings shifted up by `abs(offset)` frets; open voicings dropped.
 * - **Tier 3** (`offset == null`) → [VoicingLookupResult.Unsupported].
 */
@Singleton
class VoicingRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : VoicingRepository {

    private companion object {
        const val ASSET_PATH = "chordfinder/voicings_standard_6.json"
        const val MAX_VOICINGS = 5
        const val MAX_FRET = 15
    }

    private val loadMutex = Mutex()
    private var cache: Map<ChordKey, List<Voicing>>? = null

    override suspend fun lookup(chord: ChordKey, tuning: GuitarTuning): VoicingLookupResult {
        val library = getLibrary()
        val standardVoicings = library[chord] ?: emptyList()

        val offset = tuning.uniformOffsetFrom(GuitarTuning.STANDARD_6)
        return when {
            offset == null -> VoicingLookupResult.Unsupported(tuning)
            offset == 0 -> VoicingLookupResult.Standard(standardVoicings.take(MAX_VOICINGS))
            else -> {
                val delta = abs(offset)
                val shifted = standardVoicings
                    .mapNotNull { VoicingTransposer.shift(it, delta, MAX_FRET) }
                    .sortedBy { it.baseFret }
                    .take(MAX_VOICINGS)
                VoicingLookupResult.UniformOffset(shifted, offset)
            }
        }
    }

    private suspend fun getLibrary(): Map<ChordKey, List<Voicing>> {
        cache?.let { return it }
        return loadMutex.withLock {
            cache?.let { return it } // double-checked inside lock
            val json = context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }
            val parsed = VoicingJsonParser.parse(json, GuitarTuning.STANDARD_6)
            cache = parsed
            parsed
        }
    }
}
