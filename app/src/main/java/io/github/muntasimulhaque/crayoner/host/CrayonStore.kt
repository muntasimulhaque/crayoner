package io.github.muntasimulhaque.crayoner.host

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.muntasimulhaque.crayoner.core.Pages
import io.github.muntasimulhaque.crayoner.core.Progress
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "crayoner")

/** Everything the shelf needs to draw itself, read in one pass. */
data class ShelfProgress(
    /** Pages the child has finished at least once. */
    val finished: Set<String>,
    /** The colors of a page still being worked on, by page id. */
    val drafts: Map<String, String>,
    /** Whether the two effects play at all. */
    val soundOn: Boolean,
)

/**
 * Everything the app keeps between runs: which pages have been finished,
 * the colors of a page still in progress, and the sound switch. No accounts,
 * no analytics, nothing that leaves the device.
 *
 * Only one draft is ever kept: the page the child was last working on. That
 * is enough to survive a phone call or a process death, and it means the
 * preference file can never grow with every tap.
 */
class CrayonStore(private val context: Context) {

    suspend fun load(): ShelfProgress = context.dataStore.data.map { prefs ->
        val finished = prefs[FINISHED]
            ?.split(',')
            ?.filter { id -> Pages.all.any { it.id == id } }
            ?.toSet()
            ?: emptySet()
        val draftPage = prefs[DRAFT_PAGE]?.takeIf { id -> Pages.byId(id) != null }
        val draftColors = prefs[DRAFT_COLORS]
        val drafts = if (draftPage != null && !draftColors.isNullOrBlank()) {
            mapOf(draftPage to draftColors)
        } else {
            emptyMap()
        }
        ShelfProgress(finished, drafts, prefs[SOUND_ON] ?: true)
    }.first()

    /** Marks one page finished; answers the new set. */
    suspend fun markFinished(pageId: String): Set<String> {
        var total: Set<String> = emptySet()
        context.dataStore.edit { prefs ->
            val current = prefs[FINISHED]
                ?.split(',')
                ?.filter { it.isNotBlank() }
                ?.toMutableSet()
                ?: mutableSetOf()
            current += pageId
            total = current
            prefs[FINISHED] = current.joinToString(",")
        }
        return total
    }

    /**
     * Saves the page being colored. An empty page clears the draft instead,
     * so wiping a picture leaves nothing behind to restore.
     */    suspend fun saveDraft(pageId: String, serialized: String) {
        context.dataStore.edit { prefs ->
            if (serialized.isBlank()) {
                prefs.remove(DRAFT_PAGE)
                prefs.remove(DRAFT_COLORS)
            } else {
                prefs[DRAFT_PAGE] = pageId
                prefs[DRAFT_COLORS] = serialized
            }
        }
    }

    suspend fun clearDraft() {
        context.dataStore.edit { prefs ->
            prefs.remove(DRAFT_PAGE)
            prefs.remove(DRAFT_COLORS)
        }
    }

    suspend fun setSound(on: Boolean) {
        context.dataStore.edit { prefs -> prefs[SOUND_ON] = on }
    }

    /** Loads a page's saved marks back, dropping anything malformed. */
    fun draftFor(pageId: String, drafts: Map<String, String>): Progress =
        Progress.parse(drafts[pageId])

    private companion object {
        val FINISHED = stringPreferencesKey("finished")
        val DRAFT_PAGE = stringPreferencesKey("draft_page")
        val DRAFT_COLORS = stringPreferencesKey("draft_colors")
        val SOUND_ON = booleanPreferencesKey("sound_on")
    }
}
