package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import io.github.muntasimulhaque.crayoner.core.ArcBand
import io.github.muntasimulhaque.crayoner.core.Circ
import io.github.muntasimulhaque.crayoner.core.Ell
import io.github.muntasimulhaque.crayoner.core.Poly
import io.github.muntasimulhaque.crayoner.core.RRect
import io.github.muntasimulhaque.crayoner.core.Shape

/** One shape as a path, in page units scaled to [side] pixels. */
fun pathOf(shape: Shape, side: Double): Path {
    fun px(v: Double) = (v * side).toFloat()
    return when (shape) {
        is Circ -> Path().apply {
            addOval(
                Rect(
                    Offset(px(shape.c.x - shape.r), px(shape.c.y - shape.r)),
                    Size(px(shape.r * 2), px(shape.r * 2)),
                ),
            )
        }
        is Ell -> {
            val path = Path().apply {
                addOval(
                    Rect(
                        Offset(px(shape.c.x - shape.rx), px(shape.c.y - shape.ry)),
                        Size(px(shape.rx * 2), px(shape.ry * 2)),
                    ),
                )
            }
            if (shape.angleDeg != 0.0) {
                path.transform(rotationMatrix(shape.angleDeg, px(shape.c.x), px(shape.c.y)))
            }
            path
        }
        is RRect -> {
            val path = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(
                            Offset(px(shape.x), px(shape.y)),
                            Size(px(shape.w), px(shape.h)),
                        ),
                        cornerRadius = CornerRadius(px(shape.radius)),
                    ),
                )
            }
            if (shape.angleDeg != 0.0) {
                path.transform(
                    rotationMatrix(
                        shape.angleDeg,
                        px(shape.x + shape.w / 2),
                        px(shape.y + shape.h / 2),
                    ),
                )
            }
            path
        }
        is ArcBand -> arcBandPath(shape, side)
        is Poly -> Path().apply {
            val points = shape.points
            if (points.isEmpty()) return@apply
            moveTo(px(points[0].x), px(points[0].y))
            for (i in 1 until points.size) lineTo(px(points[i].x), px(points[i].y))
            close()
        }
    }
}

private fun arcBandPath(band: ArcBand, side: Double): Path {
    fun px(v: Double) = (v * side).toFloat()
    val steps = 64
    val start = Math.toRadians(band.startDeg)
    val span = Math.toRadians(band.sweepDegrees())
    val cx = band.c.x
    val cy = band.c.y
    return Path().apply {
        fun ring(radius: Double, i: Int) {
            val a = start + span * i / steps
            lineTo(px(cx + radius * Math.cos(a)), px(cy + radius * Math.sin(a)))
        }
        moveTo(px(cx + band.rInner * Math.cos(start)), px(cy + band.rInner * Math.sin(start)))
        lineTo(px(cx + band.rOuter * Math.cos(start)), px(cy + band.rOuter * Math.sin(start)))
        for (i in 1..steps) ring(band.rOuter, i)
        for (i in steps downTo 0) ring(band.rInner, i)
        close()
    }
}

/** A rotation around a pivot, as a matrix Compose's Path.transform takes. */
private fun rotationMatrix(angleDeg: Double, pivotX: Float, pivotY: Float): Matrix = Matrix().apply {
    translate(pivotX, pivotY)
    rotateZ(angleDeg.toFloat())
    translate(-pivotX, -pivotY)
}

/** The union of several shapes, so inner seams never print. */
fun unionOf(shapes: List<Shape>, side: Double): Path {
    if (shapes.isEmpty()) return Path()
    var path = pathOf(shapes[0], side)
    for (i in 1 until shapes.size) {
        val next = Path()
        next.op(path, pathOf(shapes[i], side), PathOperation.Union)
        path = next
    }
    return path
}
