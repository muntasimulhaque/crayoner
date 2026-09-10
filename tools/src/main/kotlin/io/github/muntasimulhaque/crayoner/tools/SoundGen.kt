package io.github.muntasimulhaque.crayoner.tools

import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.outputStream
import kotlin.system.exitProcess

/**
 * Crayoner's four sound effects, synthesized to spec: tiny, license free,
 * and deterministic down to the byte.
 *
 * The religious constraint is a design input here, not an afterthought:
 * there is no music in this app. The paint, the tick and the rustle are
 * deliberately inharmonic: short noise led bursts with no pitched partials,
 * so they read as physical events (a crayon touching paper, a color landing
 * where the picture asks, a crayon leaving its seat) rather than as notes.
 * Only [chime] has a pitch, it is a single struck bell, and the app never
 * plays it twice inside 1200 ms, because two pitched notes in sequence make
 * an interval and intervals are where melody starts.
 */
object SoundGen {

    private const val RATE = 44100
    private val NAMES = listOf("sfx_paint", "sfx_tick", "sfx_rustle", "sfx_chime")

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
                kotlin.math.exp(-curve * t)
            }
        }
        return out
    }

    private fun noise(n: Int, rng: CpythonRandom): DoubleArray =
        DoubleArray(n) { rng.uniform(-1.0, 1.0) }

    /** One pole low pass; enough to turn white noise into something soft. */
    private fun lowpass(samples: DoubleArray, cutoff: Double): DoubleArray {
        val alpha = 1.0 - kotlin.math.exp(-2.0 * Math.PI * cutoff / RATE)
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
        val alpha = 1.0 / (1.0 + 2.0 * Math.PI * cutoff / RATE)
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

    /** Sum of damped sinusoids. Non integer ratios keep it inharmonic. */
    private fun partials(n: Int, freqs: DoubleArray, decays: DoubleArray, gains: DoubleArray): DoubleArray {
        val out = DoubleArray(n)
        for ((layer, f) in freqs.withIndex()) {
            val d = decays[layer]
            val g = gains[layer]
            for (i in 0 until n) {
                val t = i.toDouble() / RATE
                out[i] += g * kotlin.math.sin(2.0 * Math.PI * f * t) * kotlin.math.exp(-t / d)
            }
        }
        return out
    }

    private fun mix(vararg layers: DoubleArray): DoubleArray {
        val n = layers.maxOf { it.size }
        val out = DoubleArray(n)
        for (layer in layers) for (i in layer.indices) out[i] += layer[i]
        return out
    }

    private operator fun DoubleArray.times(k: Double): DoubleArray = DoubleArray(size) { this[it] * k }

    private fun applyEnv(samples: DoubleArray, env: DoubleArray): DoubleArray =
        DoubleArray(samples.size) { samples[it] * env[it] }

    // -- The four sounds -------------------------------------------------------

    /** 90 ms: a crayon touching paper. A soft, very quiet scratch. */
    private fun paint(rng: CpythonRandom): DoubleArray {
        val n = (0.090 * RATE).toInt()
        val body = lowpass(highpass(noise(n, rng), 900.0), 5200.0) * 0.5
        return applyEnv(body, envelope(n, 0.004, 0.026, curve = 3.6))
    }

    /** 70 ms: a color landing where the picture asks. A small wooden tick. */
    private fun tick(rng: CpythonRandom): DoubleArray {
        val n = (0.070 * RATE).toInt()
        val body = lowpass(noise(n, rng), 3600.0) * 0.45
        val knock = partials(
            n,
            doubleArrayOf(420.0, 690.0, 1040.0),
            doubleArrayOf(0.016, 0.010, 0.006),
            doubleArrayOf(0.8, 0.35, 0.15),
        )
        return applyEnv(mix(body, knock), envelope(n, 0.0008, 0.022, curve = 4.5))
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
        val bell = partials(
            n,
            doubleArrayOf(f, f * 2.0, f * 3.01, f * 4.17),
            doubleArrayOf(0.240, 0.160, 0.095, 0.055),
            doubleArrayOf(1.0, 0.34, 0.15, 0.07),
        )
        return applyEnv(bell, envelope(n, 0.004, 0.200, curve = 2.2))
    }

    // -- Output -----------------------------------------------------------------

    /** Normalizes, fades the tail, and writes a canonical 16 bit mono WAV. */
    private fun write(outDir: Path, name: String, samples: DoubleArray, peak: Double) {
        val n = samples.size
        val high = samples.maxOf { kotlin.math.abs(it) }.takeIf { it > 0 } ?: 1.0
        val scale = peak / high
        val fade = minOf((0.006 * RATE).toInt(), n)
        val frames = ByteArrayOutputStream(n * 2)
        for (i in 0 until n) {
            var v = samples[i] * scale
            if (i >= n - fade) v *= (n - i).toDouble() / fade
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
        write(outDir, "sfx_paint", paint(rng), peak = 0.32)
        write(outDir, "sfx_tick", tick(rng), peak = 0.55)
        write(outDir, "sfx_rustle", rustle(rng), peak = 0.30)
        write(outDir, "sfx_chime", chime(), peak = 0.70)
    }

    /**
     * Regenerates into a temp dir and byte compares against the committed
     * WAVs; exits non zero if any committed asset would change, so an
     * accidental binary edit cannot ride along unnoticed until release.
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
