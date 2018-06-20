package dragos.rachieru.bmp_blender

import android.graphics.Bitmap
import android.graphics.Point
import dragos.rachieru.bmp_blender.old.CStrings
import java.io.File
import java.util.*

/**
 * File belongs to javamorph (Merging of human-face-pictures).
 * Copyright (C) 2009 - 2010  Claus Wimmer
 * See file ".../help/COPYING" for details!
 *
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA *
 *
 * @author claus.erhard.wimmer@googlemail.com
 * <br></br>
 * Program: JavaMorph.
 * <br></br>
 * Class: CMorphOperator.
 * <br></br>
 * License: GPLv2.
 * <br></br>
 * Description: Morph the result from left input to right input depending on
 * the ratio parameter.
 * <br></br>
 * Hint: Writes the result into the working directory.
 * @version 1.5
 * <br></br>
 */
class CMorphOperator(private val listener: OnProgressListener?,
                     private val numOfSteps: Int,
                     internal var leftBitmap: Bitmap,
                     internal var rightBitmap: Bitmap,
                     /**
                      * Instance of the progress bar.
                      */

                     val leftTriangles: Vector<CTriangle>,
                     val rightTriangles: Vector<CTriangle>,
                     val resultTriangles: Vector<CTriangle>) {
    private val resultImage: Bitmap
    private val leftClip: Array<DoubleArray>
    private val rightClip: Array<DoubleArray>
    /**
     * If `0.0` `1.0`
     * then output is the right image. Every value between them leads to a
     * merged image.
     */
    private var ratio: Double = 0.toDouble()
    /**
     * Current point coordinates of the left image.
     */
    private var left_point = Point()
    /**
     * Current point coordinates of the right image.
     */
    private var right_point = Point()
    /**
     * Current point coordinates of the result image.
     */
    private var result_point = Point()
    /**
     * RGB value of the current left pixel.
     */
    private var left_pixel: Int = 0
    /**
     * RGB value of the current right pixel.
     */
    private var right_pixel: Int = 0
    /**
     * RGB value of the current result pixel.
     */
    private var result_pixel: Int = 0
    /**
     * Transformation matrix from result to left point.
     */
    private var left_trafo: CTransform? = null
    /**
     * Transformation matrix from result to right point.
     */
    private var right_trafo: CTransform? = null
    /**
     * Index of the current triangle within all three lists.
     */
    private var t_idx: Int = 0
    /**
     * List of result points situated within the current result triangle.
     */
    private var withins: Array<Point>? = null
    /**
     * Polygon clip ratio of the current left pixel.
     */
    private var left_ratio: Double = 0.toDouble()
    /**
     * Polygon clip ratio of the current right pixel.
     */
    private var right_ratio: Double = 0.toDouble()
    /**
     * If `true` the user forces the morph process to abort.
     */
    private var f_break: Boolean = false

    init {
        val w = Math.max(leftBitmap.width, rightBitmap.width)
        val h = Math.max(leftBitmap.height, rightBitmap.height)
        resultImage = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)

        leftClip = Array(leftBitmap.width) { DoubleArray(leftBitmap.height) }
        rightClip = Array(rightBitmap.width) { DoubleArray(rightBitmap.height) }
    }

    /**
     * Enable abort of the morph process forced by user.
     */
    fun doBreak() {
        f_break = true
    }

    /**
     * Thread API. Starts morph batch for a number of intermediate pictures
     * with increasing ratio value.
     */
    fun morph(): Bitmap {
        f_break = false
        var i = 0
        while (i <= numOfSteps && !f_break) {
            /* Clear result picture.*/
            for (x in 0 until resultImage.width) {
                for (y in 0 until resultImage.height) {
                    resultImage.setPixel(x, y, 0x0)
                }
            }
            /* Calculate ratio. */
            ratio = i.toDouble() / numOfSteps
            /* Depends on current ratio. */
            genResultTriangles()
            /* Iterate through the triangles. */
            t_idx = 0
            while (t_idx < resultTriangles.size) {
                triangle()
                ++t_idx
            }
            val f = File(CStrings.getOutput(i))
            /* Save image into workdir. */
            //            ImageIO.write(resultImage, "jpg", f); fixme
            /* Show progress. */
            listener?.onProgress(i, 0, numOfSteps)
            ++i
        }
        return resultImage
    }

    /**
     * Make a weighted average mesh depending on the current ratio.
     */
    private fun genResultTriangles() {
        resultTriangles.clear()
        /* For all triangles belonging to both pictures. */
        /* First with first, second with second and so on. */
        for (i in leftTriangles.indices) {
            val r = leftTriangles[i]
            val s = rightTriangles[i]
            val t = CTriangle(
                    merge(r.points[0], s.points[0]),
                    merge(r.points[1], s.points[1]),
                    merge(r.points[2], s.points[2])
            )
            /* Add merged triangle relating to ratio. */
            resultTriangles.add(t)
        }
    }

    /**
     * Merge two points weighted by ratio.
     *
     * @param p1 First point.
     * @param p2 Second point.
     * @return Point on a line between them.
     */
    private fun merge(p1: Point, p2: Point): Point {
        return Point(
                (p1.x * (1.0 - ratio) + p2.x * ratio).toInt(),
                (p1.y * (1.0 - ratio) + p2.y * ratio).toInt())
    }

    /**
     * Merge all points of a triangle.
     */
    private fun triangle() {
        val result = resultTriangles[t_idx]
        /* Left transformation matrix. */
        left_trafo = CGeo.getTrafo(leftTriangles[t_idx], result)
        /* Right transformation matrix. */
        right_trafo = CGeo.getTrafo(rightTriangles[t_idx], result)
        /* For all target points. */
        withins = result.withins
        for (p in withins!!) {
            result_point = p
            /* Transform left. */
            left_point = CGeo.getOrigin_(result_point, left_trafo!!)
            /* Transform right. */
            right_point = CGeo.getOrigin_(result_point, right_trafo!!)
            /* Merge both pixels. */
            merge()
        }
    }

    /**
     * Merge (left.pixel, right.pixel)->(result.pixel). Result depends on
     * ratio value & both polygon matrixes.
     */
    private fun merge() {
        left_pixel = leftBitmap.getPixel(left_point.x, left_point.y)
        right_pixel = rightBitmap.getPixel(right_point.x, right_point.y)
        left_ratio = leftClip[left_point.x][left_point.y]
        right_ratio = rightClip[right_point.x][right_point.y]
        /* Unify all 3 ratios. */
        val t1 = left_ratio
        val t2 = 1.0 - left_ratio
        val t3 = 1.0 - right_ratio
        val t4 = right_ratio
        val fl = t3 + (1.0 - ratio) * (t1 - t3)
        val fr = t2 + ratio * (t4 - t2)
        /* For each color in 32 bit color value. */
        val l_r = left_pixel and -0x10000 shr 16
        val r_r = right_pixel and -0x10000 shr 16
        val l_g = left_pixel and -0xff0100 shr 8
        val r_g = right_pixel and -0xff0100 shr 8
        val l_b = left_pixel and -0xffff01
        val r_b = right_pixel and -0xffff01
        val r = (l_r * fl + r_r * fr).toInt()
        val g = (l_g * fl + r_g * fr).toInt()
        val b = (l_b * fl + r_b * fr).toInt()
        /* Set pixel. */
        result_pixel = -0x1000000 or (r shl 16) or (g shl 8) or b
        resultImage.setPixel(result_point.x, result_point.y, result_pixel)
    }
}
