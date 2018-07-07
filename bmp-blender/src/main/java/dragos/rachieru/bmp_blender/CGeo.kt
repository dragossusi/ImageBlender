package dragos.rachieru.bmp_blender

import android.graphics.Point

/**
 * File belongs to javamorph (Merging of human-face-pictures).
 * Copyright (C) 2009 - 2010  Claus Wimmer
 * See file ".../help/COPYING" for details!
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA *
 *
 * @version 1.5
 * <br></br>
 * @author claus.erhard.wimmer@googlemail.com
 * <br></br>
 * Program: JavaMorph.
 * <br></br>
 * Class: CGeo.
 * <br></br>
 * License: GPLv2.
 * <br></br>
 * Description: Helper class for geometric calculations.
 * <br></br>
 * Handle interpolation - transformation.
 * Triangulation now done by CTriangulation.
 */
object CGeo {
    /**
     * Provide reverse transformation matrix from one result triangle to one
     * input picture triangle. All three points of the two triangles
     * correspond.
     * @param origin_ One triangle of the source picture.
     * @param result One triangle of the result picture.
     * @return Floating point transformation matrix. Indexes are row / column.
     */
    fun getTrafo(origin_: CTriangle, result: CTriangle): CTransform {
        /* Initialize helper variables. */
        val x1_ = origin_.points[0].x
        val x2_ = origin_.points[1].x
        val x3_ = origin_.points[2].x
        val y1_ = origin_.points[0].y
        val y2_ = origin_.points[1].y
        val y3_ = origin_.points[2].y
        val x1 = result.points[0].x
        val x2 = result.points[1].x
        val x3 = result.points[2].x
        val y1 = result.points[0].y
        val y2 = result.points[1].y
        val y3 = result.points[2].y
        val trafo = CTransform()
        val t: Double
        val u: Double
        val d: Double
        /* Try variants depending on which points are orthogonal. */
        if (x1 != x3) {
            d = x1.toDouble() - x3
            t = (x1.toDouble() - x2) / d
            u = y1.toDouble() - y2 - (y1.toDouble() - y3) * t
            trafo.a_12 = (x1_.toDouble() - x2_ - (x1_.toDouble() - x3_) * t) / u
            trafo.a_22 = (y1_.toDouble() - y2_ - (y1_.toDouble() - y3_) * t) / u
            trafo.a_11 = (x1_.toDouble() - x3_ - trafo.a_12 * (y1.toDouble() - y3)) / d
            trafo.a_21 = (y1_.toDouble() - y3_ - trafo.a_22 * (y1.toDouble() - y3)) / d
        } else {
            d = y1.toDouble() - y3
            t = (y1.toDouble() - y2) / d
            u = x1.toDouble() - x2 - (x1.toDouble() - x3) * t
            trafo.a_11 = (x1_.toDouble() - x2_ - (x1_.toDouble() - x3_) * t) / u
            trafo.a_21 = (y1_.toDouble() - y2_ - (y1_.toDouble() - y3_) * t) / u
            trafo.a_12 = (x1_.toDouble() - x3_ - trafo.a_11 * (x1.toDouble() - x3)) / d
            trafo.a_22 = (y1_.toDouble() - y3_ - trafo.a_21 * (x1.toDouble() - x3)) / d
        }
        trafo.a_13 = x1_.toDouble() - trafo.a_11 * x1 - trafo.a_12 * y1
        trafo.a_23 = y1_.toDouble() - trafo.a_21 * x1 - trafo.a_22 * y1
        return trafo
    }

    /**
     * Transform one point from the result matrix to the one point of the input
     * matrix.
     * @param result Point of the result picture.
     * @param trafo Transformation matrix.
     * @return Corresponding point of the input picture.
     */
    fun getOrigin_(result: Point, trafo: CTransform): Point {
        val origin_ = Point()
        /* Transform x. */
        origin_.x = (result.x * trafo.a_11 + result.y * trafo.a_12 + trafo.a_13).toInt()
        /* Transform y. */
        origin_.y = (result.x * trafo.a_21 + result.y * trafo.a_22 + trafo.a_23).toInt()
        return origin_
    }
}
