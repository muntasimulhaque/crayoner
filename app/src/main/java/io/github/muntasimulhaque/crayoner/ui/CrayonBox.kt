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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.crayoner.R
import io.github.muntasimulhaque.crayoner.core.Crayons

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
        // A lying crayon is about three and a half times as long as it is
        // thick, so the cell is that thickness plus air: a deeper cell would
        // eat the screen the picture needs and leave the crayon floating.
        val cellHeight = cellWidth * 0.62f
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
 * One crayon's place in the box: the wax, a cradle when it is the crayon in
 * hand, and a whole cell of touch target either way. The name of the color
 * is what a screen reader says, and whether it is the one being held.
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
        val cradleWidth = cellWidth * (0.78f + 0.14f * lift)
        val cradleHeight = cellHeight * (0.92f + 0.16f * lift)
        if (lift > 0.01f) {
            Box(
                modifier = Modifier
                    .size(cradleWidth, cradleHeight)
                    .shadow(6.dp, RoundedCornerShape(cellHeight * 0.30f))
                    .clip(RoundedCornerShape(cellHeight * 0.30f))
                    .background(CrayonerColors.Card),
            )
        }
        CrayonGlyph(
            color = Color(argb),
            contact = selected,
            lying = true,
            // A crayon is long and slim: about three and a half times its
            // own thickness. Anything fatter reads as a bullet, and a
            // marker is the one thing this app must never look like.
            modifier = Modifier.size(
                width = cellWidth * (0.94f + 0.04f * lift),
                height = cellWidth * 0.30f * (1f + 0.14f * lift),
            ),
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
        if (!lying) {
            drawCrayonShape(color, shade, wrapper, ink, contact, 0f, 0f, size.width, size.height)
            return@Canvas
        }
        // Lying in the box, tip to the right. The crayon is drawn as if the
        // canvas were tall, then the whole thing is turned a quarter turn
        // around the cell's center, so one drawing serves both orientations.
        val c = center
        withTransform({ rotate(-90f, pivot = c) }) {
            drawCrayonShape(
                color, shade, wrapper, ink, contact,
                left = c.x - size.height / 2f,
                top = c.y - size.width / 2f,
                w = size.height,
                h = size.width,
            )
        }
    }
}

/**
 * Draws one crayon, point up, inside the box from ([left], [top]) to
 * ([w], [h]).
 *
 * The proportions are the real object's, and they are the whole reason this
 * reads as a crayon: a body about three and a half times as long as it is
 * thick, a cone that is as wide as the body and about four fifths as long,
 * and a small, bluntly rounded nose rather than a point. A longer or
 * sharper nose is a pencil; a shorter one is a bullet; a fatter body is a
 * marker. The wrapper is the crayon's own wax, barely lightened, with the
 * two dark rules a real wrapper wears.
 */
internal fun DrawScope.drawCrayonShape(
    color: Color,
    shade: Color,
    wrapper: Color,
    ink: Color,
    contact: Boolean,
    left: Float,
    top: Float,
    w: Float,
    h: Float,
) {
    // The crayon's own thickness, taken from the width it was given, and its
    // length, taken from the height. A lying crayon is drawn tall and then
    // turned by its caller, so this always draws point up.
    val bodyLeft = left + w * 0.06f
    val bodyRight = left + w * 0.94f
    val diameter = bodyRight - bodyLeft
    val cx = left + w * 0.5f
    // A real crayon's cone is a little longer than the crayon is thick.
    val tipLength = diameter * 1.15f
    val shoulder = top + tipLength
    // The nose is blunt: pressed wax, nothing like a sharpened point.
    val nose = diameter * 0.13f
    val bandTop = top + h * 0.30f
    val bandBottom = top + h * 0.88f
    val base = top + h * 0.97f
    val line = Stroke(diameter * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round)

    val silhouette = Path().apply {
        moveTo(bodyLeft, shoulder)
        // The cone: straight flanks from the body's own edges, up to a small
        // bluntly rounded nose. Straight flanks are what make it a cone; a
        // bowed side makes it a bullet.
        lineTo(cx - nose, top + nose * 0.6f)
        cubicTo(
            cx - nose, top + nose * 0.16f,
            cx + nose, top + nose * 0.16f,
            cx + nose, top + nose * 0.6f,
        )
        lineTo(bodyRight, shoulder)
        lineTo(bodyRight, base)
        // A squared base with only a hint of softness at its corners: this
        // is one of the three things that says crayon and not marker.
        cubicTo(
            bodyRight, top + h * 0.995f,
            bodyLeft, top + h * 0.995f,
            bodyLeft, base,
        )
        close()
    }
    drawPath(silhouette, color)

    // The wrapper: the crayon's own color, a whisper lighter, the way paper
    // takes wax. Never a pale sleeve, which would make it a pencil.
    val wrap = Path().apply {
        moveTo(bodyLeft, bandTop)
        lineTo(bodyRight, bandTop)
        lineTo(bodyRight, bandBottom)
        lineTo(bodyLeft, bandBottom)
        close()
    }
    drawPath(wrap, wrapper)

    // The two dark rules a real wrapper wears, at its top and its bottom.
    val rule = Stroke(line.width * 0.40f, cap = StrokeCap.Round)
    for (y in listOf(bandTop + h * 0.012f, bandBottom - h * 0.012f)) {
        drawPath(
            Path().apply {
                moveTo(bodyLeft, y)
                lineTo(bodyRight, y)
            },
            shade,
            style = rule,
        )
    }

    if (contact) {
        // The picked crayon wears a paper collar, the way a held crayon is
        // banded by a hand, so the choice reads at a glance even when two
        // colors are hard to tell apart.
        val collar = Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    rect = Rect(
                        Offset(bodyLeft - diameter * 0.16f, top + h * 0.48f),
                        Size(diameter * 1.32f, h * 0.16f),
                    ),
                    cornerRadius = CornerRadius(w * 0.04f),
                ),
            )
        }
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
