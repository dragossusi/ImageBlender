package dragos.rachieru.bmp_blender

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.util.Log
import io.reactivex.Maybe
import java.io.Closeable
import com.tzutalin.dlib.FaceDet
import com.tzutalin.dlib.VisionDetRet
import io.reactivex.Completable
import java.util.Vector


class BitmapBlender(val context: Context,
                    var leftBitmap: Bitmap,
                    var rightBitmap: Bitmap,
                    var numOfSteps: Int = 10) : Closeable {
    private lateinit var detector: FaceDet

    var listener: OnProgressListener? = null

    fun init(datPath: String) = Completable.fromAction {
        detector = FaceDet(datPath)
    }

    fun blend(): Maybe<Bitmap> {
        return Maybe.fromCallable {
            var faces = detector.detect(leftBitmap)
            if (faces == null || faces.isEmpty())
                return@fromCallable null
            val leftMesh = createMesh(faces[0])
            faces = detector.detect(rightBitmap)
            if (faces == null || faces.isEmpty()) {
                return@fromCallable null
            }
            val rightMesh = createMesh(faces[0])
            val bitmap = morph(leftMesh, rightMesh)
            return@fromCallable bitmap
        }
    }

    private fun createMesh(face: VisionDetRet): Vector<Point> {
        val mesh = Vector<Point>()
        face.faceLandmarks.forEach { mesh.add(it) }
        return mesh
    }

    private fun morph(leftMesh: Vector<Point>, rightMesh: Vector<Point>): Bitmap {
        var time = System.currentTimeMillis()
        val result = CTriangulation(leftBitmap, rightBitmap, leftMesh, rightMesh).start(listener, numOfSteps);

        /* Calculate duration. */
        time = System.currentTimeMillis() - time
        time /= 1000
        /* Print duration to the console. */
        Log.d("BitmapBlenderGoogle", "Duration = $time seconds.")
        return result;
    }

    override fun close() {
        detector.release()
    }
}