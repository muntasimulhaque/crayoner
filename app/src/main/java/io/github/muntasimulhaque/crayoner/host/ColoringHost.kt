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
         * True once the child has stamped the picture. It is their own word
         * that the work is done: the app never decides that for them.
         */
        val sealed: Boolean = false,
        /**
         * Counts finished marks. The write-only half of the state: it is
         * what the one-shot answers (the haptic, the tick) read, so a mark
         * that lands over paper already colored still answers like a mark.
         */
        val stamp: Long = 0L,
        /** Counts stamps of the seal, so the haptic fires exactly once. */
        val sealStamp: Long = 0L,
    ) : Screen
}

/** What the shelf needs to draw itself. */
data class ShelfState(
    /** Pictures the child has stamped as finished. */
    val sealed: Set<String> = emptySet(),
    /** The marks of the page still being worked on, by page id. */
    val drafts: Map<String, String> = emptyMap(),
    val soundOn: Boolean = true,
    /**
     * False until the saved shelf has been read (or its read has failed).
     * The shelf waits for it, so no picture ever flashes as unsealed and
     * then turns over. The screenshot harness hosts states directly and
     * leaves it true.
     */
    val loaded: Boolean = true,
) {
    fun isSealed(pageId: String): Boolean = pageId in sealed
    fun hasDraft(pageId: String): Boolean = !drafts[pageId].isNullOrBlank()
}

/**
 * The host performs what the domain decides. It owns which screen is up,
 * which crayon is in hand, and what the page shows. The rules themselves
 * (geometry, wax, saving) live in :core and are tested there.
 *
 * A finger draws. Down starts a mark, moving draws it, up finishes it, and
 * every finished mark is saved a fraction of a second later, so a phone
 * call, a rotation or a process death costs at most the mark in flight. No
 * network, no accounts, nothing leaving the device.
 *
 * Nothing here counts, scores or compares anything the child does. The app
 * has exactly one opinion about a picture, and the child is the one who says
 * it: the seal.
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
                ShelfState(saved.sealed, saved.drafts, saved.soundOn, loaded = true)
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
            crayon = null,
            sealed = _shelf.value.isSealed(page.id),
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
        chime(Sfx.RUSTLE)
    }

    /** The rubber in hand, or put away. */
    fun setErasing(on: Boolean) {
        val s = _screen.value
        if (s !is Screen.Coloring) return
        if (s.erasing == on) return
        _screen.value = s.copy(erasing = on)
        chime(Sfx.RUSTLE)
    }

    /** The box of colors, held up over the page or put back down. */
    fun setBoxOpen(on: Boolean) {
        val s = _screen.value
        if (s !is Screen.Coloring) return
        if (s.boxOpen == on) return
        _screen.value = s.copy(boxOpen = on)
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
        if (s.peeking || s.boxOpen) return
        val at = clamp(p)
        if (s.erasing) {
            _screen.value = s.copy(live = Strokes.eraseDot(at))
            if (_shelf.value.soundOn) soundBoard.startRub(ERASER_RATE)
            return
        }
        val hand = s.crayon ?: colorWantedAt(s.page, at) ?: return
        _screen.value = s.copy(crayon = hand, live = Strokes.dot(hand, at))
        if (_shelf.value.soundOn) soundBoard.startRub(CRAYON_RATE)
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
        // The rub ends with the hand, whatever else happens next: a loop
        // left running after the finger is gone is the one failure this
        // sound can have.
        soundBoard.stopRub()
        val s = _screen.value
        if (s !is Screen.Coloring) return
        val live = s.live ?: return
        if (s.peeking || s.boxOpen) {
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
        if (s.peeking || s.boxOpen) return
        val region = s.page.region(index) ?: return
        val hand = s.crayon ?: region.fillArgb
        val scribble = Strokes.scribble(region, hand)
        if (scribble.isEmpty) return
        commit(s.copy(crayon = hand, erasing = false), scribble)
    }

    /** The one place a mark is finished, answered, and saved. */
    private fun commit(s: Screen.Coloring, live: Stroke) {
        val progress = s.progress.with(live)
        _screen.value = s.copy(
            progress = progress,
            live = null,
            stamp = s.stamp + 1,
        )
        // The mark itself makes no sound at all. The sound of a crayon is
        // the rub under the moving finger, started when the finger lands and
        // stopped when it lifts: a click at the end of every mark is a
        // machine answering, not a crayon on paper.
        rememberDraft(s.page, progress)
    }

    /**
     * The child says the picture is done. That is the only way a picture is
     * ever called finished: the app cannot know when a coloring is complete,
     * and it will never guess, because guessing wrong means either telling a
     * child they are done before they are or telling them they are not.
     */
    fun sealPage() {
        val s = _screen.value
        if (s !is Screen.Coloring) return
        if (s.peeking || s.boxOpen) return
        val next = !s.sealed
        _screen.value = s.copy(sealed = next, sealStamp = s.sealStamp + 1)
        if (next) chime(Sfx.CHIME)
        viewModelScope.launch {
            runCatching { store.setSealed(s.page.id, next) }.getOrNull()?.let { total ->
                _shelf.value = _shelf.value.copy(sealed = total)
            }
        }
    }

    /** Which color the area under [p] asks for, if the point is on paper. */
    private fun colorWantedAt(page: Page, p: Vec2): Long? {
        val index = page.regionIndexAt(p)
        return page.region(index)?.fillArgb
    }

    private fun clamp(p: Vec2): Vec2 =
        Vec2(p.x.coerceIn(0.0, 1.0), p.y.coerceIn(0.0, 1.0))

    /** Look at the sample closely, or put it back down. */
    fun setPeek(on: Boolean) {
        val s = _screen.value
        if (s is Screen.Coloring) _screen.value = s.copy(peeking = on, boxOpen = false)
    }

    /** Back to the shelf. Nothing is lost: the marks are already saved. */
    fun home() {
        val s = _screen.value
        if (s is Screen.Coloring) rememberDraft(s.page, s.progress)
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

    /** The effects, unless the sound switch is off. */
    private fun chime(sfx: Sfx) {
        if (_shelf.value.soundOn) soundBoard.play(sfx)
    }

    override fun onCleared() {
        soundBoard.release()
    }

    private companion object {
        const val DRAFT_SETTLE_MS = 250L

        /** The rub, as the wax sounds and as the rubber sounds. */
        const val CRAYON_RATE = 1.0f
        const val ERASER_RATE = 0.82f
    }
}
