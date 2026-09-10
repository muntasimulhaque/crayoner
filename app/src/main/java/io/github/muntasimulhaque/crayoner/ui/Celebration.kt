package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.crayoner.R
import io.github.muntasimulhaque.crayoner.core.Crayons
import io.github.muntasimulhaque.crayoner.core.Page
import io.github.muntasimulhaque.crayoner.core.star
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * The finish: the finished picture held up on one clean sheet, paper
 * confetti in the colors the child actually used, and the two ways onward
 * under it. No score, no timer, no fail state: the picture being finished
 * is the whole reward, and it is celebrated whether or not it matches.
 */
@Composable
fun Celebration(
    page: Page,
    fills: Map<Int, Long>,
    onAgain: () -> Unit,
    onHome: () -> Unit,
) {
    // The confetti falls in the child's own colors, taken from the page they
    // just finished: a picture colored in five different blues celebrates in
    // five different blues, which is the app saying look what you made.
    val confetti = remember(page.id, fills) { buildConfetti(page, fills) }
    val praise = stringResource(R.string.well_done)
    val fall = remember { Animatable(0f) }
    LaunchedEffect(page.id) {
        fall.animateTo(1f, tween(3400, easing = androidx.compose.animation.core.LinearEasing))
    }
    val pop = remember { Animatable(0.55f) }
    LaunchedEffect(page.id) {
        pop.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(CrayonerColors.Scrim)
                // The sheet owns the screen: taps that miss the coins land on
                // the scrim and stop there, never on the page underneath.
                .pointerInput(Unit) { detectTapGestures { } },
        )
        Canvas(Modifier.fillMaxSize()) {
            if (fall.value < 1f) drawConfetti(confetti, fall.value)
        }
        Column(
            modifier = Modifier.fillMaxSize().semantics { contentDescription = praise },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CelebrationPlate(
                page = page,
                fills = fills,
                pop = pop.value,
                onAgain = onAgain,
                onHome = onHome,
            )
        }
    }
}

/** One clean sheet: the finished work, the praise, then the two coins. */
@Composable
private fun CelebrationPlate(
    page: Page,
    fills: Map<Int, Long>,
    pop: Float,
    onAgain: () -> Unit,
    onHome: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .padding(horizontal = 22.dp)
            .buttonShadow(RoundedCornerShape(28.dp), elevation = 10.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(CrayonerColors.Card)
            .padding(horizontal = 18.dp, vertical = 18.dp),
    ) {
        // The sheet yields to the shape of the field it lands in, so the
        // picture, the praise and both coins always fit; a scroll is the last
        // safety net for a field too short for even the smallest sheet.
        val landscape = maxWidth > maxHeight
        val pictureSide = if (landscape) {
            minOf(212.dp, maxHeight * 0.72f)
        } else {
            minOf(maxWidth, 258.dp, (maxHeight - 244.dp).coerceAtLeast(104.dp))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (landscape) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FinishedPicture(page = page, fills = fills, side = pictureSide, pop = pop)
                    Spacer(Modifier.width(22.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Praise()
                        Spacer(Modifier.height(16.dp))
                        FinishButtons(onAgain = onAgain, onHome = onHome)
                    }
                }
            } else {
                FinishedPicture(page = page, fills = fills, side = pictureSide, pop = pop)
                Spacer(Modifier.height(14.dp))
                Praise()
                Spacer(Modifier.height(16.dp))
                FinishButtons(onAgain = onAgain, onHome = onHome)
            }
        }
    }
}

/** The finished work, popped to its place and centered. */
@Composable
private fun FinishedPicture(page: Page, fills: Map<Int, Long>, side: Dp, pop: Float) {
    val sidePx = with(androidx.compose.ui.platform.LocalDensity.current) { side.roundToPx() }
    Box(
        modifier = Modifier
            .width(side)
            .height(side)
            .clip(RoundedCornerShape(14.dp))
            .graphicsLayer {
                scaleX = pop
                scaleY = pop
                alpha = ((pop - 0.5f) / 0.5f).coerceIn(0f, 1f)
            },
    ) {
        PageCanvas(
            page = page,
            fills = fills,
            sidePx = sidePx,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** The one praise, in the display face. */
@Composable
private fun Praise() {
    Text(
        text = stringResource(R.string.well_done),
        style = MaterialTheme.typography.displayMedium,
        color = CrayonerColors.Ink,
        textAlign = TextAlign.Center,
    )
}

private val FINISH_COIN = 64.dp

/** Equal coins; Again leads by the brand coral, the way Stay leads. */
@Composable
private fun FinishButtons(onAgain: () -> Unit, onHome: () -> Unit) {
    Row(verticalAlignment = Alignment.Bottom) {
        FinishCoin(
            onClick = onAgain,
            background = CrayonerColors.Coral,
            label = stringResource(R.string.again),
            text = stringResource(R.string.again),
        ) {
            AgainIcon(color = CrayonerColors.Card, size = 30.dp)
        }
        Spacer(Modifier.width(38.dp))
        FinishCoin(
            onClick = onHome,
            background = CrayonerColors.Cardboard,
            label = stringResource(R.string.more_pictures),
            text = stringResource(R.string.more_pictures),
        ) {
            HomeIcon(color = CrayonerColors.Ink, size = 28.dp)
        }
    }
}

/** One finish coin: one size, one shadow, the ground carries the lead. */
@Composable
private fun FinishCoin(
    onClick: () -> Unit,
    background: Color,
    label: String,
    text: String,
    icon: @Composable () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircleButton(
            onClick = onClick,
            background = background,
            size = FINISH_COIN,
            label = label,
        ) {
            icon()
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = CrayonerColors.Ink,
        )
    }
}

/** The same again: one fresh sheet of the picture just finished. */
@Composable
private fun AgainIcon(color: Color, size: Dp) {
    androidx.compose.foundation.Canvas(modifier = Modifier.width(size).height(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
            width = w * 0.11f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        drawArc(
            color,
            startAngle = -60f,
            sweepAngle = 300f,
            useCenter = false,
            topLeft = Offset(w * 0.20f, h * 0.20f),
            size = Size(w * 0.60f, h * 0.60f),
            style = stroke,
        )
        // The arrowhead, pointing the way the sweep travels.
        val head = Path().apply {
            moveTo(w * 0.86f, h * 0.10f)
            lineTo(w * 0.66f, h * 0.22f)
            lineTo(w * 0.86f, h * 0.36f)
            close()
        }
        drawPath(head, color)
    }
}

private fun DrawScope.drawConfetti(pieces: List<Confetti>, t: Float) {
    for (p in pieces) {
        val local = ((t - p.delay) / p.fall).coerceIn(0.0, 1.0)
        if (local <= 0.0) continue
        val y = ((-0.08 + local * 1.25) * size.height).toFloat()
        val x = ((p.x0 + p.sway * sin(local * p.freq * 2 * PI + p.phase)) * size.width).toFloat()
        val alpha = if (local > 0.82) ((1.0 - local) / 0.18).toFloat() else 1f
        val angle = (p.rot0 + t * p.spin).toFloat()
        withTransform({
            translate(x, y)
            rotate(angle)
        }) {
            when (p.kind) {
                0 -> drawCircle(p.color.copy(alpha = alpha), radius = p.size.toFloat())
                1 -> drawRoundRect(
                    p.color.copy(alpha = alpha),
                    topLeft = Offset(-p.size.toFloat(), -p.size.toFloat()),
                    size = Size(p.size.toFloat() * 2, p.size.toFloat() * 2),
                    cornerRadius = CornerRadius(2f),
                )
                2 -> drawPath(triangle(p.size.toFloat()), p.color.copy(alpha = alpha))
                else -> drawPath(starPath(p.size.toFloat()), p.color.copy(alpha = alpha))
            }
        }
    }
}

private fun triangle(s: Float): Path = Path().apply {
    moveTo(0f, -s)
    lineTo(s * 0.9f, s * 0.7f)
    lineTo(-s * 0.9f, s * 0.7f)
    close()
}

private fun starPath(s: Float): Path = Path().apply {
    val pts = star(0.0, 0.0, s.toDouble(), (s * 0.45).toDouble(), 5)
    val list = pts.points
    moveTo(list[0].x.toFloat(), list[0].y.toFloat())
    for (i in 1 until list.size) lineTo(list[i].x.toFloat(), list[i].y.toFloat())
    close()
}

private class Confetti(
    val x0: Double,
    val delay: Double,
    val fall: Double,
    val sway: Double,
    val freq: Double,
    val phase: Double,
    val rot0: Double,
    val spin: Double,
    val size: Double,
    val color: Color,
    val kind: Int,
)

/**
 * Confetti in the child's own colors: whatever they actually put on the
 * page, plus one honey for the celebration. A picture they colored in their
 * own way celebrates in their own way.
 */
private fun buildConfetti(page: Page, fills: Map<Int, Long>): List<Confetti> {
    val rnd = Random(page.id.hashCode().toLong())
    val used = fills.values.toSet().ifEmpty { setOf(Crayons.YELLOW, Crayons.SKY) }
    val colors = used.map { Color(it) } + CrayonerColors.Honey
    return List(56) {
        Confetti(
            x0 = 0.05 + rnd.nextDouble() * 0.9,
            delay = rnd.nextDouble() * 0.25,
            fall = 0.85 + rnd.nextDouble() * 0.5,
            sway = 0.02 + rnd.nextDouble() * 0.05,
            freq = 1.0 + rnd.nextDouble() * 2.0,
            phase = rnd.nextDouble() * 2 * PI,
            rot0 = rnd.nextDouble() * 360,
            spin = (rnd.nextDouble() - 0.5) * 720,
            size = 5.0 + rnd.nextDouble() * 7.0,
            color = colors[rnd.nextInt(colors.size)],
            kind = rnd.nextInt(4),
        )
    }
}
