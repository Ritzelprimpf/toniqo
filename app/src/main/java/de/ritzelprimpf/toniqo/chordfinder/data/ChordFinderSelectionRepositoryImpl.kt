package de.ritzelprimpf.toniqo.chordfinder.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import de.ritzelprimpf.toniqo.chordfinder.domain.repository.ChordFinderSelection
import de.ritzelprimpf.toniqo.chordfinder.domain.repository.ChordFinderSelectionRepository
import de.ritzelprimpf.toniqo.common.model.ScaleType
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.chordFinderDataStore by preferencesDataStore(name = "chordfinder_preferences")

/**
 * DataStore-backed implementation of [ChordFinderSelectionRepository].
 *
 * Mirrors the metronome's persistence pattern (separate file, same DataStore Preferences API).
 * Stores [ChordFinderSelection.rootPitchClass], [ChordFinderSelection.scaleType] by name,
 * [ChordFinderSelection.includeSeventhChords], and [ChordFinderSelection.hasUserSelection].
 *
 * Invalid or absent keys fall back to [ChordFinderSelection.DEFAULT] so the seed algorithm
 * always has a valid baseline.
 */
class ChordFinderSelectionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : ChordFinderSelectionRepository {

    private val keyRoot = intPreferencesKey("root_pitch_class")
    private val keyScaleType = stringPreferencesKey("scale_type")
    private val keySevenths = booleanPreferencesKey("include_sevenths")
    private val keyHasUserSelection = booleanPreferencesKey("has_user_selection")

    override val selection: Flow<ChordFinderSelection> = context.chordFinderDataStore.data
        .map { prefs ->
            val root = prefs[keyRoot]
            val typeName = prefs[keyScaleType]
            val sevenths = prefs[keySevenths]
            val hasUser = prefs[keyHasUserSelection]

            if (root == null || typeName == null || sevenths == null || hasUser == null) {
                return@map ChordFinderSelection.DEFAULT
            }

            val scaleType = ScaleType.entries.firstOrNull { it.name == typeName }
                ?: return@map ChordFinderSelection.DEFAULT

            if (root !in 0..11) return@map ChordFinderSelection.DEFAULT

            ChordFinderSelection(
                rootPitchClass = root,
                scaleType = scaleType,
                includeSeventhChords = sevenths,
                hasUserSelection = hasUser,
            )
        }

    override suspend fun saveSelection(selection: ChordFinderSelection) {
        context.chordFinderDataStore.edit { writeAll(it, selection) }
    }

    private fun writeAll(prefs: MutablePreferences, s: ChordFinderSelection) {
        prefs[keyRoot] = s.rootPitchClass
        prefs[keyScaleType] = s.scaleType.name
        prefs[keySevenths] = s.includeSeventhChords
        prefs[keyHasUserSelection] = s.hasUserSelection
    }
}
