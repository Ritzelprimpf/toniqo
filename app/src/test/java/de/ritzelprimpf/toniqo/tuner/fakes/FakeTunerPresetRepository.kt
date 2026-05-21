package de.ritzelprimpf.toniqo.tuner.fakes

import de.ritzelprimpf.toniqo.common.model.Note
import de.ritzelprimpf.toniqo.common.model.NoteName
import de.ritzelprimpf.toniqo.tuner.domain.model.TunerCategory
import de.ritzelprimpf.toniqo.tuner.domain.model.TunerPreset
import de.ritzelprimpf.toniqo.tuner.domain.repository.TunerPresetRepository

/**
 * In-memory test double for [TunerPresetRepository].
 *
 * Contains a minimal catalog of three presets (one 6-string, one 7-string, one 8-string) with
 * two notes each, sufficient for all ViewModel tests without the full 30-entry catalog.
 */
class FakeTunerPresetRepository : TunerPresetRepository {

    private val catalog = listOf(
        TunerPreset(
            id = "six_string_standard_e",
            displayName = "E Standard",
            category = TunerCategory.STANDARD,
            stringCount = 2,
            notes = listOf(
                Note(NoteName.E, 2),
                Note(NoteName.A, 2),
            ),
        ),
        TunerPreset(
            id = "six_string_drop_d",
            displayName = "Drop D",
            category = TunerCategory.DROPPED,
            stringCount = 2,
            notes = listOf(
                Note(NoteName.D, 2),
                Note(NoteName.A, 2),
            ),
        ),
        TunerPreset(
            id = "seven_string_standard_b",
            displayName = "B Standard",
            category = TunerCategory.STANDARD,
            stringCount = 2,
            notes = listOf(
                Note(NoteName.B, 1),
                Note(NoteName.E, 2),
            ),
        ),
    )

    override suspend fun getPresets(): List<TunerPreset> = catalog

    override suspend fun getPresetById(id: String): TunerPreset? =
        catalog.firstOrNull { it.id == id }

    override suspend fun getPresetsGrouped(): Map<Int, Map<TunerCategory, List<TunerPreset>>> =
        catalog
            .groupBy { it.stringCount }
            .mapValues { (_, list) -> list.groupBy { it.category } }
}
