package seamcarving

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
    val inImage = ImageIO.read(File(infileName))
    val energyMatrix = inImage.calculateEnergyMatrix()
//    val maxEnergy = energyMatrix.maxOf {
//        it.maxOf { it }
//    }
//
//    for (x in 0 until inImage.width) {
//        for (y in 0 until inImage.height) {
//            val intensity = (255.0 * energyMatrix[y][x] / maxEnergy).toInt()
//            inImage.setRGB(x, y, Color(intensity, intensity, intensity).rgb)
//        }
//    }
    val seam = findBestSeam(energyMatrix)
//    println(seam)
    updateImage(inImage, seam)

    ImageIO.write(inImage, "png", File(outfileName))
}

fun updateImage(inImage: BufferedImage, seam: List<Pair<Int, Int>>) {
//    println("Image widx X height = ${inImage.width} X ${inImage.height}")
    for (s in seam) {
//        println("${s.first}, ${s.second}")
        inImage.setRGB(s.first, s.second, Color.RED.rgb)
    }

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

fun findBestSeam(energyMatrix1: Array<Array<Double>>): List<Pair<Int, Int>> {
    val height = energyMatrix1.size
    val width = energyMatrix1[0].size
    val imaginaryMatrix = Array(height + 2) { r ->
        if (r == 0 || r == height + 1)
            Array(width) { c -> Node(c, r, 0.0, 0.0, null,false,true) }
        else {
            Array(width) { c -> Node(c, r, Double.POSITIVE_INFINITY, energyMatrix1[r - 1][c], null) }
        }
//
    }
//    var unprocessed = width * (height + 2)

    println("energyMatrix1: ${energyMatrix1.size} X ${energyMatrix1[0].size}")
    println("imaginaryMatrix: ${imaginaryMatrix.size} X ${imaginaryMatrix[0].size}")
//    println(imaginaryMatrix.map { it.joinToString(" ") }.joinToString("\n"))
    val result = mutableListOf<Pair<Int, Int>>()

    val q = PriorityQueue<Node>()
    q.add(imaginaryMatrix[0].first())


    while (q.isNotEmpty()) {
        val node = q.remove()
//        q.clear()
//        val node = imaginaryMatrix[x][y]
        val neighbours = getNeighbours(imaginaryMatrix, node)

        for (nn in neighbours) {
            val newDistance = node.distance + nn.energy
            if ( nn.imaginary  ||   newDistance < nn.distance) {
                nn.distance = newDistance
                nn.from = node
                q.add(nn)
            }

        }
        node.processded = true
//println("q size :${q.size}")
    }
//    imaginaryMatrix[0][0].distance=0.0

//    var minCol = energyMatrix1.map { it[0] }.withIndex().minByOrNull { (_, f) -> f }!!.index
//    result.add(minCol to 0)
//
//    for (row in 1..energyMatrix[0].lastIndex) {
//        minCol += listOf(
//            energyMatrix[minCol - 1][row],
//            energyMatrix[minCol][row],
//            energyMatrix[minCol + 1][row]
//        ).withIndex()
//         .minByOrNull { (_, f) -> f }!!.index - 1
//        result.add(minCol to row)
//    }
//    result.add(   minCol  to energyMatrix.lastIndex)
    var n:Node? = imaginaryMatrix.last().last()
    while (n != null ){
        if(n.y in 1 until  imaginaryMatrix.lastIndex)
        result.add(0,n.x to n.y-1)
        n = n.from
    }
    return result.toList()
}

fun getNeighbours(imaginaryMatrix: Array<Array<Node>>, n: Node): List<Node> {
    val list = mutableListOf<Node>()


    if(n.y == 0 || n.y == imaginaryMatrix.lastIndex){
        if (n.x < imaginaryMatrix[n.y].lastIndex ) {
            imaginaryMatrix[n.y][n.x + 1].also {
                if(!it.processded)
                    list.add(it)
            }

        }
    }
    if(n.y < imaginaryMatrix.lastIndex){
       val s = if (n.imaginary) n.x else (n.x-1).coerceAtLeast(0)
        val e = if (n.imaginary) n.x else (n.x+1).coerceAtMost(imaginaryMatrix[n.y].lastIndex)
        (s .. e).forEach { c->
            imaginaryMatrix[n.y+1][c].also {
                if(!it.processded)
                    list.add(it)
            }
        }
//        list.add(imaginaryMatrix[n.y+1][n.x])
    }
//
//    if(n.y == imaginaryMatrix.lastIndex && n.x == imaginaryMatrix[0].lastIndex) listOf()
//    else{
//
//    }
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

fun BufferedImage.calculateEnergyMatrix(): Array<Array<Double>> {
    val result = Array(this.height) { Array(this.width) { 0.0 } }

    // inner central region
    for (y in 1 until this.height - 1) {
        for (x in 1 until this.width - 1) {
            result[y][x] = sqrt(deltaX(x, y) + deltaY(x, y))
        }
    }


    for (y in 1 until this.height - 1) {
        //left border
        result[y][0] = sqrt(deltaX(1, y) + deltaY(0, y))
        //right border
        result[y][width - 1] = sqrt(deltaX(width - 2, y) + deltaY(width - 1, y))

    }

    for (x in 1 until this.width - 1) {
        //top border
        result[0][x] = sqrt(deltaX(x, 0) + deltaY(x, 1))
        //bottom border
        result[height - 1][x] = sqrt(deltaX(x, height - 1) + deltaY(x, height - 2))

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