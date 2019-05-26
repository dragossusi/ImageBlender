package dragos.rachieru.imageblend

import android.content.Context
import android.os.Environment

import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object Constants {
    @Throws(IOException::class)
    fun copyRawToSdcard(context: Context, id: Int, path: String) {
        val inp = context.resources.openRawResource(id)
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

    val datFileName: String = "shape_predictor_68_face_landmarks.dat"

    @Deprecated("dont use", level = DeprecationLevel.ERROR, replaceWith = ReplaceWith("datFileName", "dragos.rachieru.imageblend.Constants.datFileName"))
    val faceShapeModelPath: String
        get() {
            return appDir + File.separator + datFileName
        }

    val appDir: String
        get() = Environment.getExternalStorageDirectory().absolutePath + File.separator + "image_morph"
}
