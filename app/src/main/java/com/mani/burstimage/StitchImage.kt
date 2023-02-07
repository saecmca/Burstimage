package com.mani.burstimage

import android.app.ProgressDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.Picasso
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*


class StitchImage : AppCompatActivity() {
    private lateinit var outputDirectory: File
    lateinit var progress:ProgressDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stitch_image)

        outputDirectory = getOutputDirectory()
        OpenCVLoader.initDebug()
        progress=ProgressDialog.show(this@StitchImage,"Stitch Image","Loading...")
        Handler(Looper.getMainLooper()).postDelayed({
            stitch()
        }, 50)


    }

    private fun stitch() {
        val imgBitmap=StitchModel.stitchByTransaction(outputDirectory,"",this)

        if(imgBitmap!=null) {
            val imageView = findViewById<ImageView>(R.id.iv)
            val file = saveBitmap(imgBitmap, "stitch_horizontal")
            val displayMetrics = resources.displayMetrics
            Picasso.get()
                .load(file)
                .resize(displayMetrics.widthPixels, 300)
                .memoryPolicy(MemoryPolicy.NO_CACHE)
                .into(imageView)
        }
        progress.dismiss()
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }
    fun saveBitmap(bitmap: Bitmap, fileNameOpening: String):File {
        val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        val pictureFile = File(outputDirectory, "" +  "_stitch.jpg")

        try {
            pictureFile.createNewFile()
            val oStream = FileOutputStream(pictureFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, oStream)
            oStream.flush()
            oStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
            Log.i("TAG", "There was an issue saving the image.")
        }
        return pictureFile
    }

}