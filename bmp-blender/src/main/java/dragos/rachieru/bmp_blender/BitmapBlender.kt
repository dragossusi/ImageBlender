package dragos.rachieru.bmp_blender

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.util.Log
import com.tzutalin.dlib.FaceDet
import com.tzutalin.dlib.VisionDetRet
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import java.io.Closeable
import java.util.*


class BitmapBlender(val context: Context,
                    var leftBitmap: Bitmap,
                    var rightBitmap: Bitmap,
                    var numOfSteps: Int = 10) : Closeable {
    private lateinit var detector: FaceDet

    var listener: OnProgressListener? = null

    fun init(datPath: String) = Completable.fromAction {
        detector = FaceDet(datPath)
    }

    fun blend(leftMesh: Vector<Point>, rightMesh: Vector<Point>): Maybe<Bitmap> {
        return Maybe.fromCallable {
            val bitmap = morph(leftMesh, rightMesh)
            return@fromCallable bitmap
        }
    }

    fun debug(leftMesh: Vector<Point>, rightMesh: Vector<Point>): Single<Bitmap> {
        return Single.fromCallable {
            return@fromCallable triangulate(leftMesh, rightMesh)
        }
    }

    fun scanLeft(): Single<Vector<Point>> = Single.fromCallable {
        val faces = detector.detect(leftBitmap)
        if (faces.isNullOrEmpty())
            throw Exception("Can't find face in left image")
        createMesh(faces[0])
    }

    fun scanRight(): Single<Vector<Point>> = Single.fromCallable {
        val faces = detector.detect(rightBitmap)
        if (faces.isNullOrEmpty())
            throw Exception("Can't find face in right image")
        createMesh(faces[0])
    }

    private fun createMesh(face: VisionDetRet): Vector<Point> {
        val mesh = Vector<Point>()
        face.faceLandmarks.forEach { mesh.add(it) }
        mesh.add(Point(0, 0))
        mesh.add(Point(0, 500))
        mesh.add(Point(500, 0))
        mesh.add(Point(500, 500))
        for (i in 0..mesh.size - 2) {
            for (j in i + 1..mesh.size - 2) {
                if (mesh[i].y < mesh[j].y || (mesh[i].y == mesh[j].y && mesh[i].x < mesh[j].x)) {
                    val aux = mesh[i]
                    mesh[i] = mesh[j]
                    mesh[j] = aux
                }
            }
        }
        return mesh
    }

    private fun triangulate(leftMesh: Vector<Point>, rightMesh: Vector<Point>): Bitmap {
        return CTriangulation(leftBitmap, rightBitmap, leftMesh, rightMesh).debug()
    }

    private fun morph(leftMesh: Vector<Point>, rightMesh: Vector<Point>): Bitmap {
        var time = System.currentTimeMillis()
        val result = CTriangulation(leftBitmap, rightBitmap, leftMesh, rightMesh).start(listener, numOfSteps)

        /* Calculate duration. */
        time = System.currentTimeMillis() - time
        time /= 1000
        /* Print duration to the console. */
        Log.d("BitmapBlender", "Duration = $time seconds.")
        return result
    }

    override fun close() {
        detector.release()
    }
}