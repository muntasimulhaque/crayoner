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
    /** The marks of a page still being worked on, by page id. */
    val drafts: Map<String, String>,
    /** Whether the effects play at all. */
    val soundOn: Boolean,
)

/**
 * Everything the app keeps between runs: the marks of the page the child was
 * last working on, and the sound switch. No accounts, no analytics, nothing
 * that leaves the device.
 *
 * Only one draft is ever kept: the page the child was last working on. That
 * is enough to survive a phone call or a process death, and it means the
 * preference file can never grow with every mark.
 */
class CrayonStore(private val context: Context) {

    suspend fun load(): ShelfProgress = context.dataStore.data.map { prefs ->
        val draftPage = prefs[DRAFT_PAGE]?.takeIf { id -> Pages.byId(id) != null }
        val draftColors = prefs[DRAFT_COLORS]
        val drafts = if (draftPage != null && !draftColors.isNullOrBlank()) {
            mapOf(draftPage to draftColors)
        } else {
            emptyMap()
        }
        ShelfProgress(drafts, prefs[SOUND_ON] ?: true)
    }.first()

    /**
     * Saves the page being colored. An empty page clears the draft instead,
     * so a page the child has rubbed clean leaves nothing behind to restore.
     */
    suspend fun saveDraft(pageId: String, serialized: String) {
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

    /**
     * Drops the record of finished pictures that versions up to 0.3 kept.
     * The app has no finished state any more, so the old key is removed the
     * first time the shelf is read; a key that is already gone is not a
     * change and is not written back.
     */
    suspend fun forgetFinished() {
        context.dataStore.edit { prefs -> prefs.remove(FINISHED_LEGACY) }
    }

    /** Loads a page's saved marks back, dropping anything malformed. */
    fun draftFor(pageId: String, drafts: Map<String, String>): Progress =
        Progress.parse(drafts[pageId])

    private companion object {
        /**
         * The key 0.3 and earlier wrote the finished set to. It is not read
         * any more; it is only cleaned up.
         */
        val FINISHED_LEGACY = stringPreferencesKey("finished")
        val DRAFT_PAGE = stringPreferencesKey("draft_page")
        val DRAFT_COLORS = stringPreferencesKey("draft_colors")
        val SOUND_ON = booleanPreferencesKey("sound_on")
    }
}
