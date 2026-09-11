package io.github.muntasimulhaque.crayoner.ui

import androidx.compose.foundation.LocalIndication
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ripple
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
    /** Celebration only, never chrome: the seal a finished picture earns. */
    val Honey = Color(0xFFEFB53A)
    /** The crayon box: toasted cardboard. */
    val Cardboard = Color(0xFFEFE1C6)
    /**
     * The tape holding paper down. It is cream, lighter than the desk and
     * deeper than the paper, with a real edge: tape you cannot see is not
     * holding anything, and the whole reason the strip is there is to say
     * this sheet is lying on a table.
     */
    val Tape = Color(0xE0EFD9A8)
    /** The edge of the roll, and the fiber running through it. */
    val TapeEdge = Color(0x4D9C7F45)
    val TapeFiber = Color(0x59B08F52)
    /** One scrim for every layer that stands the world back. */
    val Scrim = Ink.copy(alpha = 0.62f)
    /** The whisper of a shadow under coins, cards and pages. */
    val Shadow = Ink.copy(alpha = 0.22f)
}

/**
 * The one face: Chewy, bundled offline (Apache 2.0, text in docs/). Every
 * word in the app is written in it and painted with [WaxInkBrush], so the
 * words and the pictures are made of the same thing, and a child who cannot
 * read yet still sees a page that was drawn by hand rather than set in type.
 */
private val Chewy = FontFamily(
    Font(R.font.chewy, FontWeight.Normal),
)

// Every style the app uses is defined here, no defaults anywhere.
private val BrandTypography = Typography(
    displayMedium = TextStyle(
        fontFamily = Chewy,
        fontWeight = FontWeight.Normal,
        fontSize = 42.sp,
        lineHeight = 50.sp,
        brush = WaxInkBrush,
    ),
    // The shelf's wordmark: one clear step above the picture names.
    displaySmall = TextStyle(
        fontFamily = Chewy,
        fontWeight = FontWeight.Normal,
        fontSize = 37.sp,
        lineHeight = 44.sp,
        brush = WaxInkBrush,
    ),
    titleLarge = TextStyle(
        fontFamily = Chewy,
        fontWeight = FontWeight.Normal,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        brush = WaxInkBrush,
    ),
    titleMedium = TextStyle(
        fontFamily = Chewy,
        fontWeight = FontWeight.Normal,
        fontSize = 21.sp,
        lineHeight = 26.sp,
        brush = WaxInkBrush,
    ),
    bodyLarge = TextStyle(
        fontFamily = Chewy,
        fontWeight = FontWeight.Normal,
        fontSize = 19.sp,
        lineHeight = 27.sp,
        brush = WaxInkBrush,
    ),
    bodyMedium = TextStyle(
        fontFamily = Chewy,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        brush = WaxInkBrush,
    ),
    labelLarge = TextStyle(
        fontFamily = Chewy,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 21.sp,
        brush = WaxInkBrush,
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
 * The one theme: the desk, the paper, the one hand-drawn voice, and a soft
 * ink ripple. Material's default ripple paints itself in the primary color,
 * so every tap would flash coral; ink at a tenth is the quiet answer a
 * button already gives with its shadow.
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
