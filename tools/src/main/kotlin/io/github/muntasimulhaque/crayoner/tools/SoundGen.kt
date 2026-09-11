package io.github.muntasimulhaque.crayoner.tools

import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.outputStream
import kotlin.math.PI
import kotlin.math.exp
import kotlin.system.exitProcess

/**
 * Crayoner's one sound effect, synthesized to spec: tiny, license free, and
 * deterministic down to the byte.
 *
 * The sound rules are design inputs here, not an afterthought: there is no
 * music in this app, and nothing pitched anywhere in it. [rustle] is noise
 * with no partials at all, shaped into 60 ms of paper, so it reads as a
 * physical event (a crayon lifted from its seat) rather than as a note. The
 * wax itself is silent: nothing plays while a finger is on the paper.
 */
object SoundGen {

    private const val RATE = 44100
    private val NAMES = listOf("sfx_rustle")

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

    private fun applyEnv(samples: DoubleArray, env: DoubleArray): DoubleArray =
        DoubleArray(samples.size) { samples[it] * env[it] }

    // -- The one sound ---------------------------------------------------------

    /** 60 ms: a crayon lifted from its seat. Paper rustle, no pitch at all. */
    private fun rustle(rng: CpythonRandom): DoubleArray {
        val n = (0.060 * RATE).toInt()
        val shaped = highpass(lowpass(noise(n, rng), 7000.0), 1800.0)
        val body = DoubleArray(n) { shaped[it] * 0.7 }
        return applyEnv(body, envelope(n, 0.003, 0.016, curve = 4.0))
    }

    // -- Output -----------------------------------------------------------------

    /** Normalizes, fades the tail, and writes a canonical 16 bit mono WAV. */
    private fun write(outDir: Path, name: String, samples: DoubleArray, peak: Double) {
        val n = samples.size
        val high = samples.maxOf { kotlin.math.abs(it) }.takeIf { it > 0 } ?: 1.0
        val scale = peak / high
        // A one shot that stops mid wave pops when it is cut: every effect
        // fades to nothing over its own last few milliseconds.
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
        write(outDir, "sfx_rustle", rustle(CpythonRandom(20260911L)), peak = 0.30)
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
