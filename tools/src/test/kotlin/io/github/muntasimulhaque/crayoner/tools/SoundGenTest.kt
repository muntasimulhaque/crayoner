package io.github.muntasimulhaque.crayoner.tools

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The one effect, held to the rules that make it usable: deterministic down
 * to the byte, faded to silence at its own tail, and carrying no pitch at
 * all. The app makes one sound and nothing else, so anything else in the raw
 * folder is a bug.
 */
class SoundGenTest {

    private fun generate(): File {
        val dir = Files.createTempDirectory("crayoner-soundgen").toFile()
        SoundGen.generateAll(dir.toPath())
        return dir
    }

    /** A canonical 16 bit mono WAV, as the samples it holds. */
    private fun readWav(file: File): ShortArray {
        val bytes = file.readBytes()
        assertEquals("RIFF", String(bytes, 0, 4, Charsets.US_ASCII))
        assertEquals("WAVE", String(bytes, 8, 4, Charsets.US_ASCII))
        assertEquals("data", String(bytes, 36, 4, Charsets.US_ASCII))
        val dataSize = ByteBuffer.wrap(bytes, 40, 4).order(ByteOrder.LITTLE_ENDIAN).int
        val out = ShortArray(dataSize / 2)
        val buf = ByteBuffer.wrap(bytes, 44, dataSize).order(ByteOrder.LITTLE_ENDIAN)
        for (i in out.indices) out[i] = buf.short
        return out
    }

    @Test
    fun everyEffectIsGenerated() {
        val dir = generate()
        val names = dir.listFiles()?.map { it.name }?.toSet() ?: emptySet()
        assertEquals(setOf("sfx_rustle.wav"), names)
        // The app plays one effect, and nothing else may be lying in the raw
        // folder hoping to be played: no rub while the wax is moving, and no
        // bell for anything.
        assertFalse(names.contains("sfx_rub.wav"))
        assertFalse(names.contains("sfx_chime.wav"))
        assertFalse(names.contains("sfx_paint.wav"))
    }

    @Test
    fun theEffectEndsInSilence() {
        // A one shot that stops mid wave pops when it is cut, so it is faded
        // to nothing at its own tail.
        val samples = readWav(File(generate(), "sfx_rustle.wav"))
        val tail = samples.takeLast(64).map { kotlin.math.abs(it.toInt()) }.max()
        assertTrue("the rustle ends at $tail, not silence", tail < 64)
    }

    @Test
    fun nothingIsClippedAndNothingIsSilent() {
        val samples = readWav(File(generate(), "sfx_rustle.wav"))
        val peak = samples.maxOf { kotlin.math.abs(it.toInt()) }
        assertTrue("the rustle is silent", peak > 3000)
        assertTrue("the rustle is clipped at $peak", peak <= 32767)
    }

    @Test
    fun theSameEffectComesOutOfTheGeneratorEveryTime() {
        // Deterministic by construction, and pinned by checkSounds against
        // the committed file; this is the cheap version of that promise,
        // runnable without the repository.
        val a = File(generate(), "sfx_rustle.wav").readBytes()
        val b = File(generate(), "sfx_rustle.wav").readBytes()
        assertTrue("the rustle differs between runs", a.contentEquals(b))
    }

    @Test
    fun nothingInTheAppIsPitched() {
        // The rustle is noise: its energy is spread across a band, with no
        // one frequency carrying the sound. A probe at a few musical notes
        // finds nothing singing, because two pitched notes in sequence make
        // an interval, and intervals are where melody starts.
        val rustle = readWav(File(generate(), "sfx_rustle.wav"))
        for (hz in listOf(261.63, 523.25, 1046.5, 2093.0)) {
            val energy = toneEnergy(rustle, hz)
            assertTrue("the rustle sings at $hz Hz (energy $energy)", energy < 0.05)
        }
    }

    /** How much of [samples]' energy sits at [hz], by a single DFT bin. */
    private fun toneEnergy(samples: ShortArray, hz: Double): Double {
        var real = 0.0
        var imag = 0.0
        var total = 0.0
        val w = 2.0 * Math.PI * hz / 44100.0
        for (i in samples.indices) {
            val v = samples[i] / 32768.0
            real += v * kotlin.math.cos(w * i)
            imag += v * kotlin.math.sin(w * i)
            total += v * v
        }
        val power = (real * real + imag * imag) / samples.size
        return if (total <= 0.0) 0.0 else power / (total / samples.size * 64.0)
    }
}
