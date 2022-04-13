package seamcarving

import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.util.*
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

    indx = args.indexOf("-width")
    if (indx == -1 || indx == args.lastIndex) {
        throw IllegalArgumentException("missing '-width' argument")
    }
    val width = args[indx + 1].toInt()

    indx = args.indexOf("-height")
    if (indx == -1 || indx == args.lastIndex) {
        throw IllegalArgumentException("missing '-height' argument")
    }
    val height = args[indx + 1].toInt()

    val inImage = ImageIO.read(File(infileName))
    var energyMatrix = inImage.calculateEnergyMatrix()
    energyMatrix = reduceEnergyMatrix(energyMatrix, width, height)
    println("After reduction energyMatrix: ${energyMatrix.size} X ${energyMatrix[0].size}")

    val outImg = reduceImage(inImage, energyMatrix)

    ImageIO.write(outImg, "png", File(outfileName))
}

fun reduceEnergyMatrix(
    energyMatrix: MutableList<MutableList<Double>>,
    width: Int,
    height: Int
): MutableList<MutableList<Double>> {
    println("energyMatrix: ${energyMatrix.size} X ${energyMatrix[0].size}")

    val energyMatrixCopy =
        MutableList(energyMatrix.size) { r -> MutableList(energyMatrix.first().size) { c -> energyMatrix[r][c] } }
    println("energyMatrixCopy: ${energyMatrixCopy.size} X ${energyMatrixCopy[0].size}")
//    println("imaginaryMatrix: ${imaginaryMatrix.size} X ${imaginaryMatrix[0].size}")
    for (w in 1..width) {
        val seam = findBestVerticalSeam(energyMatrixCopy)
        for (s in seam) {
            energyMatrixCopy[s.second].removeAt(s.first)
        }
        println(seam)
    }
    val energyMatrixT =
        MutableList(energyMatrixCopy.first().size) { c -> MutableList(energyMatrixCopy.size) { r -> energyMatrixCopy[r][c] } }
    println("energyMatrixT: ${energyMatrixT.size} X ${energyMatrixT[0].size}")

    for (h in 1..height) {
        val seam = findBestVerticalSeam(energyMatrixT)

        println(seam)
        for (s in seam) {
            energyMatrixT[s.second].removeAt(s.first)
        }

    }
    return MutableList(energyMatrixT.first().size) { c -> MutableList(energyMatrixT.size) { r -> energyMatrixT[r][c] } }
}

fun reduceImage(inImage: BufferedImage, energyMatrix: MutableList<MutableList<Double>>): BufferedImage {
//    println("Image widx X height = ${inImage.width} X ${inImage.height}")
    val outImg = BufferedImage(energyMatrix.first().size, energyMatrix.size, inImage.type)

    for (r in energyMatrix.withIndex()) {
        for (c in r.value.withIndex())
//        println("${s.first}, ${s.second}")
            outImg.setRGB(c.index, r.index, inImage.getRGB(c.index, r.index))
    }
    return outImg

}

class Node(
    val x: Int, val y: Int,
    var distance: Double,
    val energy: Double,
    var from: Node?,
    var processded: Boolean = false,
    val imaginary: Boolean = false
) : Comparable<Node> {
    override fun compareTo(other: Node): Int {
        return this.distance.compareTo(other.distance)
    }
}

fun findBestVerticalSeam(energyMatrix1: MutableList<MutableList<Double>>): List<Pair<Int, Int>> {
    val height = energyMatrix1.size
    val width = energyMatrix1[0].size
    val imaginaryMatrix = Array(height + 2) { r ->
        if (r == 0 || r == height + 1)
            Array(width) { c -> Node(c, r, 0.0, 0.0, null, false, true) }
        else {
            Array(width) { c -> Node(c, r, Double.POSITIVE_INFINITY, energyMatrix1[r - 1][c], null) }
        }
    }
    val result = mutableListOf<Pair<Int, Int>>()

    val q = PriorityQueue<Node>()
    q.add(imaginaryMatrix[0].first())


    while (q.isNotEmpty()) {
        val node = q.remove()
        val neighbours = getVerticalNeighbours(imaginaryMatrix, node)

        for (nn in neighbours) {
            val newDistance = node.distance + nn.energy
            if (nn.imaginary || newDistance < nn.distance) {
                nn.distance = newDistance
                nn.from = node
                q.add(nn)
            }

        }
        node.processded = true
    }
    var n: Node? = imaginaryMatrix.last().last()
    while (n != null) {
        if (n.y in 1 until imaginaryMatrix.lastIndex)
            result.add(0, n.x to n.y - 1)
        n = n.from
    }
    return result.toList()
}


fun getVerticalNeighbours(imaginaryMatrix: Array<Array<Node>>, n: Node): List<Node> {
    val list = mutableListOf<Node>()


    if (n.y == 0 || n.y == imaginaryMatrix.lastIndex) {
        if (n.x < imaginaryMatrix[n.y].lastIndex) {
            imaginaryMatrix[n.y][n.x + 1].also {
                if (!it.processded)
                    list.add(it)
            }

        }
    }
    if (n.y < imaginaryMatrix.lastIndex) {
        val s = if (n.imaginary) n.x else (n.x - 1).coerceAtLeast(0)
        val e = if (n.imaginary) n.x else (n.x + 1).coerceAtMost(imaginaryMatrix[n.y].lastIndex)
        (s..e).forEach { c ->
            imaginaryMatrix[n.y + 1][c].also {
                if (!it.processded)
                    list.add(it)
            }
        }
    }

    return list
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

fun BufferedImage.calculateEnergyMatrix(): MutableList<MutableList<Double>> {
    val result = MutableList(this.height) { MutableList(this.width) { 0.0 } }
    runBlocking {
        val jobs = mutableListOf<Job>()

// inner central region
        for (y in 1 until height - 1) {
            val job = launch {
                for (x in 1 until width - 1) {

                    result[y][x] = sqrt(deltaX(x, y) + deltaY(x, y))
                }
            }
            jobs.add(job)

        }
        joinAll()
    }

    runBlocking {
        launch {
            for (y in 1 until height - 1) {
                //left border
                result[y][0] = sqrt(deltaX(1, y) + deltaY(0, y))
                //right border
                result[y][width - 1] = sqrt(deltaX(width - 2, y) + deltaY(width - 1, y))

            }
        }
        launch {
            for (x in 1 until width - 1) {
                //top border
                result[0][x] = sqrt(deltaX(x, 0) + deltaY(x, 1))
                //bottom border
                result[height - 1][x] = sqrt(deltaX(x, height - 1) + deltaY(x, height - 2))

            }
        }
        joinAll()
    }
    //top left pixel
    result[0][0] = sqrt(deltaX(1, 0) + deltaY(0, 1))

    //top right pixel
    result[0][width - 1] = sqrt(
        deltaX(width - 2, 0) +
                deltaY(width - 1, 1)
    )

    //bottom left pixel
    result[height - 1][0] = sqrt(
        deltaX(1, height - 1) +
                deltaY(0, height - 2)
    )

    //bottom right pixel
    result[height - 1][width - 1] = sqrt(
        deltaX(width - 2, height - 1) +
                deltaY(width - 1, height - 2)
    )

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