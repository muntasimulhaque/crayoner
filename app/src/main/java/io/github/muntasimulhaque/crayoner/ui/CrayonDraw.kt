package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import io.github.muntasimulhaque.crayoner.core.Area
import io.github.muntasimulhaque.crayoner.core.CrayonShape
import io.github.muntasimulhaque.crayoner.core.Vec2

/**
 * The app's one crayon, drawn.
 *
 * Everything that draws a crayon on the device comes through
 * [drawCrayonShape]: the tray's crayon in the child's hand, the dozens lying
 * in the box of colors, the mark beside the app's name, and the sample sheets
 * in `:tools` when they need the same shape. The geometry itself is core's
 * ([CrayonShape]), and this file is only the hand that paints it: where each
 * piece lands, which of the four waxes paints it, and how a turn carries all
 * of it.
 *
 * The file is separate from the composables that host it (see
 * `CrayonGlyph.kt`) because the two are different jobs: one is a drawing, the
 * other is where on a screen the drawing goes. A drawing that only ever
 * happens through a Compose canvas is a drawing no other renderer can share,
 * and the sheets in `:tools` need the same one.
 */

/**
 * Draws one crayon filling the box it is given.
 *
 * Standing, the crayon's length runs down the box with the tip at the bottom,
 * the way a crayon is held. Lying, its length runs across the box with the
 * tip at the right, the way it rests in a tray. A crayon thicker or thinner
 * than its own proportions is not a crayon, so the box is fitted by the one
 * scale that puts the whole stick inside it and the mark is centered in
 * whatever room is left over: a caller that passes the mark's own proportions
 * gets a mark that fills its box, and a caller that does not still gets a
 * crayon rather than a stretched one.
 *
 * The turn is applied to the shape's own points, before they are placed, so
 * turning the stick can never stretch it or slide the wrapper off it. Lying
 * is not a special case of drawing but the same turn taken a different way,
 * which is why one mapping serves every way a crayon is ever held.
 */
internal fun DrawScope.drawCrayonShape(
    wax: Color,
    inks: CrayonPaints,
    contact: Boolean,
    left: Float,
    top: Float,
    lying: Boolean,
    leanDeg: Double = 0.0,
    lineBoost: Float = 1f,
) {
    // A held crayon is the shape turned point down; a lean is that stance
    // turned a little further, so the app's own mark is one number in :core
    // and the launcher icon wears the same one. Lying is the shape laid down.
    val turn = when {
        lying -> CrayonShape.LYING_TURN
        else -> CrayonShape.HELD_TURN + leanDeg
    }
    val bounds = CrayonShape.turnedBounds(turn)
    val boxW = size.width
    val boxH = size.height
    // One scale for both axes, and the mark is centered in what is left over:
    // the caller's box decides how much paper the crayon gets, and the
    // crayon's own proportion decides how much of it is crayon.
    val unit = minOf(boxW / bounds.w.toFloat(), boxH / bounds.h.toFloat())
    val originX = left + (boxW - bounds.w.toFloat() * unit) / 2f - bounds.x.toFloat() * unit
    val originY = top + (boxH - bounds.h.toFloat() * unit) / 2f - bounds.y.toFloat() * unit
    fun px(p: Vec2): Offset {
        val turned = CrayonShape.turned(p, turn)
        return Offset(originX + turned.x.toFloat() * unit, originY + turned.y.toFloat() * unit)
    }
    val silhouette = outlinePath(::px)
    drawPath(silhouette, wax)
    drawPath(conePath(::px), inks.cone)
    val band = CrayonShape.wrapperBand()
    drawPath(rectPath(::px, band.x, band.y, band.right, band.bottom), inks.wrapper)
    drawWrapperRules(::px, band, inks.rule, unit * lineBoost)
    val base = CrayonShape.baseBand()
    drawPath(rectPath(::px, base.x, base.y, base.right, base.bottom), inks.base.copy(alpha = 0.72f))
    if (contact) {
        // The picked crayon wears a paper collar, the way a held crayon is
        // banded by a hand, so the choice reads at a glance even when two
        // colors are hard to tell apart.
        val collar = CrayonShape.collarBand()
        drawPath(rectPath(::px, collar.x, collar.y, collar.right, collar.bottom), CrayonerColors.Card)
    }
}

/** The crayon's own silhouette, mapped onto the canvas by [px]. */
private fun outlinePath(px: (Vec2) -> Offset): Path = Path().apply {
    val points = CrayonShape.outline()
    val first = px(points[0])
    moveTo(first.x, first.y)
    for (i in 1 until points.size) {
        val o = px(points[i])
        lineTo(o.x, o.y)
    }
    close()
}

/** The cone alone, so it can be a shade deeper than the body. */
private fun conePath(px: (Vec2) -> Offset): Path = Path().apply {
    val points = CrayonShape.outline()
    moveTo(px(points[0]).x, px(points[0]).y)
    for (point in points) {
        if (point.y > CrayonShape.TIP_LENGTH + 1e-9) break
        val o = px(point)
        lineTo(o.x, o.y)
    }
    // Back across the shoulders, so the cone's own band closes on itself.
    val left = px(Vec2(0.0, CrayonShape.TIP_LENGTH))
    val right = px(Vec2(1.0, CrayonShape.TIP_LENGTH))
    lineTo(right.x, right.y)
    lineTo(left.x, left.y)
    close()
}

/** One band of the crayon, mapped onto the canvas by [px]. */
private fun rectPath(px: (Vec2) -> Offset, x: Double, y: Double, right: Double, bottom: Double): Path {
    val a = px(Vec2(x, y))
    val b = px(Vec2(right, bottom))
    return Path().apply {
        addRect(
            Rect(
                minOf(a.x, b.x),
                minOf(a.y, b.y),
                maxOf(a.x, b.x),
                maxOf(a.y, b.y),
            ),
        )
    }
}

/**
 * The two rules every real wrapper wears, one above the label and one below
 * it. They follow the crayon's own turn, so a lying crayon's rules run across
 * it and a standing one's run down it.
 *
 * Two unit systems meet in here, and mixing them up is a bug that already
 * shipped once: the crayon's shape is measured in its own thicknesses, and
 * the canvas is measured in pixels. The shape's two numbers have to be
 * turned into pixels exactly once, through [px]. The rules were once inset
 * a *pixel* distance into a band measured in shape units, which put both
 * lines roughly a hundred thicknesses along the crayon and off the end of
 * it: two floating hairs beside the stick. The inset is a share of the
 * band's own ends, so it is correct at every size by construction.
 */
private fun DrawScope.drawWrapperRules(
    px: (Vec2) -> Offset,
    band: Area,
    rule: Color,
    unit: Float,
) {
    val line = Stroke((unit * 0.055f).coerceAtLeast(0.7f), cap = StrokeCap.Round)
    // Derived from the band itself, not from a pixel count: a real wrapper's
    // rules sit inside its own ends by a fixed share of the band.
    val near = band.h * 0.12
    for (y in listOf(band.y + near, band.bottom - near)) {
        val a = px(Vec2(band.x, y))
        val b = px(Vec2(band.right, y))
        drawPath(
            Path().apply {
                moveTo(a.x, a.y)
                lineTo(b.x, b.y)
            },
            rule,
            style = line,
        )
    }
}
