package io.github.muntasimulhaque.crayoner.ui

import io.github.muntasimulhaque.crayoner.R

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
