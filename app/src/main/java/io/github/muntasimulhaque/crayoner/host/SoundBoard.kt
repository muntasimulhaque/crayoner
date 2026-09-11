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
 * Three sounds, nothing more. [RUB] is the crayon moving over the paper: it
 * loops for as long as the finger is down, because that is the sound a
 * crayon actually makes while a child colors, and it starts and stops with
 * the hand. [RUSTLE] is the crayon being picked up or put down. [CHIME] is
 * the one pitched sound in the app, struck once when the child stamps a
 * finished picture, and the board refuses to play it twice inside 1200 ms:
 * two pitched notes in quick succession make an interval, and intervals are
 * where melody starts.
 */
enum class Sfx { RUB, RUSTLE, CHIME }

class SoundBoard(context: Context) {

    private val app = context.applicationContext

    /** Sample ids, filled once during construction and read-only after. */
    private val loaded = HashMap<Sfx, Int>(3)
    private val readySamples = Collections.synchronizedSet(HashSet<Int>())

    /** Requests that arrived before their sample decoded, replayed on load. */
    private val pending = Collections.synchronizedSet(HashSet<Sfx>())

    /** The mark currently being drawn, so the rub can be stopped with it. */
    @Volatile private var rubStream: Int = 0

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
        loaded[Sfx.RUB] = loadOrZero(R.raw.sfx_rub)
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

    /**
     * Starts the rub, or keeps it going. Called while a finger is on the
     * paper: the sound of wax over paper has to last exactly as long as the
     * hand is moving, or it reads as a sound effect rather than as the
     * child's own action. [rate] is the speed of that movement: the rubber
     * travels a little slower over the paper than the crayon does.
     */
    fun startRub(rate: Float) {
        if (released) return
        if (rubStream != 0) return
        val id = loaded[Sfx.RUB] ?: return
        if (id == 0 || id !in readySamples) return
        val volume = volumeOf(Sfx.RUB)
        rubStream = runCatching {
            pool?.play(id, volume, volume, 0, -1, rate.coerceIn(0.5f, 2.0f)) ?: 0
        }.getOrDefault(0)
    }

    /** Stops the rub, at the same moment the finger lifts. */
    fun stopRub() {
        val stream = rubStream
        rubStream = 0
        if (stream == 0) return
        runCatching { pool?.stop(stream) }
    }

    private fun playNow(sfx: Sfx) {
        if (released) return
        val id = loaded[sfx] ?: return
        if (id == 0) return
        val volume = volumeOf(sfx)
        runCatching { pool?.play(id, volume, volume, 1, 0, 1f) }
    }

    private fun volumeOf(sfx: Sfx) = when (sfx) {
        Sfx.RUB -> 0.30f
        Sfx.RUSTLE -> 0.30f
        Sfx.CHIME -> 0.80f
    }

    fun release() {
        if (released) return
        released = true
        runCatching { pending.clear() }
        runCatching { pool?.release() }
        rubStream = 0
    }

    private companion object {
        const val CHIME_GAP_MS = 1200L
    }
}
