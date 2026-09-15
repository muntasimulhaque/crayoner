package io.github.muntasimulhaque.crayoner.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot

/**
 * The app's control marks: the house, the speaker, the rubber and the two
 * steps. They are drawn by every renderer there is, on the device and in the
 * offline sheets, so what they are is not a taste: it is the thing that
 * makes a row of buttons read as one row rather than as four apps, and every
 * part of it is held here.
 *
 * The rules the file has to keep are these. A mark fits inside its own box,
 * because a mark that runs off its box is a mark that collides with its
 * neighbor or gets clipped by a coin's own round edge. A mark is drawn with
 * points, never with a curve call, so the two renderers cannot disagree about
 * which curve was meant. The two steps are the same mark in two directions,
 * which is a promise to the child as much as to the renderer. And every mark
 * is a line at one weight: a mark with no points is nothing to draw.
 */
class AppIconTest {

    private val all = AppIcon.Name.entries.toList()

    @Test
    fun everyMarkStaysInsideItsOwnBox() {
        for (name in all) {
            val pieces = AppIcon.pieces(name)
            assertTrue("$name has nothing in it", pieces.isNotEmpty())
            for ((index, piece) in pieces.withIndex()) {
                assertTrue("$name piece $index has a single point", piece.points.size >= 2)
                for (p in piece.points) {
                    assertTrue(
                        "$name piece $index leaves its box at $p",
                        p.x >= -1e-9 && p.x <= 1.0 + 1e-9 && p.y >= -1e-9 && p.y <= 1.0 + 1e-9,
                    )
                }
            }
        }
    }

    @Test
    fun everyMarkFillsItsBox() {
        // A mark that is drawn in a tenth of its box is a mark that comes out
        // invisible beside its neighbors: every mark uses the room it is
        // given, in both directions.
        for (name in all) {
            val points = AppIcon.pieces(name).flatMap { it.points }
            val w = points.maxOf { it.x } - points.minOf { it.x }
            val h = points.maxOf { it.y } - points.minOf { it.y }
            assertTrue("$name is only $w wide in its box", w >= 0.55)
            assertTrue("$name is only $h tall in its box", h >= 0.55)
        }
    }

    @Test
    fun theSpeakerIsOneObjectInTwoStates() {
        // The on and off speaker share the cone exactly, point for point, so
        // switching the sound does not shift the mark under the thumb. What
        // changes is only what leaves the cone: waves when it is on, a quiet
        // crossed-out cone when it is off.
        val on = AppIcon.pieces(AppIcon.Name.SOUND_ON)
        val off = AppIcon.pieces(AppIcon.Name.SOUND_OFF)
        assertEquals(3, on.size)
        assertEquals(3, off.size)
        assertEquals(on[0].points, off[0].points)
        // The waves are curves, so they are sampled into many short steps;
        // the cross is two straight lines. That is the whole of the
        // difference between the two states, and it has to show in the
        // geometry or the two marks are the same mark.
        assertTrue("the speaker's waves are not curves", on[1].points.size > 12)
        assertEquals("the muted speaker's cross is not a line", 2, off[1].points.size)
        assertEquals("the muted speaker's cross is not a line", 2, off[2].points.size)
    }

    @Test
    fun theTwoStepsAreOneMarkInTwoDirections() {
        // The step forward is the step back mirrored about its own middle,
        // and the drawing is point by point rather than a transform, so the
        // two arrows are exactly the same weight at every size. A pair that
        // drifted would be two shapes to learn instead of one with two
        // directions.
        val back = AppIcon.pieces(AppIcon.Name.UNDO)
        val forward = AppIcon.pieces(AppIcon.Name.REDO)
        assertEquals(back.size, forward.size)
        for (i in back.indices) {
            val a = back[i]
            val b = forward[i]
            assertEquals("piece $i has a different point count", a.points.size, b.points.size)
            assertEquals("piece $i is filled differently", a.fill, b.fill)
            assertEquals("piece $i is closed differently", a.closed, b.closed)
            for (j in a.points.indices) {
                assertEquals("piece $i point $j is not mirrored in x", 1.0 - a.points[j].x, b.points[j].x, 1e-9)
                assertEquals("piece $i point $j is not level in y", a.points[j].y, b.points[j].y, 1e-9)
            }
        }
    }

    @Test
    fun theStepsPointTheWayTheyTravel() {
        // A step arrow whose head does not point along its own stroke says
        // nothing: the child sees a circle with a lump on it. So the head's
        // own base sits on the ring, and its tip is further out still, away
        // from the middle on the side the step means.
        for ((name, forward) in listOf(AppIcon.Name.UNDO to false, AppIcon.Name.REDO to true)) {
            val head = AppIcon.pieces(name)[1]
            val tip = head.points[0]
            val baseA = head.points[1]
            val baseB = head.points[2]
            // The base's own middle sits astride the ring, so the wedge rides
            // the end of the stroke like a nib on a pen; its two corners
            // straddle the stroke by half the head's own width.
            val midX = (baseA.x + baseB.x) / 2.0
            val midY = (baseA.y + baseB.y) / 2.0
            assertEquals("$name's head is off its own ring", 0.30, hypot(midX - 0.5, midY - 0.5), 0.02)
            val halfWidth = hypot(baseA.x - midX, baseA.y - midY)
            assertTrue("$name's head is too narrow to sit on its stroke", halfWidth >= 0.09)
            assertTrue("$name's head is wider than its own ring", halfWidth <= 0.14)
            // And the tip leads further out than either base corner, on the
            // side the step means: back points left, forward points right.
            val tipDistance = hypot(tip.x - 0.5, tip.y - 0.5)
            val baseDistance = hypot(midX - 0.5, midY - 0.5)
            assertTrue("$name's head points off its own stroke", tipDistance > baseDistance)
            if (forward) {
                assertTrue("$name's head points the wrong way", tip.x > baseA.x && tip.x > baseB.x)
            } else {
                assertTrue("$name's head points the wrong way", tip.x < baseA.x && tip.x < baseB.x)
            }
        }
    }

    @Test
    fun theRubberHasItsSleeveRulesInsideIt() {
        // The two rules across the block are what say rubber rather than
        // eraser block, and a rule drawn off the end of the object is a
        // floating hair: the bug this project has already shipped once on
        // the crayon's wrapper.
        val pieces = AppIcon.pieces(AppIcon.Name.ERASER)
        val block = pieces[0].points
        val rules = pieces.drop(1)
        assertEquals(2, rules.size)
        val minX = block.minOf { it.x }
        val maxX = block.maxOf { it.x }
        val minY = block.minOf { it.y }
        val maxY = block.maxOf { it.y }
        for (rule in rules) {
            for (p in rule.points) {
                assertTrue("a rule leaves the block at $p", p.x >= minX && p.x <= maxX)
                assertTrue("a rule leaves the block at $p", p.y >= minY && p.y <= maxY)
            }
        }
        // And the two rules do not touch: a pair drawn on top of one another
        // is one rule with a wider line, which says nothing.
        fun middle(rule: AppIcon.Piece): Vec2 = Vec2(
            rule.points.sumOf { it.x } / rule.points.size,
            rule.points.sumOf { it.y } / rule.points.size,
        )
        val a = middle(rules[0])
        val b = middle(rules[1])
        assertTrue(
            "the rules are on top of each other",
            kotlin.math.hypot(a.x - b.x, a.y - b.y) > 0.12,
        )
    }

    @Test
    fun theHouseHasAFloorToStandOn() {
        // A house drawn as a roof over open air is a tent. The walls reach
        // below the roof's own eaves and the door sits between them.
        val walls = AppIcon.pieces(AppIcon.Name.HOME)[1].points
        assertEquals(0.26, walls.minOf { it.x }, 1e-9)
        assertEquals(0.74, walls.maxOf { it.x }, 1e-9)
        assertTrue("the walls do not reach the ground", walls.maxOf { it.y } >= 0.8)
        val roof = AppIcon.pieces(AppIcon.Name.HOME)[0].points
        assertTrue("the walls do not reach the roof", walls.minOf { it.y } <= roof.maxOf { it.y } + 1e-9)
    }

    @Test
    fun noMarkIsOnlyAClosedBlob() {
        // Every mark is a line the app drew, and a mark made of one filled
        // shape with no stroke anywhere is an icon-font glyph: this app does
        // not use those. Each mark has at least one stroked piece, and the
        // filled ones are heads and doors that sit on one.
        for (name in all) {
            val pieces = AppIcon.pieces(name)
            val stroked = pieces.count { !it.fill }
            assertTrue("$name has nothing stroked in it", stroked >= 1)
            // A fill with no stroke and no other piece would be a solid blob.
            if (pieces.size == 1) assertFalse("$name is a solid blob", pieces[0].fill)
        }
    }
}
