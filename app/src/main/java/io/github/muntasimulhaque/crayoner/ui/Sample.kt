package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.crayoner.R
import io.github.muntasimulhaque.crayoner.core.Page
import io.github.muntasimulhaque.crayoner.core.Pages

/**
 * The picture to copy, as one more round button in the bar.
 *
 * It is the same size, the same shape and the same shadow as every other
 * button on the screen, with the finished picture inside it instead of an
 * icon: the picture is the whole lesson of the app, and it should be the
 * most inviting thing to press. One tap holds it up big.
 *
 * On a fresh page it breathes, three slow swells and then stillness, because
 * a three year old cannot read a label that explains it and should not have
 * to guess.
 */
@Composable
fun SampleButton(
    page: Page,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    announce: Boolean = false,
) {
    val label = stringResource(R.string.show_sample)
    val side = with(LocalDensity.current) { size.roundToPx() }
    val breath = if (announce) rememberBreath(stamp = page.id.hashCode().toLong()) else 0f
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = 1f + 0.06f * breath
                scaleY = 1f + 0.06f * breath
            }
            .buttonShadow(CircleShape)
            .clip(CircleShape)
            .background(CrayonerColors.Card)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        // The whole picture, mounted in the round button like a picture in a
        // round frame: the sample is the lesson of the app, so none of it may
        // fall outside the edge of the thing that shows it.
        val side = size * PICTURE_IN_SET
        Box(modifier = Modifier.size(side)) {
            PageCanvas(
                page = page,
                fills = sampleFills(page),
                sidePx = with(LocalDensity.current) { side.roundToPx() },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** How much of the round sample button the picture itself fills. */
private const val PICTURE_IN_SET = 0.78f

/**
 * A once-in, three breath pulse at page start: zero, then three slow
 * swells, then zero forever. Reading a stamp of zero means no animation at
 * all, which is how the screenshot harness and a resumed page stay still.
 */
@Composable
private fun rememberBreath(stamp: Long): Float {
    var value by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(stamp) {
        if (stamp == 0L) {
            value = 0f
            return@LaunchedEffect
        }
        val anim = Animatable(0f)
        anim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = BREATH_MS, easing = FastOutSlowInEasing),
        ) {
            // Three sine swells across one run, so it starts and ends at rest.
            value = (kotlin.math.sin(this.value * 3f * Math.PI).toFloat()).coerceAtLeast(0f)
        }
        value = 0f
    }
    return value
}

private const val BREATH_MS = 2400

/** Every area in its own color: the picture as the book prints it. */
fun sampleFills(page: Page): Map<Int, Long> =
    page.regions.indices.associateWith { page.regions[it].fillArgb }

/**
 * The sample held up big: one tap anywhere puts it back down. The page
 * underneath is dimmed but never hidden, so the child keeps their place.
 *
 * The plate is a sheet too, taped at its corners like the page it explains:
 * the picture the child is copying and the paper they are copying it onto
 * are visibly the same kind of object.
 */
@Composable
fun SamplePeek(page: Page, onDismiss: () -> Unit) {
    val name = stringResource(pageNameRes(page.id))
    val hint = stringResource(R.string.hide_sample)
    val density = LocalDensity.current
    val tapeReach = with(density) { SHEET_TAPE_REACH.roundToPx() }
    val tapeWidth = with(density) { SHEET_TAPE_WIDTH.roundToPx() }.toFloat()
    val tapeHeight = with(density) { SHEET_TAPE_HEIGHT.roundToPx() }.toFloat()
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(CrayonerColors.Scrim)
            .pointerInput(Unit) { detectTapGestures { onDismiss() } }
            .semantics { contentDescription = hint },
        contentAlignment = Alignment.Center,
    ) {
        // The plate is sized so the picture AND its name both fit inside it,
        // with a clear margin on every side: the name belongs on the plate,
        // never floating over the dimmed screen behind it. The margin grows
        // with the screen, so the panel never feels cramped on a tablet or
        // fills the screen on a phone.
        val margin = minOf(maxWidth, maxHeight) * 0.09f
        val plateMax = minOf(maxWidth - margin * 2f, maxHeight - margin * 2f)
        val nameBlock = 54.dp
        val platePadding = 16.dp
        val side = (plateMax - platePadding * 2 - nameBlock).coerceAtLeast(96.dp)
        val total = side + platePadding * 2 + nameBlock
        val sidePx = with(density) { side.roundToPx() }
        Box(
            modifier = Modifier.size(total + SHEET_TAPE_REACH * 2),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(total)
                    .buttonShadow(PaperShape, elevation = 12.dp)
                    .clip(PaperShape)
                    .background(CrayonerColors.Card)
                    .padding(platePadding),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(modifier = Modifier.size(side)) {
                        PageCanvas(
                            page = page,
                            fills = sampleFills(page),
                            sidePx = sidePx,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Box(
                        modifier = Modifier.height(nameBlock),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleLarge,
                            color = CrayonerColors.Ink,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            Canvas(modifier = Modifier.fillMaxSize()) {
                val o = tapeReach.toFloat()
                val spots = listOf(
                    Offset(o, o) to -45f,
                    Offset(size.width - o, o) to 45f,
                    Offset(o, size.height - o) to 45f,
                    Offset(size.width - o, size.height - o) to -45f,
                )
                for ((center, angle) in spots) {
                    drawTape(center, tapeWidth, tapeHeight, angle)
                }
            }
        }
    }
}

/** The word for one page, shown to parents and read to a screen reader. */
internal fun pageNameRes(pageId: String): Int = when (pageId) {
    "sail" -> R.string.page_sail
    "tree" -> R.string.page_tree
    "balloon" -> R.string.page_balloon
    "icecream" -> R.string.page_icecream
    "mushroom" -> R.string.page_mushroom
    "kite" -> R.string.page_kite
    "flowers" -> R.string.page_flowers
    "rainbow" -> R.string.page_rainbow
    "cupcake" -> R.string.page_cupcake
    "house" -> R.string.page_house
    "car" -> R.string.page_car
    "umbrella" -> R.string.page_umbrella
    "rocket" -> R.string.page_rocket
    "train" -> R.string.page_train
    "lighthouse" -> R.string.page_lighthouse
    "castle" -> R.string.page_castle
    else -> R.string.app_name
}

/** The word for one area, read by the screen reader where it matters. */
internal fun areaNameRes(kind: String): Int = when (kind) {
    "sky" -> R.string.area_sky
    "cloud" -> R.string.area_cloud
    "clouds" -> R.string.area_clouds
    "sea" -> R.string.area_sea
    "sail" -> R.string.area_sail
    "boat" -> R.string.area_boat
    "balloon" -> R.string.area_balloon
    "stripe" -> R.string.area_stripe
    "stripes" -> R.string.area_stripes
    "basket" -> R.string.area_basket
    "kite" -> R.string.area_kite
    "tail" -> R.string.area_tail
    "rainbow" -> R.string.area_rainbow
    "hill" -> R.string.area_hill
    "grass" -> R.string.area_grass
    "wall" -> R.string.area_wall
    "walls" -> R.string.area_walls
    "roof" -> R.string.area_roof
    "roofs" -> R.string.area_roofs
    "window" -> R.string.area_window
    "windows" -> R.string.area_windows
    "door" -> R.string.area_door
    "trunk" -> R.string.area_trunk
    "leaves" -> R.string.area_leaves
    "apples" -> R.string.area_apples
    "stem" -> R.string.area_stem
    "stems" -> R.string.area_stems
    "cap" -> R.string.area_cap
    "spots" -> R.string.area_spots
    "petals" -> R.string.area_petals
    "flower_center" -> R.string.area_flower_center
    "cone" -> R.string.area_cone
    "scoop" -> R.string.area_scoop
    "cherry" -> R.string.area_cherry
    "sprinkles" -> R.string.area_sprinkles
    "table" -> R.string.area_table
    "wrapper" -> R.string.area_wrapper
    "frosting" -> R.string.area_frosting
    "road" -> R.string.area_road
    "car" -> R.string.area_car
    "wheels" -> R.string.area_wheels
    "smoke" -> R.string.area_smoke
    "track" -> R.string.area_track
    "engine" -> R.string.area_engine
    "space" -> R.string.area_space
    "stars" -> R.string.area_stars
    "flame" -> R.string.area_flame
    "fins" -> R.string.area_fins
    "body" -> R.string.area_body
    "nose" -> R.string.area_nose
    "rocks" -> R.string.area_rocks
    "tower" -> R.string.area_tower
    "lamp" -> R.string.area_lamp
    "rain" -> R.string.area_rain
    "canopy" -> R.string.area_canopy
    "panel" -> R.string.area_panel
    "pole" -> R.string.area_pole
    else -> R.string.app_name
}

/** Every page in the book, for anything that needs the list. */
internal val AllPages: List<Page> get() = Pages.all
