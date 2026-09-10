package io.github.muntasimulhaque.crayoner.host

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock
import io.github.muntasimulhaque.crayoner.R
import java.util.Collections

/**
 * SoundPool rather than MediaPlayer, deliberately (the house lesson):
 * MediaPlayer's start latency runs past 100 ms, and past about 100 ms a
 * 3-year-old no longer perceives the sound as caused by their own finger.
 * The effects only work as immediate physical consequences of the child's
 * own actions.
 *
 * Four sounds, nothing more: the tap of a crayon touching paper, the tick
 * when a region is the color the picture asks for, the rustle when the
 * crayon changes, and one bell for a finished picture. Only [Sfx.CHIME]
 * has a pitch, and the board refuses to play it twice inside 1200 ms: two
 * pitched notes in quick succession make an interval, and intervals are
 * where melody starts.
 */
enum class Sfx { PAINT, TICK, RUSTLE, CHIME }

class SoundBoard(context: Context) {

    private val app = context.applicationContext

    /** Sample ids, filled once during construction and read-only after. */
    private val loaded = HashMap<Sfx, Int>(4)
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
        loaded[Sfx.PAINT] = loadOrZero(R.raw.sfx_paint)
        loaded[Sfx.TICK] = loadOrZero(R.raw.sfx_tick)
        loaded[Sfx.RUSTLE] = loadOrZero(R.raw.sfx_rustle)
        loaded[Sfx.CHIME] = loadOrZero(R.raw.sfx_chime)
    }

    private fun loadOrZero(resId: Int): Int = runCatching {
        pool?.load(app, resId, 1) ?: 0
    }.getOrDefault(0)

    @Volatile private var lastChimeAt = 0L

    fun play(sfx: Sfx) {
        if (released) return
        val id = loaded[sfx] ?: return
        if (sfx == Sfx.CHIME) {
            val now = runCatching { SystemClock.elapsedRealtime() }.getOrDefault(0L)
            if (now - lastChimeAt < CHIME_GAP_MS) return
            lastChimeAt = now
        }
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
        Sfx.PAINT -> 0.45f
        Sfx.TICK -> 0.60f
        Sfx.RUSTLE -> 0.35f
        Sfx.CHIME -> 0.80f
    }

    fun release() {
        if (released) return
        released = true
        runCatching { pending.clear() }
        runCatching { pool?.release() }
    }

    private companion object {
        const val CHIME_GAP_MS = 1200L
    }
}
