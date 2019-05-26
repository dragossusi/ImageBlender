package dragos.rachieru.bmp_blender

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import androidx.appcompat.app.AlertDialog
import android.util.Log
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.face.FaceDetector
import io.reactivex.Maybe
import java.io.Closeable
import java.util.*
import android.util.SparseArray
import com.google.android.gms.vision.face.Face


class BitmapBlenderGoogle(val context: Context,
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
        if (!detector.isOperational()) {
            AlertDialog.Builder(context).setMessage("Could not set up the face detector!").show()
        }
    }

    fun blend(): Maybe<Bitmap> {
        return Maybe.create { emitter ->
            if(detector.isOperational) {
                var frame = Frame.Builder().setBitmap(leftBitmap).build()
                var faces = detector.detect(frame)
                if (faces.size() == 0) {
                    emitter.onComplete()
                    return@create
                }
                val leftMesh = createMesh(faces.valueAt(0))
                frame = Frame.Builder().setBitmap(leftBitmap).build()
                faces = detector.detect(frame)
                if (faces.size() == 0) {
                    emitter.onComplete()
                    return@create
                }
                val rightMesh = createMesh(faces.valueAt(0))
                val bitmap = morph(leftMesh, rightMesh)
                emitter.onSuccess(bitmap)
            } else
                emitter.onError(Throwable("nu merge detectorul!"))
        }
    }

    private fun createMesh(face: Face): Vector<Point> {
        val mesh = Vector<Point>()
        for (lm in face.landmarks) {
            val point = lm.position
            mesh.add(Point(point.x.toInt(), point.y.toInt()))
        }
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