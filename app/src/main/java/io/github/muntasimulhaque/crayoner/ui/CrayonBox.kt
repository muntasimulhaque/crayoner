package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.crayoner.R
import io.github.muntasimulhaque.crayoner.core.Area
import io.github.muntasimulhaque.crayoner.core.CrayonShape
import io.github.muntasimulhaque.crayoner.core.Crayons
import io.github.muntasimulhaque.crayoner.core.Vec2

/** The narrowest a crayon may be drawn and still be a comfortable target. */
private val MIN_CRAYON_CELL = 52.dp

/** The widest a crayon may be drawn before it stops looking like a crayon. */
private val MAX_CRAYON_CELL = 88.dp

/**
 * The crayon box.
 *
 * One box, the same sixteen crayons inside it, on every page in the book: a
 * real box of crayons does not change its contents because of the page you
 * open. A child reaches for red because red is always in the same place.
 *
 * The box is drawn as the object it is: a warm cardboard tray, the crayons
 * lying in it in rows, tips all the same way, and a front wall across the
 * bottom that the crayons rest behind. The picked crayon rises out of the
 * tray on a spring and wears a paper collar, which is how a held crayon
 * looks and how a picked crayon should look too.
 *
 * It never scrolls and it has no second page: all sixteen are always here.
 */
@Composable
fun CrayonBox(
    selected: Long?,
    onPick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .buttonShadow(RoundedCornerShape(24.dp), elevation = 6.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(CrayonerColors.Cardboard)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        val crayons = Crayons.all
        val gaps = 6.dp
        // The box sizes itself to the room it is given, and there are only
        // three shapes it ever takes, so a crayon is always a comfortable
        // thing to grab no matter the screen:
        //
        //   a wide box (a tablet, or the tools column on its own) gets four
        //   columns of big crayons in four rows;
        //   a phone-width box gets six columns in three rows, which is the
        //   same crayon size in a shorter box;
        //   a narrow box (a phone held sideways) goes back to four columns,
        //   because six crayons across two hundred dp would be slivers.
        //
        // The crayon's own width is capped, so on a very wide tablet the
        // crayons stay crayon-sized instead of stretching into ribbons.
        val columns = when {
            maxWidth >= 560.dp -> 4
            maxWidth >= 330.dp -> 6
            else -> 4
        }
        val cellWidth = ((maxWidth - gaps * (columns - 1)) / columns)
            .coerceAtLeast(MIN_CRAYON_CELL)
            .coerceAtMost(MAX_CRAYON_CELL)
        // A lying crayon is a little over three times as long as it is
        // thick, so the cell is that thickness plus air: a deeper cell would
        // eat the screen the picture needs and leave the crayon floating.
        val cellHeight = cellWidth * CrayonShape.THICKNESS.toFloat() * 1.6f
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(gaps),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            for (row in crayons.chunked(columns)) {
                Row(horizontalArrangement = Arrangement.spacedBy(gaps)) {
                    for (argb in row) {
                        CrayonSeat(
                            argb = argb,
                            selected = argb == selected,
                            onClick = { onPick(argb) },
                            cellWidth = cellWidth,
                            cellHeight = cellHeight,
                        )
                    }
                }
            }
        }
    }
}

/**
 * One crayon's place in the box: the wax, and the whole cell as a touch
 * target either way. The name of the color is what a screen reader says, and
 * whether it is the one being held.
 *
 * The picked crayon says so in three ways that are all real things a held
 * crayon does: it lifts on a spring, it is drawn a little larger, and it
 * wears a soft ink shadow on the cardboard under it, the way an object
 * raised off a surface does. There is no plate, no pip and no highlight
 * behind it: a crayon lying in a box casts a shadow, it does not sit on a
 * white tile.
 */
@Composable
fun CrayonSeat(
    argb: Long,
    selected: Boolean,
    onClick: () -> Unit,
    cellWidth: Dp,
    cellHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(crayonNameRes(argb))
    // The picked crayon rises: a spring on where it sits and on how large it
    // is drawn. A crayon that only changed color would be easy to lose among
    // sixteen; one that lifts out of the box is not.
    val lift = animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "crayon-lift",
    ).value
    // The crayon is exactly as long as the box it lies in and as thick as
    // its own proportions say, so it is the same crayon at every size.
    val length = cellWidth * (0.94f + 0.06f * lift)
    val thickness = length * CrayonShape.THICKNESS.toFloat()
    Box(
        modifier = modifier
            .size(cellWidth, cellHeight)
            .semantics {
                contentDescription = label
                this.selected = selected
            }
            .clickable(role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // The shadow under a raised crayon. It grows with the lift, so the
        // crayon visibly comes off the cardboard and settles back down.
        if (lift > 0.02f) {
            Canvas(modifier = Modifier.size(length, thickness)) {
                drawOval(
                    color = CrayonerColors.Shadow.copy(alpha = 0.34f * lift),
                    topLeft = Offset(0f, size.height * 0.30f),
                    size = Size(size.width * 0.86f, size.height * 0.68f),
                )
            }
        }
        CrayonGlyph(
            color = Color(argb),
            contact = selected,
            lying = true,
            // A crayon is about three times as long as it is thick, which is
            // the stub a child holds. Anything fatter reads as a bullet, and
            // a marker is the one thing this app must never look like.
            modifier = Modifier.size(width = length, height = thickness),
        )
    }
}

/**
 * The name of each crayon. Every crayon in the box has its own word, held to
 * that by a test, because a crayon the screen reader cannot name is a crayon
 * a blind child cannot find.
 */
internal fun crayonNameRes(argb: Long): Int = when (argb) {
    Crayons.RED -> R.string.crayon_red
    Crayons.ORANGE -> R.string.crayon_orange
    Crayons.YELLOW -> R.string.crayon_yellow
    Crayons.GREEN -> R.string.crayon_green
    Crayons.FOREST -> R.string.crayon_forest
    Crayons.TEAL -> R.string.crayon_teal
    Crayons.SKY -> R.string.crayon_sky
    Crayons.BLUE -> R.string.crayon_blue
    Crayons.NAVY -> R.string.crayon_navy
    Crayons.PURPLE -> R.string.crayon_purple
    Crayons.LILAC -> R.string.crayon_lilac
    Crayons.PINK -> R.string.crayon_pink
    Crayons.BROWN -> R.string.crayon_brown
    Crayons.SAND -> R.string.crayon_sand
    Crayons.GRAY -> R.string.crayon_gray
    Crayons.WHITE -> R.string.crayon_white
    else -> R.string.app_name
}

/** A crayon drawn flat, with its own ink line, in the book's hand. */
@Composable
fun CrayonGlyph(
    color: Color,
    modifier: Modifier = Modifier,
    contact: Boolean = false,
    lying: Boolean = false,
) {
    val ink = CrayonerColors.Ink
    // The wrapper is the crayon's own wax, barely lightened: a pale sleeve
    // on a colored body is a pencil, and this app draws crayons. The rules
    // that band the wrapper are the same wax taken deeper, the way print on
    // paper reads.
    val shade = remember(color) { mix(color, ink, 0.32f) }
    val wrapper = remember(color) { mix(color, CrayonerColors.Card, 0.12f) }
    Canvas(modifier = modifier) {
        // Standing, the crayon's length runs down the canvas; lying, it runs
        // across it, tip to the right. The geometry is built in the frame it
        // is drawn in either way, so nothing is ever rotated out of the
        // canvas and clipped.
        val length = if (lying) size.width else size.height
        drawCrayonShape(
            color = color,
            shade = shade,
            wrapper = wrapper,
            ink = ink,
            contact = contact,
            left = 0f,
            top = 0f,
            length = length,
            lying = lying,
        )
    }
}

/**
 * Draws one crayon, [length] pixels long, in the box starting at ([left],
 * [top]).
 *
 * Standing, its length runs down the canvas and its tip is at the top.
 * Lying, its length runs across the canvas and its tip is at the right, the
 * way a crayon rests in a tray. The width always follows from the length,
 * because a crayon's thickness is a property of the crayon and not of the
 * box it happens to be drawn in: that is what keeps the tray's crayon and
 * the launcher icon's crayon the same object at every size.
 *
 * The crayon itself is core's one crayon: the body, the blunt cone, the
 * squared base, the wrapper in the wax's own color with its two dark rules,
 * and the paper collar a held crayon wears. This function only decides the
 * colors and the place it lands; nothing about the object's shape is decided
 * here.
 */
internal fun DrawScope.drawCrayonShape(
    color: Color,
    shade: Color,
    wrapper: Color,
    ink: Color,
    contact: Boolean,
    left: Float,
    top: Float,
    length: Float,
    lying: Boolean,
) {
    // The shape is measured in the crayon's own thicknesses, so one unit is
    // the thickness on both axes and the crayon cannot be stretched.
    val unit = length / CrayonShape.LENGTH.toFloat()
    val thickness = unit
    val line = Stroke(
        (thickness * CrayonShape.LINE.toFloat()).coerceAtLeast(1f),
        cap = StrokeCap.Round,
        join = StrokeJoin.Round,
    )
    // One mapping for the whole drawing: a point in the shape's own unit box
    // to a point on the canvas, in whichever way the crayon is lying.
    fun px(p: Vec2): Offset = if (lying) {
        Offset(
            left + length - p.y.toFloat() * unit,
            top + p.x.toFloat() * unit,
        )
    } else {
        Offset(
            left + p.x.toFloat() * unit,
            top + p.y.toFloat() * unit,
        )
    }
    fun bandPath(band: Area): Path {
        val a = px(Vec2(band.x, band.y))
        val b = px(Vec2(band.right, band.bottom))
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
    val silhouette = Path().apply {
        val points = CrayonShape.outline()
        val first = px(points[0])
        moveTo(first.x, first.y)
        for (i in 1 until points.size) {
            val o = px(points[i])
            lineTo(o.x, o.y)
        }
        close()
    }
    drawPath(silhouette, color)

    // The wrapper: the crayon's own color, a whisper lighter, the way paper
    // takes wax. Never a pale sleeve, which would make it a pencil.
    val band = CrayonShape.wrapperBand()
    drawPath(bandPath(band), wrapper)

    // The two dark rules a real wrapper wears, at its top and its bottom.
    val rule = Stroke(line.width * 0.40f, cap = StrokeCap.Round)
    for (y in listOf(band.y + 0.012, band.bottom - 0.012)) {
        val a = px(Vec2(band.x, y))
        val b = px(Vec2(band.right, y))
        drawPath(
            Path().apply {
                moveTo(a.x, a.y)
                lineTo(b.x, b.y)
            },
            shade,
            style = rule,
        )
    }

    if (contact) {
        // The picked crayon wears a paper collar, the way a held crayon is
        // banded by a hand, so the choice reads at a glance even when two
        // colors are hard to tell apart.
        val collar = bandPath(CrayonShape.collarBand())
        drawPath(collar, CrayonerColors.Card)
        drawPath(collar, ink, style = Stroke(line.width * 0.5f, cap = StrokeCap.Round))
    }

    drawPath(silhouette, ink, style = line)
}

/** Mixes two colors in sRGB; [amount] is how much of [other] lands. */
internal fun mix(base: Color, other: Color, amount: Float): Color {
    val t = amount.coerceIn(0f, 1f)
    return Color(
        red = base.red + (other.red - base.red) * t,
        green = base.green + (other.green - base.green) * t,
        blue = base.blue + (other.blue - base.blue) * t,
        alpha = base.alpha + (other.alpha - base.alpha) * t,
    )
}
