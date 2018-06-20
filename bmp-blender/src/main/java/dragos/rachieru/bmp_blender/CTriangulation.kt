package dragos.rachieru.bmp_blender

import android.graphics.Bitmap
import android.graphics.Point

import java.util.ArrayDeque
import java.util.HashSet
import java.util.Queue
import java.util.Vector

import dragos.rachieru.bmp_blender.utils.*

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
 * Class: CTriangulation.
 * <br></br>
 * License: GPLv2.
 * <br></br>
 * Description: Groups a set of points within one picture to a
 * DELAUNAY triangulation.
 * <br></br>
 * Hint: Not more than 500 points expected.
 * @version 1.5
 * <br></br>
 */
class CTriangulation(private val leftBitmap: Bitmap,
                     private val rightBitmap: Bitmap,
                     private val leftMesh: Vector<Point>,
                     private val rightMesh: Vector<Point>) {
    private var width: Int = 0
    private var height: Int = 0
    /**
     * Collection of all input points, order not modified.
     */
    private val order = Vector<Point>()
    /**
     * Collection of all unique input points. No point occurs twice.
     * Order not original.
     */
    private val points = HashSet<Point>()
    /**
     * Collection of lines which have already been processed.
     */
    private val used = HashSet<CLine>()
    /**
     * Queue of explored points which have to be processed.
     */
    private val queue = ArrayDeque<Array<Point>>()
    /**
     * Result of the process is this triangulation.
     */
    private val triangles = HashSet<CTriangle>()
    /**
     * Due to DELAUNAY. Circumcircle of the triangle to test.
     */
    private var circle_radius: Double = 0.toDouble()
    /**
     * Center x of the circumcircle of the triangle to test.
     */
    private var center_x: Double = 0.toDouble()
    /**
     * Center y of the circumcircle of the triangle to test.
     */
    private var center_y: Double = 0.toDouble()
    /**
     * First point of the line to explore.
     */
    private var p1: Point? = null
    /**
     * Second point of the line to explore.
     */
    private var p2: Point? = null
    /**
     * Point to test relating to the line: No other point shall lay nearer.
     */
    private var pnew: Point? = null
    /**
     * Count of found new point relating to the line. Max. two points
     * can be found.
     */
    private var count: Int = 0

    private val leftTriangles = Vector<CTriangle>()
    private val rightTriangles = Vector<CTriangle>()
    private val resultTriangles = Vector<CTriangle>()

    fun start(listener: OnProgressListener?, numOfSteps: Int): Bitmap {
        triangulate()
        val morphOperator = CMorphOperator(listener,
                numOfSteps,
                leftBitmap,
                rightBitmap,
                leftTriangles,
                rightTriangles,
                resultTriangles)
        return morphOperator.morph()
    }

    /**
     * Perform complete operation.
     */
    fun triangulate() {
        val time = System.currentTimeMillis()
        println("Begin triangulation.")
        var p: Point
        var l: Point
        var r: Point
        clear()
        /* Bug fix "Array index out of bounds" in CTriangulation.add().*/
        order.clear()
        points.clear()
        width = leftBitmap.width + rightBitmap.width
        height = leftBitmap.height + rightBitmap.height
        width /= 2
        height /= 2
        for (i in leftMesh.indices) {
            l = leftMesh[i]
            r = rightMesh[i]
            p = Point((l.x + r.x) / 2, (l.y + r.y) / 2)
            points.add(p)
            order.add(p)
        }
        work()
        debug()
        println("End. Duration of triangulation = " + (System.currentTimeMillis() - time) + '.'.toString())
    }

    /**
     * Clear all permanent date of the collections.
     */
    private fun clear() {
        used.clear()
        triangles.clear()
        queue.clear()
        leftTriangles.clear()
        rightTriangles.clear()
        resultTriangles.clear()
    }

    /**
     * Calculate the triangles.
     */
    private fun work() {
        clear()
        if (3 > points.size) return
        val it = points.iterator()
        p1 = it.next()
        p2 = findNearest(p1)
        used.add(CLine(p1!!, p2!!))
        queue.add(arrayOf<Point>(p1!!, p2!!))
        while (0 < queue.size) findPoint()
    }

    /**
     * Explore the third points for one line.
     */
    private fun findPoint() {
        val a = queue.poll()
        p1 = a[0]
        p2 = a[1]
        count = 0
        for (p in points) {
            pnew = p
            if (circle()) {
                if (delaunayCond()) {
                    add(CTriangle(p1!!, p2!!, pnew!!))
                    if (used.add(CLine(p1!!, pnew!!))) {
                        queue.add(arrayOf<Point>(p1!!, pnew!!))
                    }
                    if (used.add(CLine(p2!!, pnew!!))) {
                        queue.add(arrayOf<Point>(p2!!, pnew!!))
                    }
                    if (1 == count++) {
                        return
                    }
                }
            }
        }
    }

    /**
     * Add one left & one right triangle.
     * Points are fetched ordered from left & right mesh them self.
     *
     * @param temp Input triangle.
     */
    private fun add(temp: CTriangle) {
        if (triangles.add(temp)) {
            val l0 = leftMesh[indexOf(temp.points[0])]
            val l1 = leftMesh[indexOf(temp.points[1])]
            val l2 = leftMesh[indexOf(temp.points[2])]
            val r0 = rightMesh[indexOf(temp.points[0])]
            val r1 = rightMesh[indexOf(temp.points[1])]
            val r2 = rightMesh[indexOf(temp.points[2])]
            leftTriangles.add(CTriangle(l0, l1, l2))
            rightTriangles.add(CTriangle(r0, r1, r2))
        }
    }

    /**
     * Seek one point within the ordered input list.
     *
     * @param p Point to seek.
     * @return Index of the point in the ordered input list.
     */
    private fun indexOf(p: Point): Int {
        return order.indexOf(p)
    }

    /**
     * Check the DELAUNAY condition of P1 P2 and PNEW.
     * No other point shall be within the circumcircle of the three points.
     *
     * @return `true` if DELAUNAY condition is satisfied.
    `` */
    private fun delaunayCond(): Boolean {
        val l1 = CLine(p1!!, pnew!!)
        val l2 = CLine(p2!!, pnew!!)
        for (p in points) {
            val d = distance(p)
            if (p != pnew && p != p1 && p != p2) {
                if (d < circle_radius) {
                    return false
                }
            }
        }
        for (l in used) {
            if (l.cross(l1) || l.cross(l2)) {
                return false
            }
        }
        return true
    }

    /**
     * Find the nearest neighbor of one mesh point.
     *
     * @param p1 First point.
     * @return Nearest point to p1.
     */
    private fun findNearest(p1: Point?): Point? {
        var dist = java.lang.Double.MAX_VALUE
        var d: Double
        var result: Point? = null
        for (p in points) {
            d = p.distance(p1!!)
            //            d = p.distance(p1);
            if (d < dist && d > 0.0) {
                dist = d
                result = p
            }
        }
        return result
    }

    /**
     * Calculates the circumcircle of the current test triangle's points.
     *
     * @return `true` if such a circle can be found.
     */
    private fun circle(): Boolean {
        val x1: Double
        val y1: Double
        val x2: Double
        val y2: Double
        val x3: Double
        val y3: Double
        var q: Double
        val n: Double
        x1 = (p1!!.x + pnew!!.x) / 2.0
        y1 = (p1!!.y + pnew!!.y) / 2.0
        x3 = (p2!!.x + pnew!!.x) / 2.0
        y3 = (p2!!.y + pnew!!.y) / 2.0
        x2 = pnew!!.x.toDouble()
        y2 = pnew!!.y.toDouble()
        q = (y2 - y1) * (y3 - y1) - (-x2 + x1) * (x3 - x1)
        n = (y2 - y3) * (-x2 + x1) - (-x2 + x3) * (y2 - y1)
        if (0.0 == n) {
            return false
        }
        q /= n
        center_x = x3 + q * (y2 - y3)
        center_y = y3 + q * (-x2 + x3)
        circle_radius = distance(p1!!)
        return true
    }

    /**
     * Distance of one point to the center of the circumcircle.
     *
     * @param p Point to be explored.
     * @return Distance.
     */
    private fun distance(p: Point): Double {
        val dx = p.x - center_x
        val dy = p.y - center_y
        return Math.sqrt(dx * dx + dy * dy)
    }

    /**
     * Write left right and 50% triangulation into the debug directory.
     */
    private fun debug() {
        var image: Bitmap
        try {
            image = Bitmap.createBitmap(leftBitmap.width,
                    leftBitmap.height,
                    Bitmap.Config.RGB_565)
            for (t in leftTriangles) {
                t.debug(image)
            }
            //            ImageIO.write(image, "png", new File(CStrings.LEFT_TRI)); fixme
            image = Bitmap.createBitmap(
                    rightBitmap.width,
                    rightBitmap.height,
                    Bitmap.Config.RGB_565
            )
            for (t in rightTriangles) {
                t.debug(image)
            }
            //            ImageIO.write(image, "png", new File(CStrings.RIGHT_TRI)); fixme

            image = Bitmap.createBitmap(
                    width,
                    height,
                    Bitmap.Config.RGB_565
            )
            for (t in triangles) {
                t.debug(image)
            }
            //            ImageIO.write(image, "png", new File(CStrings.MIDDLE_TRI)); fixme
        } catch (e: Exception) {
            println("Can't debug triangles.")
            e.printStackTrace()
        }

    }
}
