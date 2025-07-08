package com.example.Mobile_FaceRecognition.utils

import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.Image
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer


object ImageUtils {
    private const val kMaxChannelValue = 262143

    /**
     * Utility method to compute the allocated size in bytes of a YUV420SP image of the given
     * dimensions.
     */
    fun getYUVByteSize(width: Int, height: Int): Int {
        val ySize = width * height
        val uvSize = ((width + 1) / 2) * ((height + 1) / 2) * 2

        return ySize + uvSize
    }

    /**
     * Saves a Bitmap object to disk for analysis.
     *
     * @param bitmap The bitmap to save.
     */
    fun saveBitmap(bitmap: Bitmap) {
        saveBitmap(bitmap, "preview.png")
    }

    /**
     * Saves a Bitmap object to disk for analysis.
     *
     * @param bitmap The bitmap to save.
     * @param filename The location to save the bitmap to.
     */
    fun saveBitmap(bitmap: Bitmap, filename: String) {
        val root =
            Environment.getExternalStorageDirectory().absolutePath + File.separator + "tensorflow"
        val myDir = File(root)

        if (!myDir.mkdirs()) {
            // LOGGER.i("Make dir failed");
        }

        val fname = filename
        val file = File(myDir, fname)
        if (file.exists()) {
            file.delete()
        }
        try {
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 99, out)
            out.flush()
            out.close()
        } catch (e: Exception) {
            //LOGGER.e(e, "Exception!");
        }
    }

    fun convertYUV420SPToARGB8888(input: ByteArray, width: Int, height: Int, output: IntArray) {
        val frameSize = width * height
        for (j in 0 until height) {
            var uvp = frameSize + (j shr 1) * width
            var u = 0
            var v = 0

            for (i in 0 until width) {
                var y = 0xff and input[j * width + i].toInt()
                if ((i and 1) == 0) {
                    v = 0xff and input[uvp++].toInt()
                    u = 0xff and input[uvp++].toInt()
                }

                output[j * width + i] = YUV2RGB(y, u, v)
            }
        }
    }

    private fun YUV2RGB(y: Int, u: Int, v: Int): Int {
        // Adjust and check YUV values
        var newY = (y - 16).coerceAtLeast(0)
        val newU = u - 128
        val newV = v - 128

        val y1192 = 1192 * newY
        var r = (y1192 + 1634 * newV)
        var g = (y1192 - 833 * newV - 400 * newU)
        var b = (y1192 + 2066 * newU)

        // Clipping RGB values to be inside boundaries [ 0 , kMaxChannelValue ]
        r = r.coerceIn(0, kMaxChannelValue)
        g = g.coerceIn(0, kMaxChannelValue)
        b = b.coerceIn(0, kMaxChannelValue)

        return 0xff000000.toInt() or ((r shl 6) and 0xff0000) or ((g shr 2) and 0xff00) or ((b shr 10) and 0xff)
    }

    fun convertYUV420ToARGB8888(
        yData: ByteArray,
        uData: ByteArray,
        vData: ByteArray,
        width: Int,
        height: Int,
        yRowStride: Int,
        uvRowStride: Int,
        uvPixelStride: Int,
        out: IntArray
    ) {
        var yp = 0
        for (j in 0 until height) {
            val pY = yRowStride * j
            val pUV = uvRowStride * (j shr 1)

            for (i in 0 until width) {
                val uvOffset = pUV + (i shr 1) * uvPixelStride

                out[yp++] = YUV2RGB(0xff and yData[pY + i].toInt(), 0xff and uData[uvOffset].toInt(), 0xff and vData[uvOffset].toInt())
            }
        }
    }

    /**
     * Mengembalikan matriks transformasi yang memetakan koordinat dari sumber
     * ke koordinat tujuan, dengan memperhitungkan rotasi dan pemeliharaan aspek rasio.
     * Matriks ini cocok untuk mengubah gambar mentah kamera menjadi bitmap yang diputar/dipotong.
     *
     * @param srcWidth Lebar sumber (misalnya, lebar pratinjau kamera).
     * @param srcHeight Tinggi sumber (misalnya, tinggi pratinjau kamera).
     * @param dstWidth Lebar tujuan (misalnya, lebar croppedBitmap).
     * @param dstHeight Tinggi tujuan (misalnya, tinggi croppedBitmap).
     * @param applyRotation Rotasi yang akan diterapkan pada sumber (misalnya, sensorOrientation).
     * @param maintainAspectRatio Jika true, skala akan mempertahankan aspek rasio, mungkin
     * menghasilkan letterboxing/pillarboxing pada tujuan.
     */
    fun getTransformationMatrix(
        srcWidth: Int,
        srcHeight: Int,
        dstWidth: Int,
        dstHeight: Int,
        applyRotation: Int,
        maintainAspectRatio: Boolean
    ): Matrix {
        val matrix = Matrix()

        // Translate the coordinate system origin to the center of the source rectangle
        matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f)

        // Apply rotation around the center
        matrix.postRotate(applyRotation.toFloat())

        // Determine if the dimensions are transposed after rotation (e.g., 90 or 270 degrees)
        val transpose = (kotlin.math.abs(applyRotation) + 90) % 180 == 0

        val inWidth = if (transpose) srcHeight else srcWidth
        val inHeight = if (transpose) srcWidth else srcHeight

        // Apply scaling if necessary.
        if (inWidth != dstWidth || inHeight != dstHeight) {
            val scaleFactorX = dstWidth / inWidth.toFloat()
            val scaleFactorY = dstHeight / inHeight.toFloat()

            if (maintainAspectRatio) {
                val scaleFactor = kotlin.math.min(scaleFactorX, scaleFactorY)
                matrix.postScale(scaleFactor, scaleFactor)
            } else {
                matrix.postScale(scaleFactorX, scaleFactorY)
            }
        }

        matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f)

        return matrix
    }

    /**
     * Mengembalikan matriks transformasi yang akan memetakan koordinat dari frame kamera asli
     * (ukuran previewWidth x previewHeight) ke koordinat piksel pada OverlayView (ukuran canvas.width x canvas.height).
     * Ini memperhitungkan rotasi sensor, penskalaan untuk mengisi TextureView sambil mempertahankan
     * aspek rasio (FIT), dan pencerminan untuk kamera depan.
     *
     * @param frameWidth Lebar frame pratinjau kamera (previewWidth).
     * @param frameHeight Tinggi frame pratinjau kamera (previewHeight).
     * @param canvasWidth Lebar kanvas OverlayView (textureView.width).
     * @param canvasHeight Tinggi kanvas OverlayView (textureView.height).
     * @param sensorOrientation Orientasi sensor kamera.
     * @param isFrontCamera True jika kamera depan sedang digunakan.
     * @return Matriks yang dapat digunakan untuk mentransformasi RectF.
     */
    fun getFrameToCanvasMatrix(
        frameWidth: Int,
        frameHeight: Int,
        canvasWidth: Int,
        canvasHeight: Int,
        sensorOrientation: Int,
        isFrontCamera: Boolean
    ): Matrix {
        val matrix = Matrix()

        val scaleX = canvasWidth.toFloat() / frameWidth
        val scaleY = canvasHeight.toFloat() / frameHeight
        val scale = kotlin.math.min(scaleX, scaleY)

        val scaledFrameWidth = frameWidth * scale
        val scaledFrameHeight = frameHeight * scale
        val translateX = (canvasWidth - scaledFrameWidth) / 2.0f
        val translateY = (canvasHeight - scaledFrameHeight) / 2.0f

        matrix.postScale(scale, scale)
        matrix.postTranslate(translateX, translateY)

        if (isFrontCamera) {
            matrix.postScale(-1f, 1f, canvasWidth / 2f, canvasHeight / 2f)
        }

        return matrix
    }

    fun fillBytes(planes: Array<Image.Plane>, yuvBytes: Array<ByteArray?>) {
        for (i in planes.indices) {
            val buffer = planes[i].buffer
            if (yuvBytes[i] == null || yuvBytes[i]!!.size != buffer.capacity()) {
                yuvBytes[i] = ByteArray(buffer.capacity())
            }
            buffer.get(yuvBytes[i])
        }
    }
}