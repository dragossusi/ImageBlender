package dragos.rachieru.bmp_blender.utils

import android.graphics.Point

fun Point.distance(p2: Point): Double {
    return Math.hypot((x - p2.x).toDouble(), (y - p2.y).toDouble())
}