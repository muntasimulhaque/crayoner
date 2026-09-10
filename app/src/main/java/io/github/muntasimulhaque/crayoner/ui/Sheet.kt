package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.crayoner.core.Page
import io.github.muntasimulhaque.crayoner.core.Vec2
import io.github.muntasimulhaque.crayoner.host.Screen

/**
 * The sheet: one square of paper, sized to the room it is given.
 *
 * It is paper, not a card: square corners with only the smallest softening,
 * a soft shadow so it stands off the desk, and the same wax grain the colors
 * carry, laid over the whole sheet at a whisper, so even the bare paper has
 * the tooth of real paper under a finger.
 *
 * The sheet owns every way a color answers: the sweep that wipes it in from
 * under the finger, the sparkle when it lands where the picture asked, and
 * the soft ring when it does not. None of them block the next tap.
 */
@Composable
internal fun SheetOf(
    state: Screen.Coloring,
    onTap: (Vec2) -> Unit,
    onColorArea: (Int) -> Unit,
    side: Dp,
    modifier: Modifier = Modifier,
) {
    val sidePx = with(LocalDensity.current) { side.roundToPx() }
    val fills = state.progress.asMap()
    val rightFade = fadeAfter(state.rightAt)
    val wrongFade = fadeAfter(state.wrongAt)
    val sweepProgress = sweepAfter(state.lastPaint?.stamp ?: 0L)
    val settle = rememberSettle(state.page.id)
    Box(
        modifier = modifier
            .size(side)
            .graphicsLayer {
                scaleX = 0.96f + 0.04f * settle
                scaleY = 0.96f + 0.04f * settle
            }
            .buttonShadow(RoundedCornerShape(PageCorner), elevation = 8.dp)
            .clip(RoundedCornerShape(PageCorner))
            .background(CrayonerColors.Card)
            .pointerInput(state.page.id, sidePx) {
                detectTapGestures { offset ->
                    if (sidePx <= 0) return@detectTapGestures
                    val s = sidePx.toFloat()
                    onTap(Vec2((offset.x / s).toDouble(), (offset.y / s).toDouble()))
                }
            },
    ) {
        PageCanvas(
            page = state.page,
            fills = fills,
            sidePx = sidePx,
            modifier = Modifier.fillMaxSize(),
            sweep = state.lastPaint?.let { paint ->
                PaintSweep(
                    index = paint.index,
                    x = paint.at.x,
                    y = paint.at.y,
                    progress = sweepProgress,
                    before = paint.before,
                )
            },
            overlay = { geometry ->
                drawPaintedFeedback(
                    page = state.page,
                    geometry = geometry,
                    rightIndex = state.rightIndex,
                    rightFade = rightFade,
                    wrongIndex = state.wrongIndex,
                    wrongFade = wrongFade,
                    strokePx = size.width * STROKE_FRACTION,
                )
            },
        )
        // The screen reader's view of the page: one focusable target per
        // area, naming what it is and what color it wants. Semantics only,
        // so a finger passes straight through to the sheet.
        PageSemantics(
            page = state.page,
            fills = fills,
            crayon = state.crayon,
            onColor = { index -> onColorArea(index) },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** The sheet, sized to the space its parent gives it. */
@Composable
internal fun Sheet(
    state: Screen.Coloring,
    onTap: (Vec2) -> Unit,
    onColorArea: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val side: Dp = minOf(maxWidth, maxHeight)
        SheetOf(state = state, onTap = onTap, onColorArea = onColorArea, side = side)
    }
}

/**
 * The two answers a color can give, drawn over the page: a warm sparkle
 * where the color was right, and a soft ink ring where the picture asks
 * for something else. Both fade on their own and neither blocks the next
 * tap, so a child who colors at speed is never held up by praise.
 */
internal fun DrawScope.drawPaintedFeedback(
    page: Page,
    geometry: PageGeometry,
    rightIndex: Int,
    rightFade: Float,
    wrongIndex: Int,
    wrongFade: Float,
    strokePx: Float,
) {
    if (rightIndex >= 0 && rightFade > 0f) {
        val argb = page.region(rightIndex)?.fillArgb
        if (argb != null) {
            pulseRegion(
                geometry = geometry,
                index = rightIndex,
                color = Color(argb),
                alpha = 0.60f * rightFade,
                width = strokePx * (2.4f + 2.2f * (1f - rightFade)),
            )
            // The sparkle sits at the area's own center, taken from the
            // shape data rather than from a path's bounds: a path's bounds
            // are the bounds of its control points, which for a curve or a
            // ring sits far from where the area actually looks centered.
            val center = page.region(rightIndex)?.centroid
            if (center != null) {
                drawSparkle(
                    androidx.compose.ui.geometry.Offset(
                        (center.x * size.width).toFloat(),
                        (center.y * size.width).toFloat(),
                    ),
                    rightFade,
                )
            }
        }
    }
    if (wrongIndex >= 0 && wrongFade > 0f) {
        pulseRegion(
            geometry = geometry,
            index = wrongIndex,
            color = CrayonerColors.Ink,
            alpha = 0.26f * wrongFade,
            width = strokePx * (1.9f + 1.6f * (1f - wrongFade)),
        )
    }
}

/** A four pointed sparkle, the quiet little cheer for a color well placed. */
internal fun DrawScope.drawSparkle(center: androidx.compose.ui.geometry.Offset, fade: Float) {
    if (center == androidx.compose.ui.geometry.Offset.Unspecified) return
    val r = size.minDimension * 0.055f * (0.45f + 0.55f * fade)
    val alpha = (fade * 0.95f).coerceIn(0f, 1f)
    val color = CrayonerColors.Honey.copy(alpha = alpha)
    val spark = Path().apply {
        moveTo(center.x, center.y - r)
        lineTo(center.x + r * 0.26f, center.y - r * 0.26f)
        lineTo(center.x + r, center.y)
        lineTo(center.x + r * 0.26f, center.y + r * 0.26f)
        lineTo(center.x, center.y + r)
        lineTo(center.x - r * 0.26f, center.y + r * 0.26f)
        lineTo(center.x - r, center.y)
        lineTo(center.x - r * 0.26f, center.y - r * 0.26f)
        close()
    }
    drawPath(spark, color)
}

/** How much of the sheet is covered, as one quiet line, for the parents. */
@Composable
internal fun ProgressLine(
    colored: Int,
    total: Int,
    accent: Long,
    modifier: Modifier = Modifier,
) {
    val label = androidx.compose.ui.res.stringResource(
        io.github.muntasimulhaque.crayoner.R.string.page_progress,
        colored,
        total,
    )
    val fraction = if (total <= 0) 0f else colored.toFloat() / total.toFloat()
    val color = Color(accent)
    Box(
        modifier = modifier
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(mix(color, CrayonerColors.Card, 0.82f))
            .semantics { contentDescription = label },
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(4.dp))
                .background(color),
        )
    }
}
