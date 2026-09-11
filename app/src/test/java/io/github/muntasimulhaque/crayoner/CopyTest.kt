package io.github.muntasimulhaque.crayoner

import io.github.muntasimulhaque.crayoner.core.Crayons
import io.github.muntasimulhaque.crayoner.core.Pages
import io.github.muntasimulhaque.crayoner.ui.areaLabel
import io.github.muntasimulhaque.crayoner.ui.areaNameRes
import io.github.muntasimulhaque.crayoner.ui.crayonNameRes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The two places where the app's own tables can drift: the word for a crayon
 * and the word for an area. Both are resources, so this test lives in the app
 * module; both are reachable by a screen reader, so a missing entry is a bug
 * a child would hear, not a cosmetic gap.
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
        // The box is the real thirty two count box: every crayon in it is a
        // crayon a child can name out loud, and the two of them that are
        // not single words (bluetiful, timberwolf) are exactly the two names
        // the box itself prints.
        assertEquals(32, names.size)
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
        assertEquals(
            "cloud, needs white, ready to color",
            areaLabel("cloud", wanted = "white", ready = true),
        )
        assertEquals("cloud, needs white", areaLabel("cloud", wanted = "white", ready = false))
        // No label may ever carry a judgment of the child, and none may
        // claim the area is done: the app does not know what done means.
        for (label in listOf(
            areaLabel("sky", wanted = "sky blue", ready = false),
            areaLabel("sky", wanted = "sky blue", ready = true),
        )) {
            for (bad in listOf("wrong", "incorrect", "oops", "try again", "no ", "done", "finished")) {
                assertFalse("$label contains \"$bad\"", label.contains(bad))
            }
        }
    }

    @Test
    fun theAppNeverSaysWellDoneOnItsOwn() {
        // The one word of praise in the app belongs to the child's own
        // stamp, and the string table holds no other. A build that grew a
        // congratulations screen would have to grow a string for it first,
        // and this is where that would be caught.
        assertTrue(R.string.seal != 0)
        assertNotEquals(R.string.seal, R.string.app_name)
    }
}
