package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ripple
import androidx.compose.foundation.LocalIndication
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.crayoner.R
import io.github.muntasimulhaque.crayoner.core.Crayons

/**
 * Crayoner's own palette: a child's desk, a sheet of paper on it, a box of
 * crayons beside it.
 *
 * The three grounds are one steps-apart family. The desk is warm and a
 * little deeper than paper, so a sheet always stands off it; paper and every
 * card are the brightest thing on screen, because paper is what a coloring
 * book is; cardboard is the crayon box, warm and slightly toasted, which is
 * the one surface in the app that is neither desk nor paper.
 *
 * The crayon colors themselves live in core/Crayons.kt, beside the pictures
 * that use them, and every colored area on a page carries the wax grain from
 * core/WaxGrain.
 */
object CrayonerColors {
    /** The desk the whole app sits on. Warm, a step under paper. */
    val Desk = Color(0xFFF6EFE3)
    /** Paper, cards, plates: the brightest surface in the app. */
    val Card = Color(0xFFFFFDF8)
    /** Words and icons. The same ink the pictures are drawn in. */
    val Ink = Color(Crayons.INK)
    /** The brand: the crayon red of the sailboat, the book's first page. */
    val Coral = Color(Crayons.RED)
    /** Celebration only, never chrome: the star a finished picture earns. */
    val Honey = Color(0xFFEFB53A)
    /** The crayon box: toasted cardboard. */
    val Cardboard = Color(0xFFEFE1C6)
    /** The washi tape holding paper down: warm, translucent. */
    val Tape = Color(0x8CF6E7C4)
    /** The fiber running through the tape, a whisper darker. */
    val TapeFiber = Color(0x33A08B5E)
    /** One scrim for every layer that stands the world back. */
    val Scrim = Ink.copy(alpha = 0.62f)
    /** The whisper of a shadow under coins, cards and pages. */
    val Shadow = Ink.copy(alpha = 0.22f)
}

// The display face: Baloo 2, bundled offline (OFL text lives in docs/).
private val Baloo = FontFamily(
    Font(R.font.baloo2_bold, FontWeight.Bold),
    Font(R.font.baloo2_extrabold, FontWeight.ExtraBold),
)

// Every style the app uses is defined here, no defaults anywhere.
private val BrandTypography = Typography(
    displayMedium = TextStyle(
        fontFamily = Baloo,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 44.sp,
        lineHeight = 50.sp,
    ),
    // The shelf's wordmark: one clear step above the picture names.
    displaySmall = TextStyle(
        fontFamily = Baloo,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Baloo,
        fontWeight = FontWeight.Bold,
        fontSize = 25.sp,
        lineHeight = 31.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Baloo,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 26.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Baloo,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
)

private val BrandScheme = lightColorScheme(
    primary = CrayonerColors.Coral,
    onPrimary = CrayonerColors.Card,
    background = CrayonerColors.Desk,
    onBackground = CrayonerColors.Ink,
    surface = CrayonerColors.Desk,
    onSurface = CrayonerColors.Ink,
)

/**
 * The one theme: the desk, the paper, the two voices (Baloo for display, the
 * system face for reading under it), and a soft ink ripple. Material's
 * default ripple paints itself in the primary color, so every tap would
 * flash coral; ink at a tenth is the quiet answer a button already gives
 * with its shadow.
 */
@Composable
fun CrayonerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BrandScheme,
        typography = BrandTypography,
    ) {
        CompositionLocalProvider(
            LocalIndication provides ripple(color = CrayonerColors.Ink.copy(alpha = 0.10f)),
            content = content,
        )
    }
}
