package io.github.muntasimulhaque.crayoner.host

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.muntasimulhaque.crayoner.core.Crayons
import io.github.muntasimulhaque.crayoner.core.Page
import io.github.muntasimulhaque.crayoner.core.Pages
import io.github.muntasimulhaque.crayoner.core.Progress
import io.github.muntasimulhaque.crayoner.core.Stroke
import io.github.muntasimulhaque.crayoner.core.Strokes
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
        /** Every mark the child has made, in the order they made them. */
        val progress: Progress,
        /**
         * Which areas the crayon has touched so far. It is derived from
         * [progress] and [page], and it is kept here rather than recomputed
         * while drawing: deciding it means walking every point of every mark
         * through the picture, which is not work a frame should repeat. The
         * host owns it, so the bar's count, the screen reader's labels and
         * the completion rule all read one answer.
         */
        val reached: Set<Int> = emptySet(),
        /** The crayon in hand; null until the child picks one up. */
        val crayon: Long? = null,
        /**
         * The mark under the finger right now. It is drawn with the finished
         * ones and saved the moment the finger lifts, so what the child sees
         * while drawing is what they get.
         */
        val live: Stroke? = null,
        /** The finished plate, risen over the page. */
        val celebrating: Boolean = false,
        /** The sample held up big, while the child looks closely. */
        val peeking: Boolean = false,
        /** The clear confirm, waiting for an answer. */
        val confirmingClear: Boolean = false,
        /** The areas the last mark got right, and when. */
        val rightIndices: Set<Int> = emptySet(),
        val rightAt: Long = 0L,
        /** The areas the last mark got wrong, and when. */
        val wrongIndices: Set<Int> = emptySet(),
        val wrongAt: Long = 0L,
        /**
         * Counts finished marks. The write-only half of the state: it is
         * what the one-shot answers (the haptic, the tick) read, so a mark
         * that lands over areas already colored still answers like a mark.
         */
        val stamp: Long = 0L,
    ) : Screen
}

/** What the shelf needs to draw itself. */
data class ShelfState(
    val finished: Set<String> = emptySet(),
    /** The marks of the page still being worked on, by page id. */
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
 * (geometry, saving, completion) live in :core and are tested there.
 *
 * A finger draws. Down starts a mark, moving draws it, up finishes it, and
 * every finished mark is saved a fraction of a second later, so a phone
 * call, a rotation or a process death costs at most the mark in flight. No
 * network, no accounts, nothing leaving the device.
 */
class ColoringHost(app: Application) : ViewModel() {

    private val store = CrayonStore(app)
    private val soundBoard = SoundBoard(app)

    private val _screen = MutableStateFlow<Screen>(Screen.Home)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _shelf = MutableStateFlow(ShelfState(loaded = false))
    val shelf: StateFlow<ShelfState> = _shelf.asStateFlow()

    /** Draft writes are conflated and slightly delayed, never per mark. */
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

    /** Open a page: its saved marks if it has any, else a blank sheet. */
    fun open(pageId: String) {
        val page = Pages.byId(pageId) ?: return
        val saved = runCatching { store.draftFor(page.id, _shelf.value.drafts) }
            .getOrDefault(Progress.Empty)
        _screen.value = Screen.Coloring(
            page = page,
            progress = saved,
            reached = saved.reached(page),
            crayon = null,
        )
    }

    /** Pick a crayon up. Its color is the only thing a mark can be drawn in. */
    fun pickCrayon(argb: Long) {
        val s = _screen.value
        if (s !is Screen.Coloring) return
        if (!Crayons.exists(argb)) return
        if (s.crayon == argb) return
        _screen.value = s.copy(crayon = argb)
        chime(Sfx.RUSTLE)
    }

    /**
     * A finger touching the paper at [p], in page units.
     *
     * The first touch on a fresh page always works: with no crayon in hand
     * yet, the app picks up the color the area under the finger is asking
     * for, so a three year old's first act ends with their own mark on the
     * paper in the right color, instead of in nothing happening. From then on
     * the child is holding a crayon and every mark is drawn with it, which is
     * the whole lesson.
     */
    fun beginStroke(p: Vec2) {
        val s = _screen.value
        if (s !is Screen.Coloring) return
        if (s.celebrating || s.peeking || s.confirmingClear) return
        val at = clamp(p)
        val hand = s.crayon ?: colorWantedAt(s.page, at) ?: return
        _screen.value = s.copy(crayon = hand, live = Strokes.dot(hand, at))
    }

    /** The finger moved: the mark grows under it. */
    fun moveStroke(p: Vec2) {
        val s = _screen.value
        if (s !is Screen.Coloring) return
        val live = s.live ?: return
        if (s.celebrating || s.peeking || s.confirmingClear) return
        val grown = Strokes.extend(live, clamp(p))
        if (grown !== live) _screen.value = s.copy(live = grown)
    }

    /** The finger lifted: the mark is finished, saved, and answered. */
    fun endStroke() {
        val s = _screen.value
        if (s !is Screen.Coloring) return
        val live = s.live ?: return
        if (s.celebrating || s.peeking || s.confirmingClear) {
            _screen.value = s.copy(live = null)
            return
        }
        commit(s, live)
    }

    /**
     * Colors one whole area. It is how a screen reader colors, because a
     * child who cannot aim a finger cannot draw a mark either: the app
     * scribbles the area in the crayon in hand, and the result is the same
     * kind of wax on the same paper as anybody else's mark.
     */
    fun colorArea(index: Int) {
        val s = _screen.value
        if (s !is Screen.Coloring) return
        if (s.celebrating || s.peeking || s.confirmingClear) return
        val region = s.page.region(index) ?: return
        val hand = s.crayon ?: region.fillArgb
        val scribble = Strokes.scribble(region, hand)
        if (scribble.isEmpty) return
        commit(s.copy(crayon = hand), scribble)
    }

    /** The one place a mark is finished, answered, and saved. */
    private fun commit(s: Screen.Coloring, live: Stroke) {
        val wasReached = s.reached
        val progress = s.progress.with(live)
        val after = progress.reached(s.page)
        val touched = after - wasReached
        val right = touched.filter { s.page.region(it)?.fillArgb == live.color }.toSet()
        val wrong = touched - right
        val justFinished = s.page.isComplete(after) && wasReached.size < s.page.regionCount
        val now = System.nanoTime()
        _screen.value = s.copy(
            progress = progress,
            reached = after,
            live = null,
            rightIndices = right,
            rightAt = if (right.isNotEmpty()) now else s.rightAt,
            wrongIndices = wrong,
            wrongAt = if (wrong.isNotEmpty()) now else s.wrongAt,
            celebrating = justFinished,
            stamp = s.stamp + 1,
        )
        chime(if (right.isNotEmpty()) Sfx.TICK else Sfx.PAINT)
        if (justFinished) {
            chime(Sfx.CHIME)
            onFinished(s.page.id)
        } else {
            rememberDraft(s.page, progress)
        }
    }

    /** Which color the area under [p] asks for, if the point is on paper. */
    private fun colorWantedAt(page: Page, p: Vec2): Long? {
        val index = page.regionIndexAt(p)
        return page.region(index)?.fillArgb
    }

    private fun clamp(p: Vec2): Vec2 =
        Vec2(p.x.coerceIn(0.0, 1.0), p.y.coerceIn(0.0, 1.0))

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

    /** Wipe this page back to blank paper. */
    fun clearPage() {
        val s = _screen.value
        if (s !is Screen.Coloring) return
        _screen.value = s.copy(
            progress = Progress.Empty,
            reached = emptySet(),
            confirmingClear = false,
            rightIndices = emptySet(),
            wrongIndices = emptySet(),
        )
        viewModelScope.launch { runCatching { store.clearDraft() } }
    }

    /** Back to the shelf. Nothing is lost: the marks are already saved. */
    fun home() {
        _screen.value = Screen.Home
    }

    fun setSound(on: Boolean) {
        _shelf.value = _shelf.value.copy(soundOn = on)
        viewModelScope.launch { runCatching { store.setSound(on) } }
    }

    private fun rememberDraft(page: Page, progress: Progress) {
        val text = progress.serialize()
        drafts.tryEmit(Draft(page.id, if (progress.isEmpty) "" else text))
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
