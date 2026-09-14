package io.github.muntasimulhaque.crayoner.host

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.muntasimulhaque.crayoner.core.Crayons
import io.github.muntasimulhaque.crayoner.core.Draft
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
        /** The paper: every mark on it, and the one step back. */
        val draft: Draft,
        /** The crayon in hand; null until the child picks one up. */
        val crayon: Long? = null,
        /**
         * True while the rubber is in hand: the next mark takes wax off the
         * paper instead of putting it on. It never touches the printed line,
         * because on paper the print is under the wax.
         */
        val erasing: Boolean = false,
        /** The box of colors, held up over the page. */
        val boxOpen: Boolean = false,
        /**
         * The mark under the finger right now. It is drawn with the finished
         * ones and saved the moment the finger lifts, so what the child sees
         * while drawing is what they get.
         */
        val live: Stroke? = null,
        /** The sample held up big, while the child looks closely. */
        val peeking: Boolean = false,
        /**
         * Counts the marks the hand has finished. It is what the one-shot
         * answers (the haptic, and the flattened layer of marks) read, so a
         * mark that lands over paper already colored still answers like a
         * mark. Stepping back and forward both bump it, because both change
         * the paper.
         */
        val marks: Long = 0L,
    ) : Screen {
        /** The marks on the paper, for anything that only draws them. */
        val progress: Progress get() = draft.progress

        /** True while there is a mark the child can take back. */
        val canUndo: Boolean get() = draft.canUndo
    }
}

/** What the shelf needs to draw itself. */
data class ShelfState(
    /** The marks of the page still being worked on, by page id. */
    val drafts: Map<String, String> = emptyMap(),
    val soundOn: Boolean = true,
    /**
     * False until the saved shelf has been read (or its read has failed).
     * The shelf waits for it, so nothing it holds is drawn before it is
     * known. The screenshot harness hosts states directly and leaves it
     * true.
     */
    val loaded: Boolean = true,
)

/**
 * The host performs what the domain decides. It owns which screen is up,
 * which crayon is in hand, and what the page shows. The rules themselves
 * (geometry, wax, strokes, the window, saving) live in :core and are tested
 * there.
 *
 * A finger draws. Down starts a mark, moving draws it, up finishes it, and
 * every finished mark is saved a fraction of a second later, so a phone
 * call, a rotation or a process death costs at most the mark in flight. No
 * network, no accounts, nothing leaving the device.
 *
 * Nothing here counts, scores, compares or judges anything the child does:
 * not the colors, not the areas, and not whether a picture is finished. The
 * app has no opinion about any of it.
 */
class ColoringHost(app: Application) : ViewModel() {

    private val store = CrayonStore(app)
    private val soundBoard = SoundBoard(app)

    private val _screen = MutableStateFlow<Screen>(Screen.Home)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _shelf = MutableStateFlow(ShelfState(loaded = false))
    val shelf: StateFlow<ShelfState> = _shelf.asStateFlow()

    /** Draft writes are conflated and slightly delayed, never per mark. */
    private val drafts = MutableSharedFlow<DraftWrite>(extraBufferCapacity = 1)

    private class DraftWrite(val pageId: String, val text: String)

    init {
        viewModelScope.launch {
            // A failed read is not a broken app: the shelf opens on its
            // defaults and every picture plays, rather than a blank home
            // screen waiting for a file that will never arrive.
            val saved = runCatching { store.load() }.getOrNull()
            // Whatever an older build remembered about finished pictures is
            // let go of here: this build has no finished state to keep. A
            // device that cannot rewrite its preferences keeps the old key,
            // which is never read again, and still opens.
            runCatching { store.forgetFinished() }
            _shelf.value = if (saved == null) {
                ShelfState(loaded = true)
            } else {
                ShelfState(saved.drafts, saved.soundOn, loaded = true)
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
            draft = Draft.of(saved),
            crayon = null,
        )
    }

    /**
     * Pick a crayon up. Its color is the only thing a mark can be drawn in,
     * and picking one up in a box of colors puts the rubber back down.
     */
    fun pickCrayon(argb: Long) {
        val s = _screen.value
        if (s !is Screen.Coloring) return
        if (!Crayons.exists(argb)) return
        if (s.crayon == argb && !s.erasing) return
        _screen.value = s.copy(crayon = argb, erasing = false)
        play(Sfx.RUSTLE)
    }

    /** The rubber in hand, or put away. */
    fun setErasing(on: Boolean) {
        val s = _screen.value
        if (s !is Screen.Coloring) return
        if (s.erasing == on) return
        _screen.value = s.copy(erasing = on)
        play(Sfx.RUSTLE)
    }

    /** The box of colors, held up over the page or put back down. */
    fun setBoxOpen(on: Boolean) {
        val s = _screen.value
        if (s !is Screen.Coloring) return
        if (s.boxOpen == on) return
        _screen.value = s.copy(boxOpen = on)
    }

    /**
     * One step back: the mark the hand finished last comes off the paper.
     *
     * A fresh sheet has no step to undo and the press simply lands, and a
     * page read back from last time has nothing to step back to, so there is
     * nothing here a child can break by pressing it. Nothing is confirmed
     * and nothing is offered twice: the rubber is what a child uses to
     * change their mind about a whole picture, and this is the one step a
     * hand that drew a mark it did not mean needs.
     */
    fun undo() {
        val s = _screen.value
        if (s !is Screen.Coloring) return
        if (s.live != null) return
        val next = s.draft.undo()
        if (next === s.draft) return
        commit(s, next)
    }

    /**
     * A finger touching the paper at [p], in page units: x and y each run
     * over the sheet's own extent, so the point is the paper point whatever
     * the sheet's size on screen.
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
        if (s.peeking || s.boxOpen) return
        val at = clamp(p)
        if (s.erasing) {
            _screen.value = s.copy(live = Strokes.eraseDot(at))
            return
        }
        val hand = s.crayon ?: colorWantedAt(s.page, at) ?: return
        _screen.value = s.copy(crayon = hand, live = Strokes.dot(hand, at))
    }

    /** The finger moved: the mark grows under it. */
    fun moveStroke(p: Vec2) {
        val s = _screen.value
        if (s !is Screen.Coloring) return
        val live = s.live ?: return
        if (s.peeking || s.boxOpen) return
        val grown = Strokes.extend(live, clamp(p))
        if (grown !== live) _screen.value = s.copy(live = grown)
    }

    /** The finger lifted: the mark is finished, saved, and answered. */
    fun endStroke() {
        val s = _screen.value
        if (s !is Screen.Coloring) return
        val live = s.live ?: return
        if (s.peeking || s.boxOpen) {
            _screen.value = s.copy(live = null)
            return
        }
        val draft = if (live.erase) s.draft.erase(live.points) else s.draft.color(live.color, live.points)
        commit(s, draft)
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
        if (s.peeking || s.boxOpen) return
        val region = s.page.region(index) ?: return
        val hand = s.crayon ?: region.fillArgb
        val scribble = Strokes.scribble(region, hand)
        if (scribble.isEmpty) return
        commit(s.copy(crayon = hand, erasing = false), s.draft.color(hand, scribble.points))
    }

    /**
     * The one place a finished mark is answered and saved. Whatever put
     * marks on the paper (the hand, a screen reader, or a press of undo)
     * comes through here, so the paper is saved once and only once. A step
     * back is answered like a mark, because a hand that just changed the
     * paper did something.
     */
    private fun commit(s: Screen.Coloring, draft: Draft) {
        _screen.value = s.copy(
            draft = draft,
            live = null,
            marks = s.marks + 1,
        )
        // The mark itself makes no sound. Nothing plays while a finger is on
        // the paper: the wax does not answer, because the mark is the thing.
        rememberDraft(s.page, draft)
    }

    /** Which color the area under [p] asks for, if the point is on paper. */
    private fun colorWantedAt(page: Page, p: Vec2): Long? {
        val index = page.regionIndexAt(p)
        return page.region(index)?.fillArgb
    }

    private fun clamp(p: Vec2): Vec2 =
        Vec2(p.x.coerceIn(0.0, 1.0), p.y.coerceIn(0.0, Page.ASPECT))

    /** Look at the sample closely, or put it back down. */
    fun setPeek(on: Boolean) {
        val s = _screen.value
        if (s is Screen.Coloring) _screen.value = s.copy(peeking = on, boxOpen = false)
    }

    /** Back to the shelf. Nothing is lost: the marks are already saved. */
    fun home() {
        val s = _screen.value
        if (s is Screen.Coloring) rememberDraft(s.page, s.draft)
        _screen.value = Screen.Home
    }

    fun setSound(on: Boolean) {
        _shelf.value = _shelf.value.copy(soundOn = on)
        viewModelScope.launch { runCatching { store.setSound(on) } }
    }

    private fun rememberDraft(page: Page, draft: Draft) {
        val progress = draft.progress
        drafts.tryEmit(DraftWrite(page.id, if (progress.isEmpty) "" else progress.serialize()))
    }

    /** The effects, unless the sound switch is off. */
    private fun play(sfx: Sfx) {
        if (_shelf.value.soundOn) soundBoard.play(sfx)
    }

    override fun onCleared() {
        soundBoard.release()
    }

    private companion object {
        const val DRAFT_SETTLE_MS = 250L
    }
}
