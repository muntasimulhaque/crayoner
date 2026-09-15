package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import io.github.muntasimulhaque.crayoner.core.CrayonShape

/**
 * One crayon's place in the box: the crayon, lying the way crayons lie in a
 * box.
 *
 * The one in the child's hand lies with the rest, a little longer and drawn
 * with a heavier line, and that is its whole selection mark: no ring, no
 * plate, no tick and no shadow behind it, because a mark drawn around a
 * crayon is chrome on a box of crayons. Nothing is drawn around any crayon
 * in the box (see [CrayonGlyph]): the wax is the wax.
 */
@Composable
internal fun ColorSeat(
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
    // The one in hand grows a little out of the row it lies in, and never
    // past the cell it belongs to.
    val lying = cell * (0.90f + 0.08f * lift)
    val thickness = lying * CrayonShape.THICKNESS.toFloat()
    val cellHeight = cell * 0.62f
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
        CrayonGlyph(
            color = Color(argb),
            lying = true,
            lineBoost = if (selected) SELECTED_LINE else 1f,
            modifier = Modifier.size(width = lying, height = thickness),
        )
    }
}

/** How much heavier the line is on the crayon the child is holding. */
private const val SELECTED_LINE = 1.7f
