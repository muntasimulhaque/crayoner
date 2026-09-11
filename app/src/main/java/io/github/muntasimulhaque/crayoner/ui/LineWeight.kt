package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

/** The line weight a picture's outline is printed with, in pixels. */
internal fun pageOutlineStroke(side: Float): Stroke = Stroke(
    width = (side * STROKE_FRACTION).coerceAtLeast(1.6f),
    cap = StrokeCap.Round,
    join = StrokeJoin.Round,
)

/** The one line weight, as a fraction of the page side. Mirrors RenderKit. */
const val STROKE_FRACTION = 0.0072f

/**
 * How wide one crayon mark is, as a fraction of the page side. A real crayon
 * tip is about five millimeters across on a page of about two hundred, and
 * this is a little past that: a three year old's scribble still covers the
 * paper, and a sprinkle or a window frame still gets a mark of its own
 * instead of being swallowed whole.
 */
const val CRAYON_TIP_FRACTION = 0.030f

/** How wide the rubber is, which is wider than any crayon's tip. */
const val ERASER_TIP_FRACTION = 0.044f
