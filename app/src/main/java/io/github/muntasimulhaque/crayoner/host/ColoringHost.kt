package io.github.muntasimulhaque.crayoner.host

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.muntasimulhaque.crayoner.core.Crayons
import io.github.muntasimulhaque.crayoner.core.Page
import io.github.muntasimulhaque.crayoner.core.Pages
import io.github.muntasimulhaque.crayoner.core.Progress
import io.github.muntasimulhaque.crayoner.core.Region
import io.github.muntasimulhaque.crayoner.core.Vec2
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** Where in the book we are. Home is the shelf; tap a picture and color. */
sealed interface Screen {
    data object Home : Screen

    data class Coloring(
        val page: Page,
        /** What shows on the page right now, by region index. */
        val progress: Progress,
        /** The crayon in hand; null until the child picks one up. */
        val crayon: Long? = null,
        /** The finished plate, risen over the page. */
        val celebrating: Boolean = false,
        /** The sample held up big, while the child looks closely. */
        val peeking: Boolean = false,
        /** The clear confirm, waiting for an answer. */
        val confirmingClear: Boolean = false,
        /** The last region painted with the right color, and when. */
        val rightIndex: Int = -1,
        val rightAt: Long = 0L,
        /** The last region painted with the wrong color, and when. */
        val wrongIndex: Int = -1,
        val wrongAt: Long = 0L,
        /**
         * The last color that landed: which area, where the crayon touched
         * down in page units, what color was there before, and when. The
         * sheet reads this to wipe the new color in from under the finger,
         * which is the single most satisfying moment in the app.
         */
        val lastPaint: LastPaint? = null,
    ) : Screen
}

/** One color arriving, in enough detail for the sheet to animate it. */
data class LastPaint(
    val index: Int,
    val at: Vec2,
    val before: Long?,
    val stamp: Long,
)

/** What the shelf needs to draw itself. */
data class ShelfState(
    val finished: Set<String> = emptySet(),
    /** The colors of the page still being worked on, by page id. */
    val drafts: Map<String, String> = emptyMap(),
    val soundOn: Boolean = true,
    /**
     * False until the saved shelf has been read (or its read has failed).
     * The shelf waits for it, so no picture ever flashes as unfinished and
     * then turns over. The screenshot harness hosts states directly and
     * leaves it true.
     */
    val loaded: Boolean = true,
) {
    fun isFinished(pageId: String): Boolean = pageId in finished
    fun hasDraft(pageId: String): Boolean = !drafts[pageId].isNullOrBlank()
}

/**
 * The host performs what the domain decides. It owns which screen is up,
 * which crayon is in hand, and what the page shows. The rules themselves
 * (hit testing, completion, saving) live in :core and are tested there.
 *
 * Every color the child places is saved a fraction of a second later, so a
 * phone call, a rotation or a process death costs at most one tap. No
 * network, no accounts, nothing leaving the device.
 */
class ColoringHost(app: Application) : ViewModel() {

    private val store = CrayonStore(app)
    private val soundBoard = SoundBoard(app)

    private val _screen = MutableStateFlow<Screen>(Screen.Home)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _shelf = MutableStateFlow(ShelfState(loaded = false))
    val shelf: StateFlow<ShelfState> = _shelf.asStateFlow()

    /** Draft writes are conflated and slightly delayed, never per tap. */
    private val drafts = MutableSharedFlow<Draft>(extraBufferCapacity = 1)

    private class Draft(val pageId: String, val text: String)

    init {
        viewModelScope.launch {
            // A failed read is not a broken app: the shelf opens on its
            // defaults and every picture plays, rather than a blank home
            // screen waiting for a file that will never arrive.
            val saved = runCatching { store.load() }.getOrNull()
            _shelf.value = if (saved == null) {
                ShelfState(loaded = true)
            } else {
                ShelfState(saved.finished, saved.drafts, saved.soundOn, loaded = true)
            }
        }
        viewModelScope.launch {
            drafts.collectLatest { draft ->
                delay(DRAFT_SETTLE_MS)
                runCatching { store.saveDraft(draft.pageId, draft.text) }
            }
        }
    }

    /** Open a page: its saved colors if it has any, else a blank one. */
    fun open(pageId: String) {
        val page = Pages.byId(pageId) ?: return
        val saved = runCatching { store.draftFor(page, _shelf.value.drafts) }.getOrDefault(Progress.Empty)
        _screen.value = Screen.Coloring(
            page = page,
            progress = saved,
            crayon = null,
        )
    }

    /** Pick a crayon up. Its color is the only thing a tap can paint with. */
    fun pickCrayon(argb: Long) {
        val s = _screen.value
        if (s !is Screen.Coloring) return
        if (!Crayons.exists(argb)) return
        if (s.crayon == argb) return
        _screen.value = s.copy(crayon = argb)
        chime(Sfx.RUSTLE)
    }

    /**
     * Colors one area by index. It is the same rule as a tap, with the
     * point supplied by the area itself, so a screen reader's action and a
     * finger do exactly the same thing, including the first touch rule.
     */
    fun colorArea(index: Int) {
        val s = _screen.value
        if (s !is Screen.Coloring) return
        if (s.celebrating || s.peeking || s.confirmingClear) return
        val region = s.page.region(index) ?: return
        colorRegion(s, index, region, s.crayon ?: region.fillArgb)
    }

    /**
     * A tap on the page at [p], in page units. Colors the topmost area the
     * point lands on, with the crayon in hand.
     *
     * The first touch on a page always works: with no crayon in hand yet,
     * the app picks up the color that area is asking for and colors it, so
     * a three year old's first act ends in a colored picture instead of in
     * nothing happening. From then on the child is holding a crayon and
     * every later touch uses it, which is the whole lesson.
     */
    fun tap(p: Vec2) {
        val s = _screen.value
        if (s !is Screen.Coloring) return
        if (s.celebrating || s.peeking || s.confirmingClear) return
        val index = s.page.regionIndexAt(p)
        if (index < 0) return
        val region = s.page.region(index) ?: return
        colorRegion(s, index, region, s.crayon ?: region.fillArgb, at = p)
    }

    /** The one place a color lands, shared by a tap and a reader's action. */
    private fun colorRegion(
        s: Screen.Coloring,
        index: Int,
        region: Region,
        crayon: Long,
        at: Vec2 = region.centroid,
    ) {
        val right = region.fillArgb == crayon
        // A tap that changes nothing but the same color again is still real
        // painting, but it should not re-cheer: only a fresh match earns
        // the tick and the sparkle.
        val wasRight = s.progress.colorOf(index) == region.fillArgb
        val becameRight = right && !wasRight
        val progress = s.progress.with(index, crayon)
        val justFinished = s.page.isComplete(progress.asMap()) && !s.page.isComplete(s.progress.asMap())
        val now = System.nanoTime()
        _screen.value = s.copy(
            progress = progress,
            // The color just used stays in hand, including the color the
            // first touch picked up on its own.
            crayon = crayon,
            lastPaint = LastPaint(index, at, s.progress.colorOf(index), now),
            rightIndex = if (becameRight) index else s.rightIndex,
            rightAt = if (becameRight) now else s.rightAt,
            wrongIndex = if (right) s.wrongIndex else index,
            wrongAt = if (right) s.wrongAt else now,
            celebrating = justFinished,
        )
        chime(if (becameRight) Sfx.TICK else Sfx.PAINT)
        if (justFinished) {
            chime(Sfx.CHIME)
            onFinished(s.page.id)
        } else {
            rememberDraft(s.page, progress)
        }
    }

    /** A finished picture earns a sticker, and its draft is done. */
    private fun onFinished(pageId: String) {
        viewModelScope.launch {
            runCatching { store.markFinished(pageId) }.getOrNull()?.let { total ->
                _shelf.value = _shelf.value.copy(
                    finished = total,
                    drafts = _shelf.value.drafts - pageId,
                )
            }
            runCatching { store.clearDraft() }
        }
    }

    /** Look at the sample closely, or put it back down. */
    fun setPeek(on: Boolean) {
        val s = _screen.value
        if (s is Screen.Coloring) _screen.value = s.copy(peeking = on)
    }

    /** Ask before wiping the page; a whole picture is a lot to lose. */
    fun askClear(on: Boolean) {
        val s = _screen.value
        if (s is Screen.Coloring) _screen.value = s.copy(confirmingClear = on, peeking = false)
    }

    /** Wipe this page back to bare outlines. */
    fun clearPage() {
        val s = _screen.value
        if (s !is Screen.Coloring) return
        _screen.value = s.copy(progress = Progress.Empty, confirmingClear = false, rightIndex = -1, wrongIndex = -1)
        viewModelScope.launch { runCatching { store.clearDraft() } }
    }

    /** Back to the shelf. Nothing is lost: the draft is already saved. */
    fun home() {
        val s = _screen.value
        if (s is Screen.Coloring && s.progress.coloredCount == 0) {
            viewModelScope.launch { runCatching { store.clearDraft() } }
        }
        _screen.value = Screen.Home
    }

    fun setSound(on: Boolean) {
        _shelf.value = _shelf.value.copy(soundOn = on)
        viewModelScope.launch { runCatching { store.setSound(on) } }
    }

    /** Reopen the page the child was coloring, if there is one. */
    fun resume(pageId: String) = open(pageId)

    private fun rememberDraft(page: Page, progress: Progress) {
        val text = progress.serialize(page)
        drafts.tryEmit(Draft(page.id, if (progress.coloredCount == 0) "" else text))
    }

    /** The four effects, unless the sound switch is off. */
    private fun chime(sfx: Sfx) {
        if (_shelf.value.soundOn) soundBoard.play(sfx)
    }

    override fun onCleared() {
        soundBoard.release()
    }

    private companion object {
        const val DRAFT_SETTLE_MS = 250L
    }
}
