package seamcarving

import java.io.File
import javax.imageio.ImageIO

fun main(args: Array<String>) {
    var indx = args.indexOf("-in")
    if (indx == -1 || indx == args.lastIndex) {
        throw IllegalArgumentException("missing '-in' argument")
    }
    val infileName = args[indx + 1]

    indx = args.indexOf("-out")
    if (indx == -1 || indx == args.lastIndex) {
        throw IllegalArgumentException("missing '-out' argument")
    }
    val outfileName = args[indx + 1]

    val inImage = ImageIO.read(File(infileName))

    for (x in 0 until inImage.width) {
        for (y in 0 until inImage.height) {
            inImage.setRGB(x, y, inImage.getRGB(x, y).inv())
        }
    }
    ImageIO.write(inImage, "png", File(outfileName))

}
