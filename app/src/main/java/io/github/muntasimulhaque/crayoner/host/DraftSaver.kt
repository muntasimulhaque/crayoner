package io.github.muntasimulhaque.crayoner.host

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * The page's marks on their way to the disk.
 *
 * Coloring a picture is hundreds of marks, and each one changes the paper a
 * little; writing the whole draft on every finished mark would turn a drawing
 * hand into a stream of disk writes. So the writes are conflated and put off
 * by a quarter of a second: the paper is written once the hand pauses, and
 * everything the child did inside [SETTLE_MS] travels in that one write.
 *
 * The window is short enough for what it protects: a phone call, a rotation
 * or a process death costs at most the mark in flight. The shelf in memory is
 * updated the moment a draft is remembered (see `ColoringHost.rememberDraft`),
 * so this class is only ever the disk's half of a save.
 *
 * Nothing here judges or counts anything: it writes the marks the child's own
 * hand put on the paper, exactly as they are.
 */
internal class DraftSaver(
    private val scope: CoroutineScope,
    private val store: CrayonStore,
) {

    /** Draft writes are conflated and slightly delayed, never per mark. */
    private val writes = MutableSharedFlow<Write>(extraBufferCapacity = 1)

    private class Write(val pageId: String, val text: String)

    init {
        scope.launch {
            writes.collectLatest { write ->
                delay(SETTLE_MS)
                runCatching { store.saveDraft(write.pageId, write.text) }
            }
        }
    }

    /**
     * Hands one finished draft over: the page it belongs to, and the marks as
     * the store's own text. A draft that arrives while an earlier one is
     * still inside the settle window replaces it, which is what conflation
     * means here.
     */
    fun save(pageId: String, text: String) {
        writes.tryEmit(Write(pageId, text))
    }

    private companion object {
        /** How long a draft waits for more marks before it is written. */
        const val SETTLE_MS = 250L
    }
}
