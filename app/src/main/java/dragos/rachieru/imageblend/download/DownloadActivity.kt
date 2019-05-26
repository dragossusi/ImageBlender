package dragos.rachieru.imageblend.download

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import dragos.rachieru.imageblend.*
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_download.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Okio
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit


/**
 * ImageBlender
 *
 * @author Dragos
 * @since 26.05.2019
 */

class DownloadActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download)
        btn_download.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            progressBar.progress = 0
            downloadFile("https://github.com/AKSHAYUBHAT/TensorFace/blob/master/openface/models/dlib/shape_predictor_68_face_landmarks.dat?raw=true",
                    openFileOutput(Constants.datFileName, Context.MODE_PRIVATE))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        progressBar.setProgress(it)
                    }, {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                        it.printStackTrace()
                    }, {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    })
        }
    }

    fun downloadFile(url: String, outputStream: FileOutputStream): Observable<Int> {
        return Observable.create<Int> { emitter ->
            val request = Request.Builder()
                    .url(url)
                    .build()

            val progressListener: ProgressListener = { bytesRead, contentLength, done ->
                // range [0,1]
                val progress: Int = if (done) 100 else (bytesRead / contentLength).toInt()
                emitter.onNext(progress)
            }

            val client = OkHttpClient.Builder()
                    .addNetworkInterceptor { chain ->
                        val originalResponse = chain.proceed(chain.request())
                        originalResponse.newBuilder()
                                .body(ProgressResponseBody(originalResponse.body()!!, progressListener))
                                .build()
                    }
                    .connectTimeout(1, TimeUnit.MINUTES)
                    .readTimeout(5, TimeUnit.MINUTES)
                    .writeTimeout(1, TimeUnit.MINUTES)
                    .build()

            val call = client.newCall(request)
            emitter.setCancellable { call.cancel() }

            try {
                val response = call.execute()
                val sink = Okio.buffer(Okio.sink(outputStream))
                sink.writeAll(response.body()!!.source())
                sink.close()
                emitter.onNext(100)
                emitter.onComplete()
            } catch (e: IOException) {
                emitter.onError(e)
            }
        }
    }

}