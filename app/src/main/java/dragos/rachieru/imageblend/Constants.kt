package dragos.rachieru.imageblend

import android.content.Context
import android.os.Environment

import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object Constants {
    @Throws(IOException::class)
    fun copyRawToSdcard(context: Context, id: Int, path: String) {
        val inp = context.getResources().openRawResource(id)
        val out = FileOutputStream(path)
        val buff = ByteArray(1024)
        var read: Int
        try {
            read = inp.read(buff)
            while (read > 0) {
                out.write(buff, 0, read)
                read = inp.read(buff)
            }
        } finally {
            inp.close()
            out.close()
        }
    }

    val faceShapeModelPath: String
        get() {
            return appDir + File.separator + "shape_predictor_68_face_landmarks.dat"
        }

    val appDir: String
        get() = Environment.getExternalStorageDirectory().absolutePath + File.separator + "image_morph"
}
