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
 * The three effects, held to the rules that make them usable: the rub has to
 * loop without a click, everything is deterministic down to the byte, and
 * only one of them is allowed to have a pitch at all.
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
        assertEquals(
            setOf("sfx_rub.wav", "sfx_rustle.wav", "sfx_chime.wav"),
            names,
        )
        // The app plays three effects, and nothing else may be lying in the
        // raw folder hoping to be played.
        assertFalse(names.contains("sfx_paint.wav"))
        assertFalse(names.contains("sfx_tick.wav"))
    }

    @Test
    fun theRubLoopsWithoutAClick() {
        // A loop that steps at its own seam clicks once per pass, and a click
        // on a timer is a metronome, which is music's front door. The seam's
        // step has to be no worse than the ordinary step between two samples
        // anywhere else in the buffer.
        val rub = readWav(File(generate(), "sfx_rub.wav"))
        assertTrue("the rub is too short to loop", rub.size > 44100)
        val seam = kotlin.math.abs(rub[0] - rub[rub.size - 1]).toInt()
        var worst = 0
        for (i in 1 until rub.size) {
            worst = maxOf(worst, kotlin.math.abs(rub[i] - rub[i - 1]).toInt())
        }
        assertTrue("the loop seam steps by $seam against a worst step of $worst", seam <= worst)
    }

    @Test
    fun theRubCarriesNoAttackOrDecayOfItsOwn() {
        // The hand is the envelope: the loop is trimmed flat, so the sound
        // starts when the finger lands and stops when it lifts.
        val rub = readWav(File(generate(), "sfx_rub.wav"))
        val head = rub.take(441).map { kotlin.math.abs(it.toInt()) }.average()
        val middle = rub.slice(rub.size / 2 - 220 until rub.size / 2 + 220)
            .map { kotlin.math.abs(it.toInt()) }.average()
        // The head is allowed to be a touch quieter than the middle, but it
        // must not be the near silence of an attack ramp.
        assertTrue("the rub fades in: head $head against middle $middle", head > middle * 0.5)
    }

    @Test
    fun theOneShotEffectsEndInSilence() {
        // A one shot that stops mid wave pops when it is cut. Both of the
        // others are faded to nothing at their own tail.
        for (name in listOf("sfx_rustle.wav", "sfx_chime.wav")) {
            val samples = readWav(File(generate(), name))
            val tail = samples.takeLast(64).map { kotlin.math.abs(it.toInt()) }.max()
            assertTrue("$name ends at $tail, not silence", tail < 64)
        }
    }

    @Test
    fun nothingIsClippedAndNothingIsSilent() {
        val dir = generate()
        for (name in listOf("sfx_rub.wav", "sfx_rustle.wav", "sfx_chime.wav")) {
            val samples = readWav(File(dir, name))
            val peak = samples.maxOf { kotlin.math.abs(it.toInt()) }
            assertTrue("$name is silent", peak > 3000)
            assertTrue("$name is clipped at $peak", peak <= 32767)
        }
    }

    @Test
    fun theSameEffectComesOutOfTheGeneratorEveryTime() {
        // Deterministic by construction, and pinned by checkSounds against
        // the committed files; this is the cheap version of that promise,
        // runnable without the repository.
        val a = File(generate(), "sfx_rub.wav").readBytes()
        val b = File(generate(), "sfx_rub.wav").readBytes()
        assertTrue("the rub differs between runs", a.contentEquals(b))
    }

    @Test
    fun theChimeIsTheOnlyPitchedSound() {
        // The rub and the rustle are noise: their energy is spread across a
        // band, with no one frequency carrying the sound. The chime is a
        // struck bell and is allowed exactly one fundamental. A Goertzel
        // probe at the bell's own note finds the chime and not the others.
        val dir = generate()
        val chime = toneEnergy(readWav(File(dir, "sfx_chime.wav")), 523.25)
        val rub = toneEnergy(readWav(File(dir, "sfx_rub.wav")), 523.25)
        val rustle = toneEnergy(readWav(File(dir, "sfx_rustle.wav")), 523.25)
        assertTrue("the chime has no note at all", chime > 0.05)
        assertTrue("the rub sings", rub < chime / 4.0)
        assertTrue("the rustle sings", rustle < chime / 4.0)
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
