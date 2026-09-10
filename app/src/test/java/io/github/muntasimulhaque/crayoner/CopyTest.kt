package io.github.muntasimulhaque.crayoner

import io.github.muntasimulhaque.crayoner.core.Crayons
import io.github.muntasimulhaque.crayoner.core.Pages
import io.github.muntasimulhaque.crayoner.ui.areaLabel
import io.github.muntasimulhaque.crayoner.ui.areaNameRes
import io.github.muntasimulhaque.crayoner.ui.crayonNameRes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The two places where the app's own tables can drift: the word for a
 * crayon and the word for an area. Both are resources, so this test lives in
 * the app module; both are reachable by a screen reader, so a missing entry
 * is a bug a child would hear, not a cosmetic gap.
 */
class CopyTest {

    @Test
    fun everyCrayonHasItsOwnWord() {
        val names = Crayons.all.map { crayonNameRes(it) }
        assertEquals(
            "two crayons share a name",
            names.size,
            names.toSet().size,
        )
        for ((index, name) in names.withIndex()) {
            assertNotEquals(
                "crayon ${Crayons.all[index]} falls back to the app name",
                R.string.app_name,
                name,
            )
        }
    }

    @Test
    fun everyAreaKindInTheBookHasAWord() {
        val kinds = Pages.all.flatMap { page -> page.regions.map { it.kind } }.toSet()
        for (kind in kinds) {
            assertNotEquals(
                "area kind $kind falls back to the app name",
                R.string.app_name,
                areaNameRes(kind),
            )
        }
    }

    @Test
    fun theAreaLabelSaysWhatIsNeededWithoutScolding() {
        assertEquals("sun, done", areaLabel("sun", done = true, wanted = "yellow", ready = false))
        assertEquals(
            "sun, needs yellow, ready to color",
            areaLabel("sun", done = false, wanted = "yellow", ready = true),
        )
        assertEquals("sun, needs yellow", areaLabel("sun", done = false, wanted = "yellow", ready = false))
        // No label may ever carry a judgment of the child.
        for (label in listOf(
            areaLabel("sky", done = false, wanted = "blue", ready = false),
            areaLabel("sky", done = false, wanted = "blue", ready = true),
            areaLabel("sky", done = true, wanted = "blue", ready = false),
        )) {
            for (bad in listOf("wrong", "incorrect", "oops", "try again", "no ")) {
                assertTrue("$label contains a scold", !label.contains(bad))
            }
        }
    }
}
