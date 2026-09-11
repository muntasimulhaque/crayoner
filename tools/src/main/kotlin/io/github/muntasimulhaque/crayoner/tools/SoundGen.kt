package io.github.muntasimulhaque.crayoner.tools

import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.outputStream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.system.exitProcess

/**
 * Crayoner's three sound effects, synthesized to spec: tiny, license free,
 * and deterministic down to the byte.
 *
 * The religious constraint is a design input here, not an afterthought:
 * there is no music in this app. The rub and the rustle are deliberately
 * inharmonic: noise, shaped, with no pitched partials at all, so they read
 * as physical events (wax dragged over paper, a crayon lifted from its seat)
 * rather than as notes. Only [chime] has a pitch, it is a single struck
 * bell, and the app never plays it twice inside 1200 ms, because two pitched
 * notes in sequence make an interval and intervals are where melody starts.
 *
 * [rub] is also the only sound in the app that loops: a real crayon makes a
 * quiet, continuous scratch for as long as the hand keeps moving, so the
 * loop is built to be seamless, out of harmonics of its own length, and it
 * carries no attack and no decay of its own. The hand is the envelope.
 */
object SoundGen {

    private const val RATE = 44100
    private val NAMES = listOf("sfx_rub", "sfx_rustle", "sfx_chime")

    // -- DSP ------------------------------------------------------------------

    /** Percussive envelope: near instant attack, exponential decay. */
    private fun envelope(n: Int, attack: Double, decay: Double, curve: Double = 3.0): DoubleArray {
        val out = DoubleArray(n)
        val a = maxOf(1, (attack * RATE).toInt())
        val denom = maxOf(1.0, decay * RATE)
        for (i in 0 until n) {
            out[i] = if (i < a) {
                i.toDouble() / a
            } else {
                val t = (i - a).toDouble() / denom
                exp(-curve * t)
            }
        }
        return out
    }

    private fun noise(n: Int, rng: CpythonRandom): DoubleArray =
        DoubleArray(n) { rng.uniform(-1.0, 1.0) }

    /** One pole low pass; enough to turn white noise into something soft. */
    private fun lowpass(samples: DoubleArray, cutoff: Double): DoubleArray {
        val alpha = 1.0 - exp(-2.0 * PI * cutoff / RATE)
        val out = DoubleArray(samples.size)
        var prev = 0.0
        for (i in samples.indices) {
            prev += alpha * (samples[i] - prev)
            out[i] = prev
        }
        return out
    }

    /** A gentle high pass, so a noise led effect keeps a little air. */
    private fun highpass(samples: DoubleArray, cutoff: Double): DoubleArray {
        val alpha = 1.0 / (1.0 + 2.0 * PI * cutoff / RATE)
        val out = DoubleArray(samples.size)
        var prevIn = 0.0
        var prevOut = 0.0
        for (i in samples.indices) {
            prevOut = alpha * (prevOut + samples[i] - prevIn)
            prevIn = samples[i]
            out[i] = prevOut
        }
        return out
    }

    /**
     * Noise that loops: every partial is a whole number of cycles across the
     * buffer, so the last sample runs into the first without a step. A rub
     * that clicks once per loop is a metronome, and a metronome is music's
     * front door.
     *
     * The band is the sound of the thing: a crayon over paper lives between
     * roughly 1.5 and 7 kHz, with more energy up top when the wax is thin and
     * a duller, grainier middle when it is laid on thick.
     */
    private fun loopNoise(
        seconds: Double,
        rng: CpythonRandom,
        lowHz: Double,
        highHz: Double,
        partials: Int = 420,
    ): DoubleArray {
        val n = (seconds * RATE).toInt()
        val out = DoubleArray(n)
        val base = 1.0 / seconds
        val lo = (lowHz / base).toInt().coerceAtLeast(1)
        val hi = (highHz / base).toInt().coerceAtLeast(lo + 1)
        for (k in lo..hi) {
            // Sparse picks across the band: adjacent bins in phase agreement
            // would add up to a tone, and a tone is not paper.
            if (rng.uniform(0.0, 1.0) > 0.32) continue
            val f = k * base
            val a = rng.uniform(-1.0, 1.0)
            val b = rng.uniform(-1.0, 1.0)
            // Louder through the middle of the band, quieter at both ends.
            val t = (f - lowHz) / (highHz - lowHz)
            val tilt = sin(PI * t.coerceIn(0.0, 1.0))
            val amp = tilt * 0.55 + 0.45
            val step = 2.0 * PI * f / RATE
            var phase = 0.0
            for (i in 0 until n) {
                out[i] += amp * (a * cos(phase) + b * sin(phase))
                phase += step
            }
        }
        return out
    }

    /** Scales a buffer to its own peak, so the peak is one. */
    private fun normalize(samples: DoubleArray): DoubleArray {
        val high = samples.maxOf { kotlin.math.abs(it) }.takeIf { it > 0.0 } ?: 1.0
        return DoubleArray(samples.size) { samples[it] / high }
    }

    private operator fun DoubleArray.times(k: Double): DoubleArray = DoubleArray(size) { this[it] * k }

    private operator fun DoubleArray.plus(other: DoubleArray): DoubleArray =
        DoubleArray(size) { this[it] + other[it] }

    private fun applyEnv(samples: DoubleArray, env: DoubleArray): DoubleArray =
        DoubleArray(samples.size) { samples[it] * env[it] }

    // -- The three sounds ------------------------------------------------------

    /**
     * 1.20 s: a crayon moving over paper, made to be played in a loop while
     * the child's finger is down. It has no attack and no decay of its own,
     * because the finger is the envelope: the sound starts when the wax
     * touches the paper and stops when it lifts, exactly as it does in a
     * room. The grain of the wax is the amplitude modulation: a hand does not
     * push evenly, and the paper's tooth makes it catch, several times a
     * second, in no pattern at all.
     */
    private fun rub(rng: CpythonRandom): DoubleArray {
        val n = (1.20 * RATE).toInt()
        // The body: broadband paper hiss, and a grainy lower layer for the
        // wax itself.
        val hiss = loopNoise(1.20, rng, 1500.0, 7200.0)
        val grain = loopNoise(1.20, rng, 380.0, 1600.0, partials = 90) * 0.32
        val body = hiss + grain
        // A slow wobble, also a whole number of cycles per loop so the join
        // stays silent, so the rub breathes the way a moving hand does.
        val wobble = DoubleArray(n) { i ->
            val t = i.toDouble() / RATE
            1.0 +
                0.22 * sin(2.0 * PI * 1.0 / 1.20 * t) +
                0.14 * sin(2.0 * PI * 2.0 / 1.20 * t + 0.7) +
                0.10 * sin(2.0 * PI * 4.0 / 1.20 * t + 2.1)
        }
        return normalize(DoubleArray(n) { body[it] * wobble[it] })
    }

    /** 60 ms: a crayon lifted from its seat. Paper rustle, no pitch at all. */
    private fun rustle(rng: CpythonRandom): DoubleArray {
        val n = (0.060 * RATE).toInt()
        val body = highpass(lowpass(noise(n, rng), 7000.0), 1800.0) * 0.7
        return applyEnv(body, envelope(n, 0.003, 0.016, curve = 4.0))
    }

    /** 480 ms: one soft struck bell. The only pitched sound in the app. */
    private fun chime(): DoubleArray {
        val n = (0.480 * RATE).toInt()
        val f = 523.25 // a single note, struck once, never followed by another
        val out = DoubleArray(n)
        val freqs = doubleArrayOf(f, f * 2.0, f * 3.01, f * 4.17)
        val decays = doubleArrayOf(0.240, 0.160, 0.095, 0.055)
        val gains = doubleArrayOf(1.0, 0.34, 0.15, 0.07)
        for (layer in freqs.indices) {
            for (i in 0 until n) {
                val t = i.toDouble() / RATE
                out[i] += gains[layer] * sin(2.0 * PI * freqs[layer] * t) * exp(-t / decays[layer])
            }
        }
        return applyEnv(out, envelope(n, 0.004, 0.200, curve = 2.2))
    }

    // -- Output -----------------------------------------------------------------

    /** Normalizes, fades the tail, and writes a canonical 16 bit mono WAV. */
    private fun write(outDir: Path, name: String, samples: DoubleArray, peak: Double, loop: Boolean = false) {
        val n = samples.size
        val high = samples.maxOf { kotlin.math.abs(it) }.takeIf { it > 0 } ?: 1.0
        val scale = peak / high
        // A looping effect is never faded: its ends have to meet, and the
        // loop seam is already silent by construction.
        val fade = if (loop) 0 else minOf((0.006 * RATE).toInt(), n)
        val frames = ByteArrayOutputStream(n * 2)
        for (i in 0 until n) {
            var v = samples[i] * scale
            if (fade > 0 && i >= n - fade) v *= (n - i).toDouble() / fade
            val q = (v * 32767.0).toInt().coerceIn(-32767, 32767)
            frames.write(q and 0xFF)
            frames.write((q shr 8) and 0xFF)
        }
        val path = outDir.resolve("$name.wav")
        path.outputStream().use { stream ->
            val data = frames.toByteArray()
            val header = ByteArray(44)
            fun putU32(at: Int, v: Int) {
                header[at] = (v and 0xFF).toByte()
                header[at + 1] = ((v shr 8) and 0xFF).toByte()
                header[at + 2] = ((v shr 16) and 0xFF).toByte()
                header[at + 3] = ((v shr 24) and 0xFF).toByte()
            }
            fun putU16(at: Int, v: Int) {
                header[at] = (v and 0xFF).toByte()
                header[at + 1] = ((v shr 8) and 0xFF).toByte()
            }
            val riffSize = 36 + data.size
            "RIFF".toByteArray().copyInto(header, 0)
            putU32(4, riffSize)
            "WAVE".toByteArray().copyInto(header, 8)
            "fmt ".toByteArray().copyInto(header, 12)
            putU32(16, 16)
            putU16(20, 1) // PCM
            putU16(22, 1) // mono
            putU32(24, RATE)
            putU32(28, RATE * 2)
            putU16(32, 2)
            putU16(34, 16)
            "data".toByteArray().copyInto(header, 36)
            putU32(40, data.size)
            stream.write(header)
            stream.write(data)
        }
        val kb = Files.size(path) / 1024.0
        println("${path.absolutePathString()}  ${n * 1000 / RATE} ms  ${"%.1f".format(kb)} KB")
    }

    /** Regenerates every asset, in one fixed order, into [outDir]. */
    fun generateAll(outDir: Path) {
        Files.createDirectories(outDir)
        val rng = CpythonRandom(20260911L)
        write(outDir, "sfx_rub", rub(rng), peak = 0.30, loop = true)
        write(outDir, "sfx_rustle", rustle(rng), peak = 0.30)
        write(outDir, "sfx_chime", chime(), peak = 0.70)
    }

    /**
     * Regenerates into a temp dir and byte compares against the committed
     * WAVs; exits non zero if any committed asset would change, so an
     * accidental binary edit cannot ride along unnoticed until release. A
     * committed asset that is no longer generated at all is reported too.
     */
    fun check(rawDir: Path): Int {
        val tmp = Files.createTempDirectory("crayoner-sounds")
        generateAll(tmp)
        val bad = mutableListOf<String>()
        for (name in NAMES) {
            val committed = rawDir.resolve("$name.wav")
            if (!Files.exists(committed)) {
                bad += "${committed.absolutePathString()} is missing"
                continue
            }
            val fresh = Files.readAllBytes(tmp.resolve("$name.wav"))
            if (!fresh.contentEquals(Files.readAllBytes(committed))) {
                bad += "${committed.absolutePathString()} differs from a fresh regeneration"
            }
        }
        val known = NAMES.map { "$it.wav" }.toSet()
        Files.list(rawDir).use { files ->
            files.filter { it.fileName.toString().startsWith("sfx_") }
                .forEach { stray ->
                    if (stray.fileName.toString() !in known) {
                        bad += "${stray.absolutePathString()} is no effect this app plays"
                    }
                }
        }
        return if (bad.isNotEmpty()) {
            for (line in bad) println("MISMATCH: $line")
            1
        } else {
            println("All sound assets match a fresh regeneration.")
            0
        }
    }
}

fun main(args: Array<String>) {
    val rootDir = Path.of(args[0])
    val rawDir = rootDir.resolve("app/src/main/res/raw")
    if (args.size > 1 && args[1] == "--check") {
        exitProcess(SoundGen.check(rawDir))
    }
    SoundGen.generateAll(rawDir)
}
