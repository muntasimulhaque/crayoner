package io.github.muntasimulhaque.crayoner.host

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import io.github.muntasimulhaque.crayoner.R
import java.util.Collections

/**
 * SoundPool rather than MediaPlayer, deliberately (the house lesson):
 * MediaPlayer's start latency runs past 100 ms, and past about 100 ms a
 * 3-year-old no longer perceives the sound as caused by their own finger.
 * The effect only works as an immediate physical consequence of the child's
 * own action.
 *
 * One sound, nothing more: [Sfx.RUSTLE], the small paper noise of a crayon
 * being picked up or put down. Nothing plays while a finger is on the paper.
 * The wax does not answer the hand, because the mark itself is the thing, and
 * a machine hum under it is noise. There is no pitched sound in the app at
 * all, so there is no interval anywhere and no door for melody to come
 * through.
 */
enum class Sfx { RUSTLE }

class SoundBoard(context: Context) {

    private val app = context.applicationContext

    /** Sample ids, filled once during construction and read-only after. */
    private val loaded = HashMap<Sfx, Int>(1)
    private val readySamples = Collections.synchronizedSet(HashSet<Int>())

    /** Requests that arrived before their sample decoded, replayed on load. */
    private val pending = Collections.synchronizedSet(HashSet<Sfx>())

    @Volatile private var released = false

    /**
     * The pool, or null when the device refuses one (a broken audio HAL,
     * a denied native allocation). A silent game is a bug; a crashed one
     * is worse, so every use below goes through the nullable and the app
     * simply plays on without effects.
     */
    private val pool: SoundPool? = runCatching {
        SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .build()
    }.getOrNull()

    init {
        // The listener goes in before the loads, so a sample that decodes
        // on another thread can never be missed; an id whose map entry is
        // not written yet is still marked ready, which is all play() reads.
        runCatching {
            pool?.setOnLoadCompleteListener { _, sampleId, status ->
                runCatching {
                    if (status != 0) return@setOnLoadCompleteListener
                    readySamples += sampleId
                    loaded.entries.firstOrNull { it.value == sampleId }?.key?.let { wanted ->
                        if (pending.remove(wanted)) playNow(wanted)
                    }
                }
            }
        }
        loaded[Sfx.RUSTLE] = loadOrZero(R.raw.sfx_rustle)
    }

    private fun loadOrZero(resId: Int): Int = runCatching {
        pool?.load(app, resId, 1) ?: 0
    }.getOrDefault(0)

    fun play(sfx: Sfx) {
        if (released) return
        val id = loaded[sfx] ?: return
        if (id == 0 || id !in readySamples) {
            pending.add(sfx)
            return
        }
        playNow(sfx)
    }

    private fun playNow(sfx: Sfx) {
        if (released) return
        val id = loaded[sfx] ?: return
        if (id == 0) return
        val volume = volumeOf(sfx)
        runCatching { pool?.play(id, volume, volume, 1, 0, 1f) }
    }

    private fun volumeOf(sfx: Sfx) = when (sfx) {
        Sfx.RUSTLE -> 0.30f
    }

    fun release() {
        if (released) return
        released = true
        runCatching { pending.clear() }
        runCatching { pool?.release() }
    }
}
