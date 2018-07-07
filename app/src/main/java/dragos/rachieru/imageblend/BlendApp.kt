package dragos.rachieru.imageblend

import android.app.Application
import java.io.File

class BlendApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val file = File(Constants.faceShapeModelPath)
        if (!file.exists())
            file.mkdirs()
    }
}
