package io.github.muntasimulhaque.crayoner.ui

import io.github.muntasimulhaque.crayoner.core.Page

/** Every area in its own color: the picture as the book prints it. */
fun sampleFills(page: Page): Map<Int, Long> =
    page.regions.indices.associateWith { page.regions[it].fillArgb }
