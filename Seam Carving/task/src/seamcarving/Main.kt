package seamcarving

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

fun main() {
    print("Enter rectangle width:")
    val width  = readln().toInt()

    print("Enter rectangle height:")
    val height  =readln().toInt()

    print("Enter output image name:")
    val filename  = readln()

    val img = BufferedImage(width,height,BufferedImage.TYPE_INT_RGB)
    val graphics = img.graphics
    graphics.color = Color.RED
//    graphics.drawRect(0,0,width-1,height-1)
    graphics.drawLine(0,0,width-1,height-1)
    graphics.drawLine(width-1,0,0,height-1)
    ImageIO.write(img,"png", File(filename))

}
