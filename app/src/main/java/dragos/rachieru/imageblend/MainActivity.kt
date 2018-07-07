package dragos.rachieru.imageblend

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import dragos.rachieru.bmp_blender.BitmapBlender
import dragos.rachieru.bmp_blender.BitmapBlenderGoogle
import io.reactivex.Completable
import io.reactivex.CompletableObserver
import io.reactivex.MaybeObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

//import dragos.rachieru.imageblend.R

class MainActivity : AppCompatActivity(), CompletableObserver {

    private var disposable: Disposable? = null

    override fun onSubscribe(d: Disposable) {
        disposable = d
    }

    override fun onError(e: Throwable) {
        Toast.makeText(this, e.message!!, Toast.LENGTH_SHORT).show()
    }

    private lateinit var left: Bitmap
    private lateinit var right: Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        left = BitmapFactory.decodeResource(resources, R.drawable.left2)
        right = BitmapFactory.decodeResource(resources, R.drawable.right)
        image_left.setImageBitmap(left)
        image_right.setImageBitmap(right)
        BitmapFactory.decodeResource(resources, R.drawable.right)
        checkDat()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this)
    }

    fun checkDat() = Completable.fromAction {
        if (!File(Constants.faceShapeModelPath).exists())
            Constants.copyRawToSdcard(this, R.raw.shape_predictor_68_face_landmarks, Constants.faceShapeModelPath)
    }

    override fun onComplete() {
        val blender = BitmapBlender(this, left, right)
        blender.init(Constants.faceShapeModelPath)
                .andThen(blender.blend())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : MaybeObserver<Bitmap> {
                    override fun onSuccess(t: Bitmap) {
                        image_result.setImageBitmap(t)
                    }

                    override fun onComplete() {
                        Toast.makeText(this@MainActivity, "No face detected:(", Toast.LENGTH_LONG).show()
                    }

                    override fun onSubscribe(d: Disposable) {

                    }

                    override fun onError(e: Throwable) {
                        Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
                    }

                })
    }
}
