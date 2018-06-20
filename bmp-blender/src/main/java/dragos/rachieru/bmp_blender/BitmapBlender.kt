package dragos.rachieru.bmp_blender

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.util.Log
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.face.FaceDetector
import io.reactivex.Maybe
import java.io.Closeable
import java.util.*
import android.util.SparseArray
import com.google.android.gms.vision.face.Face


class BitmapBlender(val context: Context,
                    var leftBitmap: Bitmap,
                    var rightBitmap: Bitmap,
                    var numOfSteps: Int = 10) : Closeable {
    private val detector: FaceDetector

    var listener: OnProgressListener? = null

    init {
        detector = FaceDetector.Builder(context)
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .build()
    }

    fun scanFaces(): Maybe<Bitmap> {
        return Maybe.create { emitter ->
            var frame = Frame.Builder().setBitmap(leftBitmap).build()
            var faces = detector.detect(frame)
            if(faces.size() ==0) {
                emitter.onComplete()
                return@create
            }
            val leftMesh = createMesh(faces)
            frame = Frame.Builder().setBitmap(leftBitmap).build()
            faces = detector.detect(frame)
            if(faces.size() ==0) {
                emitter.onComplete()
                return@create
            }
            val rightMesh = createMesh(faces)
            val bitmap = blend(leftMesh, rightMesh)
            emitter.onSuccess(bitmap)
        }
    }

    fun createMesh(faces: SparseArray<Face>): Vector<Point> {
        val mesh = Vector<Point>()
        for (i in 0..faces.size()) {
            val point = faces.valueAt(i).position
            mesh.add(Point(point.x.toInt(), point.y.toInt()))
        }
        return mesh
    }

    fun blend(leftMesh: Vector<Point>, rightMesh: Vector<Point>): Bitmap {
        var time = System.currentTimeMillis()
        val result = CTriangulation(leftBitmap, rightBitmap, leftMesh, rightMesh).start(listener, numOfSteps);

        /* Calculate duration. */
        time = System.currentTimeMillis() - time
        time /= 1000
        /* Print duration to the console. */
        Log.d("BitmapBlender", "Duration = $time seconds.")
        return result;
    }

    override fun close() {
        detector.release()
    }
}