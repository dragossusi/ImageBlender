package dragos.rachieru.imageblend

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import dragos.rachieru.bmp_blender.BitmapBlender
import io.reactivex.MaybeObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val left = BitmapFactory.decodeResource(resources, R.drawable.left2)
        val right = BitmapFactory.decodeResource(resources, R.drawable.right)
        image_left.setImageBitmap(left)
        image_right.setImageBitmap(right)
        BitmapFactory.decodeResource(resources, R.drawable.right)
        val blender = BitmapBlender(this, left, right)
        blender.blend()
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
