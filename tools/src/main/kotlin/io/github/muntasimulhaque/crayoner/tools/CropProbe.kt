package io.github.muntasimulhaque.crayoner.tools

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * A throwaway review tool: crop and enlarge a region of any PNG, so a
 * screenshot can be looked at the way an eye looks at a corner of a page.
 * Usage: cropProbe <in.png> <out.png> <x> <y> <w> <h> <zoom>
 */
fun main(args: Array<String>) {
    val inFile = File(args[0])
    val outFile = File(args[1])
    val x = args[2].toInt()
    val y = args[3].toInt()
    val w = args[4].toInt()
    val h = args[5].toInt()
    val zoom = args[6].toInt()
    val src = ImageIO.read(inFile)
    val crop = src.getSubimage(x, y, minOf(w, src.width - x), minOf(h, src.height - y))
    val out = BufferedImage(crop.width * zoom, crop.height * zoom, BufferedImage.TYPE_INT_ARGB)
    val g = out.createGraphics()
    g.drawImage(crop, 0, 0, out.width, out.height, null)
    g.dispose()
    outFile.parentFile?.mkdirs()
    ImageIO.write(out, "png", outFile)
    println("cropProbe: ${outFile.path} (${out.width}x${out.height})")
}
