package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.crayoner.R
import io.github.muntasimulhaque.crayoner.core.CrayonShape
import io.github.muntasimulhaque.crayoner.core.Crayons

/**
 * The crayon box itself: thirty two real crayons, laid out the way the box
 * lays them out. It opens over the page rather than sitting under it
 * forever, because a tray of thirty two crayons on a phone costs the sheet
 * the room it needs to be colored on.
 *
 * The one in hand stands up out of the tray of lying crayons, so a child can
 * always see which color they are holding without leaving the page.
 */
@Composable
fun CrayonBoxSheet(
    selected: Long,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appear = remember { Animatable(0f) }
    val hint = stringResource(R.string.close_box)
    androidx.compose.runtime.LaunchedEffect(Unit) {
        appear.animateTo(1f, tween(190, easing = androidx.compose.animation.core.FastOutSlowInEasing))
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CrayonerColors.Scrim)
            .clickable(role = Role.Button, onClick = onDismiss)
            .semantics { contentDescription = hint },
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .graphicsLayer {
                    val a = appear.value
                    alpha = a
                    scaleX = 0.92f + 0.08f * a
                    scaleY = 0.92f + 0.08f * a
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                }
                .buttonShadow(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp), elevation = 12.dp)
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .background(CrayonerColors.Cardboard)
                .clickable(enabled = false, onClick = {})
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val columns = when {
                    maxWidth >= 620.dp -> 8
                    maxWidth >= 460.dp -> 6
                    maxWidth >= 340.dp -> 5
                    else -> 4
                }
                val gaps = 8.dp
                val cell = (maxWidth - gaps * (columns - 1)) / columns
                val rows = Crayons.all.chunked(columns)
                Column(
                    verticalArrangement = Arrangement.spacedBy(gaps),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    for (row in rows) {
                        Row(horizontalArrangement = Arrangement.spacedBy(gaps)) {
                            for (argb in row) {
                                ColorSeat(
                                    argb = argb,
                                    selected = argb == selected,
                                    onClick = { onPick(argb) },
                                    cell = cell,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * One crayon's place in the box: the crayon, lying the way crayons lie in a
 * box. The one in the child's hand stands up, which is how a held crayon
 * looks and how a chosen color reads at a glance, and the rest lie down.
 *
 * There is no ring, no plate and no tick behind the held crayon, because a
 * crayon standing out of a box full of lying ones is already unmistakable,
 * and a mark drawn around it would be chrome on a box of crayons.
 */
@Composable
private fun ColorSeat(
    argb: Long,
    selected: Boolean,
    onClick: () -> Unit,
    cell: Dp,
) {
    val label = stringResource(crayonNameRes(argb))
    val lift = animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "seat-lift",
    ).value
    val lying = cell * (0.94f + 0.05f * lift)
    val thickness = lying * CrayonShape.THICKNESS.toFloat()
    // A standing crayon is a little taller than a lying one is deep, so the
    // cell always has room for the one the child is holding.
    val cellHeight = lying * 0.62f
    Box(
        modifier = Modifier
            .size(cell, cellHeight)
            .semantics {
                contentDescription = label
                this.selected = selected
            }
            .clickable(role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        SeatedCrayon(
            argb = argb,
            selected = selected,
            lift = lift,
            lying = lying,
            thickness = thickness,
            cellHeight = cellHeight,
        )
    }
}

/**
 * The crayon in one seat: lying if it is waiting, standing if it is the one
 * in the child's hand, with the soft shadow a lifted crayon throws on the
 * cardboard under it.
 */
@Composable
private fun SeatedCrayon(
    argb: Long,
    selected: Boolean,
    lift: Float,
    lying: Dp,
    thickness: Dp,
    cellHeight: Dp,
) {
    // The shadow under a raised crayon, which grows as the crayon comes off
    // the cardboard and settles back down with it.
    if (lift > 0.02f) {
        Canvas(modifier = Modifier.size(lying, thickness)) {
            drawOval(
                color = CrayonerColors.Shadow.copy(alpha = 0.34f * lift),
                topLeft = Offset(0f, size.height * 0.30f),
                size = Size(size.width * 0.86f, size.height * 0.68f),
            )
        }
    }
    if (selected) {
        val height = cellHeight * 0.98f
        CrayonGlyph(
            color = Color(argb),
            modifier = Modifier.size(
                width = height * CrayonShape.THICKNESS.toFloat(),
                height = height,
            ),
        )
    } else {
        CrayonGlyph(
            color = Color(argb),
            lying = true,
            modifier = Modifier.size(width = lying, height = thickness),
        )
    }
}
