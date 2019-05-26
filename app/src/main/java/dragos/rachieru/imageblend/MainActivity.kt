package dragos.rachieru.imageblend

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import dragos.rachieru.bmp_blender.BitmapBlender
import dragos.rachieru.imageblend.download.DownloadActivity
import io.reactivex.CompletableObserver
import io.reactivex.MaybeObserver
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.util.*

class MainActivity : AppCompatActivity(), CompletableObserver {

    private var disposable: Disposable? = null

    override fun onSubscribe(d: Disposable) {
        disposable = d
    }

    override fun onError(e: Throwable) {
        Toast.makeText(this, e.message!!, Toast.LENGTH_SHORT).show()
    }

    private lateinit var left: CropImage.ActivityResult
    private lateinit var right: CropImage.ActivityResult

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (!getFileStreamPath(Constants.datFileName).exists()) {
            startActivity(Intent(this, DownloadActivity::class.java))
            finish()
        } else {
            image_left.setOnClickListener {
                startActivityForResult(CropImage.activity()
                        .setRequestedSize(500, 500)
                        .setAspectRatio(1, 1)
                        .setFixAspectRatio(true)
                        .getIntent(this), 112
                )
            }
            image_right.setOnClickListener {
                startActivityForResult(CropImage.activity()
                        .setRequestedSize(500, 500, CropImageView.RequestSizeOptions.RESIZE_EXACT)
                        .setAspectRatio(1, 1)
                        .setFixAspectRatio(true)
                        .getIntent(this), 113
                )
            }
            BitmapFactory.decodeResource(resources, R.drawable.right)
            btn_start.setOnClickListener {
                onComplete()
            }
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 112 || requestCode == 113) {
            val result = CropImage.getActivityResult(data)
            if (resultCode == Activity.RESULT_OK) {
                if (requestCode == 112) {
                    left = result
                    Glide.with(this)
                            .load(result.uri)
                            .into(image_left)
                } else {
                    right = result
                    Glide.with(this)
                            .load(result.uri)
                            .into(image_right)
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                onError(result.error)
            }
        }
    }

    override fun onComplete() {
        val blender = BitmapBlender(
                this,
                MediaStore.Images.Media.getBitmap(contentResolver, left.uri),
                MediaStore.Images.Media.getBitmap(contentResolver, right.uri)
        )
        blender.init(getFileStreamPath(Constants.datFileName).path)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .andThen(Single.zip(
                        blender.scanLeft(), blender.scanRight(),
                        BiFunction<Vector<Point>, Vector<Point>, Pair<Vector<Point>, Vector<Point>>> { leftMesh, rightMesh ->
                            return@BiFunction leftMesh to rightMesh
                        }))
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterSuccess {
                    Toast.makeText(this, "Scanned images", Toast.LENGTH_SHORT).show();
                }
                .observeOn(Schedulers.io())
                .flatMap {
                    return@flatMap blender.debug(it.first, it.second)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : SingleObserver<Bitmap> {
                    override fun onSuccess(t: Bitmap) {
                        image_result.setImageBitmap(t)
                    }

                    override fun onSubscribe(d: Disposable) {

                    }

                    override fun onError(e: Throwable) {
                        Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
                        e.printStackTrace()
                    }

                })
    }
}
