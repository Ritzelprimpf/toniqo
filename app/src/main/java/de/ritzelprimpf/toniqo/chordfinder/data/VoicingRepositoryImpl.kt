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
import java.io.FileNotFoundException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Loads the curated voicing libraries once from assets and serves all subsequent lookups from
 * an in-memory cache. Thread safety is ensured by a [Mutex] so no asset is loaded more than once
 * even under concurrent suspension.
 *
 * Two independent tuning families are curated: standard tuning ([GuitarTuning.STANDARD_6]) and
 * drop-D tuning ([GuitarTuning.DROP_D_6]). Neither is a uniform offset of the other — only the
 * lowest string moves for a drop tuning — so each needs its own hand-curated library. A
 * requested tuning is matched against each family's reference tuning in turn (see [FAMILIES]);
 * the first family it's a uniform offset of wins.
 *
 * Tier classification (relative to whichever family matched):
 * - **Tier 1** (`offset == 0`, i.e. exactly the family's reference tuning) → [VoicingLookupResult.Standard].
 * - **Tier 2** (`offset != null && offset != 0`, uniform detune) → [VoicingLookupResult.UniformOffset]:
 *   movable voicings shifted up by `abs(offset)` frets; open voicings dropped.
 * - **Tier 3** (matches no family) → [VoicingLookupResult.Unsupported].
 *
 * A family whose asset hasn't been curated/shipped yet behaves as if it were empty (every lookup
 * against it returns no voicings) rather than crashing — see [loadFamily].
 */
@Singleton
class VoicingRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : VoicingRepository {

    private data class TuningFamily(val reference: GuitarTuning, val assetPath: String)

    private companion object {
        const val MAX_VOICINGS = 5

        val FAMILIES = listOf(
            TuningFamily(GuitarTuning.STANDARD_6, "chordfinder/voicings_standard_6.json"),
            TuningFamily(GuitarTuning.DROP_D_6, "chordfinder/voicings_drop_d_6.json"),
        )
    }

    private val loadMutex = Mutex()
    private val cache = mutableMapOf<String, Map<ChordKey, List<Voicing>>>()

    override suspend fun lookup(chord: ChordKey, tuning: GuitarTuning): VoicingLookupResult {
        for (family in FAMILIES) {
            val offset = tuning.uniformOffsetFrom(family.reference) ?: continue
            val referenceVoicings = getLibrary(family)[chord] ?: emptyList()
            return if (offset == 0) {
                VoicingLookupResult.Standard(referenceVoicings.take(MAX_VOICINGS))
            } else {
                val delta = abs(offset)
                val shifted = referenceVoicings
                    .mapNotNull { VoicingTransposer.shift(it, delta, Voicing.MAX_FRET) }
                    .sortedBy { it.baseFret }
                    .take(MAX_VOICINGS)
                VoicingLookupResult.UniformOffset(shifted, offset)
            }
        }
        return VoicingLookupResult.Unsupported(tuning)
    }

    private suspend fun getLibrary(family: TuningFamily): Map<ChordKey, List<Voicing>> {
        cache[family.assetPath]?.let { return it }
        return loadMutex.withLock {
            cache[family.assetPath]?.let { return it } // double-checked inside lock
            val parsed = loadFamily(family)
            cache[family.assetPath] = parsed
            parsed
        }
    }

    /**
     * Parses [family]'s asset, or returns an empty library if it hasn't been curated/shipped
     * yet. A missing drop-tuning asset is an expected, non-error state during rollout (the
     * generator tool's own docs require hand-curation before an asset ships) — not a bug — so
     * lookups against it simply come back empty rather than crashing the Chord Voicings screen.
     */
    private fun loadFamily(family: TuningFamily): Map<ChordKey, List<Voicing>> {
        val json = try {
            context.assets.open(family.assetPath).bufferedReader().use { it.readText() }
        } catch (e: FileNotFoundException) {
            return emptyMap()
        }
        return VoicingJsonParser.parse(json, family.reference)
    }
}
