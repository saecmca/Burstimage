package com.mani.burstimage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Environment
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.Picasso
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

object StitchModel {
    fun stitchByTransaction(fullpath: File, transactionId: String,context: Context):Bitmap{
        var imgBitmap:Bitmap?=null
        val options = BitmapFactory.Options()
        options.inScaled = true
        var imageList= ArrayList<Mat>()
        var imageBitmap= ArrayList<Bitmap>()
        try {
           // var fullpath = outputDirectory
            if (fullpath != null && fullpath.listFiles() != null) {
                val files: Array<File> = fullpath.listFiles()
                Arrays.sort(files) { a, b ->
                    java.lang.Long.compare(
                        a.lastModified(),
                        b.lastModified()
                    )
                }
                for (i in 0 until files.size) {
                    val currentFile=files.get(i)
                    if (currentFile.name.contains(transactionId) && !currentFile.name.contains("_stitch")) {
                        Log.w("filename",""+currentFile.name)
                        val bitmap = BitmapFactory.decodeFile(currentFile.getAbsolutePath())
                        val imgBitmap= exifInterface(bitmap,currentFile.getAbsolutePath())
                        if (isBlurredImage(imgBitmap)) {
                            //if the image is null, popping an alert dialog to recapture the image
                            Toast.makeText(context,"Image is Blur. Kindly retake the image",
                                Toast.LENGTH_SHORT).show()
                            // imageList.clear()
                            // break
                        }else {
                            val imgMat = Mat()
                            Utils.bitmapToMat(imgBitmap, imgMat)
                            imageList.add(imgMat)
                            imageBitmap.add(imgBitmap)
                        }
                    }
                }
                if(imageList.isNotEmpty()) {
                    imgBitmap = stitchImagesHorizontal(imageList)

                    //find duplicate image
                    var j=0
                    for(i in 0 until imageBitmap.size){
                        Log.w("ii",""+i+"="+imageBitmap.size)
                        for(j in j until imageBitmap.size) {
                            if(i!=j) {
                                Log.w("jj", "" + i + "=" + j)
                                checkDuplicate(context, imageBitmap.get(i), imageBitmap.get(j))
                            }
                        }
                        j++
                    }

                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return imgBitmap!!
    }
    fun stitchImagesHorizontal(src: List<Mat?>?): Bitmap {
        val dst = Mat()
        Core.hconcat(src, dst)
        val imgBitmap = Bitmap.createBitmap(dst.cols(), dst.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(dst, imgBitmap)
        return imgBitmap
    }
    fun exifInterface(bitmap: Bitmap, photoPath: String): Bitmap {
        val ei = ExifInterface(photoPath)
        val orientation: Int = ei.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )

        var rotatedBitmap: Bitmap?
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotatedBitmap = RotateBitmap(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotatedBitmap = RotateBitmap(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotatedBitmap = RotateBitmap(bitmap, 270f)
            ExifInterface.ORIENTATION_NORMAL -> rotatedBitmap = bitmap
            else -> rotatedBitmap = bitmap
        }
        return rotatedBitmap!!
    }
    fun RotateBitmap(source: Bitmap, angle: Float): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }
    @Synchronized
    private fun isBlurredImage(image: Bitmap?): Boolean {

        return try {
            if (image != null) {
                val opt: BitmapFactory.Options = BitmapFactory.Options()
                opt.inDither = true
                opt.inPreferredConfig = Bitmap.Config.ARGB_8888
                val l = CvType.CV_8UC1
                val matImage = Mat()
                Utils.bitmapToMat(image, matImage)
                val matImageGrey = Mat()
                Imgproc.cvtColor(matImage, matImageGrey, Imgproc.COLOR_BGR2GRAY)
                val dst2 = Mat()
                Utils.bitmapToMat(image, dst2)
                val laplacianImage = Mat()
                dst2.convertTo(laplacianImage, l)
                Imgproc.Laplacian(matImageGrey, laplacianImage, CvType.CV_8U)
                val laplacianImage8bit = Mat()
                laplacianImage.convertTo(laplacianImage8bit, l)
                System.gc()
                val bmp: Bitmap = Bitmap.createBitmap(
                    laplacianImage8bit.cols(),
                    laplacianImage8bit.rows(), Bitmap.Config.ARGB_8888
                )
                Utils.matToBitmap(laplacianImage8bit, bmp)
                val pixels = IntArray(bmp.getHeight() * bmp.getWidth())
                bmp.getPixels(
                    pixels, 0, bmp.getWidth(), 0, 0, bmp.getWidth(),
                    bmp.getHeight()
                )
                if (bmp != null) if (!bmp.isRecycled()) {
                    bmp.recycle()
                }
                var maxLap = -16777216
                for (i in pixels.indices) {
                    if (pixels[i] > maxLap) {
                        maxLap = pixels[i]
                    }
                }
                //blurValue = maxLap
                val soglia = -6118750
                if (maxLap < soglia || maxLap == soglia) {
                    true
                } else {
                    false
                }
            } else {
                false
            }
        } catch (e: NullPointerException) {
            false
        } catch (e: OutOfMemoryError) {
            false
        }
    }
    private fun checkDuplicate(context: Context,bmpimg1:Bitmap,bmpimg2:Bitmap) {
        var bmpimg1=bmpimg1
        var bmpimg2=bmpimg2
        if (bmpimg1 != null && bmpimg2 != null) {
            /*if(bmpimg1.getWidth()!=bmpimg2.getWidth()){
						bmpimg2 = Bitmap.createScaledBitmap(bmpimg2, bmpimg1.getWidth(), bmpimg1.getHeight(), true);
					}*/
            bmpimg1 = Bitmap.createScaledBitmap(bmpimg1, 100, 100, true)
            bmpimg2 = Bitmap.createScaledBitmap(bmpimg2, 100, 100, true)
            val img1 = Mat()
            Utils.bitmapToMat(bmpimg1, img1)
            val img2 = Mat()
            Utils.bitmapToMat(bmpimg2, img2)
            Imgproc.cvtColor(img1, img1, Imgproc.COLOR_RGBA2GRAY)
            Imgproc.cvtColor(img2, img2, Imgproc.COLOR_RGBA2GRAY)
            img1.convertTo(img1, CvType.CV_32F)
            img2.convertTo(img2, CvType.CV_32F)
            //Log.d("ImageComparator", "img1:"+img1.rows()+"x"+img1.cols()+" img2:"+img2.rows()+"x"+img2.cols());
            val hist1 = Mat()
            val hist2 = Mat()
            val histSize = MatOfInt(180)
            val channels = MatOfInt(0)
            val bgr_planes1 = ArrayList<Mat>()
            val bgr_planes2 = ArrayList<Mat>()
            Core.split(img1, bgr_planes1)
            Core.split(img2, bgr_planes2)
            val histRanges = MatOfFloat(0f, 180f)
            val accumulate = false
            Imgproc.calcHist(bgr_planes1, channels, Mat(), hist1, histSize, histRanges, accumulate)
            Core.normalize(hist1, hist1, 0.0, hist1.rows().toDouble(), Core.NORM_MINMAX, -1, Mat())
            Imgproc.calcHist(bgr_planes2, channels, Mat(), hist2, histSize, histRanges, accumulate)
            Core.normalize(hist2, hist2, 0.0, hist2.rows().toDouble(), Core.NORM_MINMAX, -1, Mat())
            img1.convertTo(img1, CvType.CV_32F)
            img2.convertTo(img2, CvType.CV_32F)
            hist1.convertTo(hist1, CvType.CV_32F)
            hist2.convertTo(hist2, CvType.CV_32F)
            val compare = Imgproc.compareHist(hist1, hist2, Imgproc.CV_COMP_CHISQR)
            Log.d("ImageComparator", "compare: $compare")
            if (compare > 0 && compare < 1000) {
                Log.w("StitchModel","Images may be possible duplicates, verifying")
                //asyncTask(this@MainActivity).execute()
            } else if (compare == 0.0)
                Log.w("StitchModel","Images are exact duplicates")
            else
                Log.w("StitchModel","Images are not duplicates")
            //startTime = System.currentTimeMillis()
        } else Toast.makeText(
            context,
            "You haven't selected images.", Toast.LENGTH_LONG
        )
            .show()
    }
}