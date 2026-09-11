package io.github.muntasimulhaque.crayoner

import io.github.muntasimulhaque.crayoner.core.Crayons
import io.github.muntasimulhaque.crayoner.core.Pages
import io.github.muntasimulhaque.crayoner.ui.areaLabel
import io.github.muntasimulhaque.crayoner.ui.areaNameRes
import io.github.muntasimulhaque.crayoner.ui.crayonNameRes
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

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
    fun noWordInTheAppJudgesOrFinishes() {
        // Every word a person can read in this app lives in the one string
        // table, so that is where a build that grew a judgment would have to
        // put it first. Nothing here says a picture is done, and nothing
        // praises or corrects a child: there is no finished state anywhere,
        // and this is where one would be caught.
        val table = stringTable()
        assertTrue("the string table moved", table.size >= 20)
        val words = listOf(
            "done", "finish", "seal", "stamp",
            "congrat", "well done", "wrong", "score", "try again",
        )
        for ((name, text) in table) {
            for (word in words) {
                val says = "$name $text".lowercase()
                assertFalse(
                    "the string $name carries \"$word\": $text",
                    says.contains(word),
                )
            }
        }
    }

    /** The one string table, by name, read straight from the source. */
    private fun stringTable(): Map<String, String> {
        val file = File("src/main/res/values/strings.xml")
        assertTrue("no string table at ${file.absolutePath}", file.isFile)
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = document.getElementsByTagName("string")
        val out = LinkedHashMap<String, String>()
        for (i in 0 until nodes.length) {
            val element = nodes.item(i) as? Element ?: continue
            out[element.getAttribute("name")] = element.textContent.orEmpty()
        }
        return out
    }
}
