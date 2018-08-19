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
 * Class: CLine.
 * <br></br>
 * License: GPLv2.
 * <br></br>
 * Description: Line on a pixel picture.
 * <br></br>
 * Hint: Consisting of two points. Evaluation helper functions provided.
 */

class CLine {
    /** x of first point.  */
    var x1: Int = 0
    /** x of second point.  */
    var x2: Int = 0
    /** y of first point.  */
    var y1: Int = 0
    /** y of second point.  */
    var y2: Int = 0

    /**
     * Constructor with numbers.
     *
     * @param x1 X of first point.
     * @param y1 Y of first point.
     * @param x2 X of second point.
     * @param y2 Y of second point.
     */
    constructor(x1: Int, y1: Int, x2: Int, y2: Int) {
        /* Assign arguments. */
        this.x1 = x1
        this.x2 = x2
        this.y1 = y1
        this.y2 = y2
    }

    /**
     * Constructor with two points.
     *
     * @param p1 Complete first point.
     * @param p2 Complete second point.
     */
    constructor(p1: Point, p2: Point) {
        /* Assign arguments. */
        this.x1 = p1.x
        this.y1 = p1.y
        this.x2 = p2.x
        this.y2 = p2.y
    }

    /**
     * Does one line cross the other?
     * Hint: Except endpoints.
     *
     * @param other The other line.
     * @return `true` if crossing.
     */
    fun cross(other: CLine): Boolean {
        /* Initialize helper variables. */
        val x1 = this.x1.toDouble()
        val y1 = this.y1.toDouble()
        val x1_ = this.x2.toDouble()
        val y1_ = this.y2.toDouble()
        val x2 = other.x1.toDouble()
        val y2 = other.y1.toDouble()
        val x2_ = other.x2.toDouble()
        val y2_ = other.y2.toDouble()
        val z: Double
        val n: Double
        val q: Double
        val p: Double
        /* Divident. */
        z = (x1_ - x1) * (y2 - y1) - (y1_ - y1) * (x2 - x1)
        /* Divisor. */
        n = (y1_ - y1) * (x2_ - x2) - (x1_ - x1) * (y2_ - y2)
        /* Is parallel? => Can't cross! */
        if (0.0 == n) return false
        /* Quotient q. */
        q = z / n
        /* Is len zero? */
        if (x1 == x1_ && y1 == y1_) return false
        /* Variants depending on whether points are orthogonal. */
        if (0.0 != x1_ - x1) {
            p = (x2 - x1 + q * (x2_ - x2)) / (x1_ - x1)
        } else {
            p = (y2 - y1 + q * (y2_ - y2)) / (y1_ - y1)
        }
        /* Crossing after the end of the line. */
        if (q < 0.0 || q > 1.0) return false
        /* Crossing after the end of the line. */
        if (p < 0.0 || p > 1.0) return false
        /* Determine whether crossing on end point. */
        val p1 = Point(this.x1, this.y1)
        val p2 = Point(this.x2, this.y2)
        val p3 = Point(other.x1, other.y1)
        val p4 = Point(other.x2, other.y2)
        /* Determine whether crossing on end point. */
        if (p1 == p3 || p1 == p4) return false
        /* Determine whether crossing on end point. */
        return if (p2 == p3 || p2 == p4) false else true
        /* No false condition fired. */
    }

    /**
     * Both end points are identical.
     */
    override fun equals(other: Any?): Boolean {
        if (other !is CLine)
            return super.equals(other)
        else {
            val cLine = other as CLine?
            return x1 == cLine!!.x1 &&
                    x2 == cLine.x2 &&
                    y1 == cLine.y1 &&
                    y2 == cLine.y2
        }
    }

    /**
     * Hash code relating to `equals`.
     */
    override fun hashCode(): Int {
        return (x1 + x2 + y1 + y2) % Integer.MAX_VALUE
    }
}
