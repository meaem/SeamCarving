package seamcarving

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.absoluteValue
import kotlin.math.sqrt

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


    val energyMatrix = inImage.calculateEnergyMatrix()
    val maxEnergy = energyMatrix.maxOf {
        it.maxOf { it }
    }
    for (x in 0 until inImage.width) {
        for (y in 0 until inImage.height) {

            val intensity = (255.0 * energyMatrix[x][y] / maxEnergy).toInt()

            inImage.setRGB(x, y, Color(intensity, intensity, intensity).rgb)
        }
    }
    ImageIO.write(inImage, "png", File(outfileName))

}

fun BufferedImage.deltaX(x: Int, y: Int): Double {
    val prevColor = Color(this.getRGB(x - 1, y))
    val nextColor = Color(this.getRGB(x + 1, y))
    return prevColor.delta(nextColor).gradient().toDouble()
}

fun BufferedImage.deltaY(x: Int, y: Int): Double {
    val aboveColor = Color(this.getRGB(x, y - 1))
    val bottomColor = Color(this.getRGB(x, y + 1))
    return aboveColor.delta(bottomColor).gradient().toDouble()
}

fun BufferedImage.calculateEnergyMatrix(): Array<Array<Double>> {
    val result = Array(this.width) { Array(this.height) { 0.0 } }

    for (x in 1 until this.width - 1) {
        for (y in 1 until this.height - 1) {
            result[x][y] = sqrt(deltaX(x, y) + deltaY(x, y))
        }
    }
//left border
    for (y in 1 until this.height - 1) {
        result[0][y] = sqrt(deltaX(1, y) + deltaY(0, y))
    }

    //right border
    for (y in 1 until this.height - 1) {
        result[width - 1][y] = sqrt(deltaX(width - 2, y) + deltaY(width - 1, y))
    }

    //top border
    for (x in 1 until this.width - 1) {
        result[x][0] = sqrt(deltaX(x, 0) + deltaY(x, 1))
    }

    //bottom border
    for (x in 1 until this.width - 1) {
        result[x][height - 1] = sqrt(deltaX(x, height - 1) + deltaY(x, height - 2))
    }


    result[0][0] = sqrt(deltaX(1, 0) + deltaY(0, 1))

    result[width - 1][0] = sqrt(deltaX(width - 2, 0) + deltaY(width - 1, 1))


    result[0][height - 1] = sqrt(
        deltaX(1, height - 1) +
                deltaY(0, height - 2)
    )

    result[width - 1][height - 1] = sqrt(
        deltaX(width - 2, height - 1) +
                deltaY(width - 1, height - 2)
    )


//
    return result
}

fun Color.delta(other: Color): Color {
    return Color(
        (this.red - other.red).absoluteValue,
        (this.green - other.green).absoluteValue,
        (this.blue - other.blue).absoluteValue
    )
}

fun Color.gradient(): Int {
    return this.red * this.red +
            this.green * this.green +
            this.blue * this.blue
}