package io.github.muntasimulhaque.crayoner.host

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
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
import kotlinx.coroutines.launch

/**
 * The host performs what the domain decides. It owns which screen is up,
 * which crayon is in hand, and what the page shows. The rules themselves
 * (geometry, wax, strokes, saving) live in :core and are tested there.
 *
 * A finger draws. Down starts a mark, moving draws it, up finishes it, and
 * every finished mark is saved a fraction of a second later, so a phone
 * call, a rotation or a process death costs at most the mark in flight. No
 * network, no accounts, nothing leaving the device.
 *
 * ## Why the state is Compose state
 *
 * The screen and the mark under the finger are held as Compose state rather
 * than as flows, and it is not a style choice: it is the difference between
 * wax that lands under the fingertip and wax that trails it. A flow hands a
 * value to a collector through a coroutine dispatch, which costs a frame or
 * two; a snapshot write made inside the touch event that carried the finger
 * is read by the very next composition, in the same frame the finger moved.
 * On a slow frame the flow's hop is a mark a tenth of a second behind the
 * hand, and no amount of drawing quality makes up for that.
 *
 * Nothing here counts, scores, compares or judges anything the child does:
 * not the colors, not the areas, and not whether a picture is finished. The
 * app has no opinion about any of it.
 */
class ColoringHost(app: Application) : ViewModel() {

    private val store = CrayonStore(app)
    private val soundBoard = SoundBoard(app)

    private val _screen = mutableStateOf<Screen>(Screen.Home)
    val screen: State<Screen> = _screen

    /**
     * The mark under the finger, in its own state rather than in [screen].
     *
     * A move event changes it dozens of times a second, and it is read in
     * exactly one place: the draw that puts the wax on the paper. Keeping it
     * apart from the screen is what lets a moving finger cost a redraw of one
     * canvas instead of a recomposition of everything on the desk.
     */
    private val _live = mutableStateOf<Stroke?>(null)
    val live: State<Stroke?> = _live

    private val _shelf = mutableStateOf(ShelfState(loaded = false))
    val shelf: State<ShelfState> = _shelf

    /**
     * The sound switch on its own, for the page's bar.
     *
     * The shelf's map of drafts changes on every finished mark, and the bar
     * only ever wants the one boolean: reading the shelf's whole state there
     * would recompose the coloring screen every time a mark was saved. A
     * derived state only speaks up when the answer really changes.
     */
    val soundOn: State<Boolean> = derivedStateOf { _shelf.value.soundOn }

    /** The page's own marks, kept in memory and written a moment later. */
    private val drafts = DraftSaver(viewModelScope, store)

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
    }

    /** Open a page: its saved marks if it has any, else a blank sheet. */
    fun open(pageId: String) {
        val page = Pages.byId(pageId) ?: return
        // Nothing is in flight on a page nobody is looking at, but a mark
        // left behind would be drawn on the next page's paper, so it goes
        // here as well as everywhere else a page gives way to another.
        _live.value = null
        val saved = runCatching { store.draftFor(page.id, _shelf.value.drafts) }
            .getOrDefault(Progress.Empty)
        _screen.value = Screen.Coloring(
            page = page,
            draft = Draft.of(saved),
            crayon = null,
        )
        // A sheet comes off the wall and lands on the desk: paper being
        // handled, so it rustles like every other sheet in the app.
        play(Sfx.RUSTLE)
    }

    /**
     * Pick a crayon up. Its color is the only thing a mark can be drawn in,
     * and picking one up in a box of colors puts the rubber back down and
     * closes the lid behind the hand.
     */
    fun pickCrayon(argb: Long) {
        val s = _screen.value
        if (s !is Screen.Coloring) return
        if (!Crayons.exists(argb)) return
        if (s.crayon == argb && !s.erasing) return
        _screen.value = s.copy(crayon = argb, erasing = false, boxOpen = false)
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
        play(Sfx.RUSTLE)
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
        if (_live.value != null) return
        val next = s.draft.undo()
        if (next === s.draft) return
        commit(s, next)
    }

    /**
     * One step forward: the mark the last step back took off the paper comes
     * back exactly where it was.
     *
     * It is undo's pair and it is bounded the same way: it can only return
     * papers that were really on the desk, and drawing a new mark clears it,
     * so no press may ever put back a mark the hand has drawn over. A sheet
     * with no step behind it has nothing to put back and the press lands
     * quietly, exactly as it does in the other direction.
     */
    fun redo() {
        val s = _screen.value
        if (s !is Screen.Coloring) return
        if (_live.value != null) return
        val next = s.draft.redo()
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
        if (s.peeking || s.boxOpen || s.asking) return
        val at = clamp(p)
        if (s.erasing) {
            _live.value = Strokes.eraseDot(at)
            return
        }
        val hand = s.crayon ?: colorWantedAt(s.page, at) ?: return
        // The crayon is picked up here and nowhere else, so a first touch
        // without one leaves the child holding the color the paper under
        // their finger asked for.
        if (s.crayon == null) _screen.value = s.copy(crayon = hand)
        _live.value = Strokes.dot(hand, at)
    }

    /** The finger moved: the mark grows under it. */
    fun moveStroke(p: Vec2) {
        val s = _screen.value
        if (s !is Screen.Coloring) return
        val live = _live.value ?: return
        if (s.peeking || s.boxOpen || s.asking) return
        val grown = Strokes.extend(live, clamp(p))
        if (grown !== live) _live.value = grown
    }

    /** The finger lifted: the mark is finished, saved, and answered. */
    fun endStroke() {
        val s = _screen.value
        if (s !is Screen.Coloring) return
        val live = _live.value ?: return
        if (s.peeking || s.boxOpen || s.asking) {
            _live.value = null
            return
        }
        // The mark comes off the finger and onto the paper in one step, so
        // no frame can show it twice or show it nowhere at all.
        _live.value = null
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
        if (s.peeking || s.boxOpen || s.asking) return
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
        if (s !is Screen.Coloring) return
        if (s.peeking == on) return
        // A peek is a look, not a place to draw: a mark in flight when the
        // sample comes up is let go rather than finished, exactly as it was
        // before.
        if (on) _live.value = null
        // Lifting the finished sheet and laying it back down is paper being
        // handled, so it rustles like the crayon and the lid do.
        _screen.value = s.copy(peeking = on, boxOpen = false)
        play(Sfx.RUSTLE)
    }

    /**
     * Back to the shelf, with one question if there is anything to keep.
     *
     * A page with marks on it asks first, because a child pressing Home in
     * the middle of a picture should get the chance to say that the picture
     * is theirs to keep; a bare page goes straight home, because there is
     * nothing there a question could be about. It costs one press and both
     * answers are safe: nothing here can lose a coloring.
     */
    fun home() {
        val s = _screen.value
        if (s !is Screen.Coloring) return
        _live.value = null
        if (s.progress.strokes.isEmpty()) {
            _screen.value = Screen.Home
            return
        }
        // The marks are already on the desk where the hand left them, so the
        // question can be answered at leisure: nothing has to be written for
        // the answer to be safe.
        rememberDraft(s.page, s.draft)
        _screen.value = s.copy(asking = true, peeking = false, boxOpen = false)
    }

    /** Keep the picture: the child is done looking, and the work stays. */
    fun keepIt() {
        val s = _screen.value
        if (s !is Screen.Coloring) return
        _live.value = null
        rememberDraft(s.page, s.draft)
        _screen.value = Screen.Home
    }

    /**
     * Start this picture fresh: the marks come off the sheet and the save is
     * cleared, so the next visit opens on blank paper.
     *
     * It is the only place the app ever removes work, and it happens only
     * after two deliberate presses by two different intentions: Home, and
     * the cross. The rubber is still the way to change a picture.
     */
    fun startFresh() {
        val s = _screen.value
        if (s !is Screen.Coloring) return
        _live.value = null
        _shelf.value = _shelf.value.copy(drafts = _shelf.value.drafts - s.page.id)
        rememberDraft(s.page, Draft.Empty)
        _screen.value = Screen.Home
    }

    /**
     * Put the question away and stay on the page. It is what the system's own
     * Back does, and it is the one answer that changes nothing at all.
     */
    fun dismissAsk() {
        val s = _screen.value
        if (s !is Screen.Coloring) return
        if (!s.asking) return
        _screen.value = s.copy(asking = false)
    }

    fun setSound(on: Boolean) {
        _shelf.value = _shelf.value.copy(soundOn = on)
        viewModelScope.launch { runCatching { store.setSound(on) } }
        // The switch is the one control that has to answer with the thing it
        // governs: a parent who turns sound back on should hear it land.
        // Turning it off is silent, because the app has just been told to
        // stop making noise, and the icon already says so.
        if (on) play(Sfx.RUSTLE)
    }

    /**
     * Puts a finished draft where the shelf can see it, in memory and then
     * on disk: the mark is saved a quarter of a second later, and the
     * in-memory shelf is what [open] reads when the child comes back to the
     * page before the write lands. Updating only the disk was a bug: a
     * child who kept a picture and reopened it saw blank paper, because the
     * shelf still held the map it was born with.
     */
    private fun rememberDraft(page: Page, draft: Draft) {
        val progress = draft.progress
        val text = if (progress.isEmpty) "" else progress.serialize()
        _shelf.value = _shelf.value.remembering(page.id, text)
        drafts.save(page.id, text)
    }

    /** The effects, unless the sound switch is off. */
    private fun play(sfx: Sfx) {
        if (_shelf.value.soundOn) soundBoard.play(sfx)
    }

    override fun onCleared() {
        soundBoard.release()
    }
}
