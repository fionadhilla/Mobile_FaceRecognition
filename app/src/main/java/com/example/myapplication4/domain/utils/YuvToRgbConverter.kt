package com.example.myapplication4.domain.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.media.Image
import android.renderscript.*
import android.util.Log
import java.nio.ByteBuffer

class YuvToRgbConverter(context: Context) {
    private val rs = RenderScript.create(context)
    private val scriptYuvToRgb = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))

    private var pixelCount: Int = -1
    private lateinit var yuvBuffer: ByteBuffer
    private lateinit var inputAllocation: Allocation
    private lateinit var outputAllocation: Allocation

    @Synchronized
    fun yuvToRgb(image: Image, output: Bitmap) {
        // Pastikan format gambar sesuai
        if (image.format != ImageFormat.YUV_420_888) {
            Log.e("YuvToRgbConverter", "Input Image format must be YUV_420_888, but got ${image.format}")
            return
        }

        // Ensure that the intermediate output byte buffer is allocated
        if (!::yuvBuffer.isInitialized || pixelCount != image.cropRect.width() * image.cropRect.height()) {
            pixelCount = image.cropRect.width() * image.cropRect.height()
            yuvBuffer = ByteBuffer.allocateDirect(
                pixelCount * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8 // Ini adalah perkiraan ukuran untuk YUV
            ).order(java.nio.ByteOrder.nativeOrder()) // Penting untuk native order
        }

        // Get the YUV data in byte array form
        imageToByteBuffer(image, yuvBuffer) // Fungsi ini perlu diimplementasikan

        // Ensure that the RenderScript inputs and outputs are allocated
        if (!::inputAllocation.isInitialized || inputAllocation.bytesSize != yuvBuffer.array().size) {
            inputAllocation = Allocation.createSized(rs, Element.U8(rs), yuvBuffer.array().size)
        }
        // outputAllocation harus sesuai dengan ukuran bitmap output. Jika output bitmap berubah ukuran, ini perlu dialokasikan ulang
        if (!::outputAllocation.isInitialized || outputAllocation.getType().getX() != output.width || outputAllocation.getType().getY() != output.height) {
            // Daur ulang alokasi lama jika ada untuk mencegah kebocoran memori
            if (::outputAllocation.isInitialized) {
                outputAllocation.destroy()
            }
            outputAllocation = Allocation.createFromBitmap(rs, output)
        }

        // Convert YUV to RGB
        inputAllocation.copyFrom(yuvBuffer.array())
        scriptYuvToRgb.setInput(inputAllocation)
        scriptYuvToRgb.forEach(outputAllocation)
        outputAllocation.copyTo(output)
    }

    private fun imageToByteBuffer(image: Image, outputBuffer: ByteBuffer) {
        outputBuffer.rewind()

        val planes = image.planes
        var maxRowStride = 0
        for (plane in planes) {
            if (plane.rowStride > maxRowStride) {
                maxRowStride = plane.rowStride
            }
        }
        val rowData = ByteArray(maxRowStride)

        for (i in planes.indices) {
            val plane = planes[i]
            val buffer = plane.buffer
            val rowStride = plane.rowStride
            val pixelStride = plane.pixelStride

            val planeWidth = if (i == 0) image.width else image.width / 2
            val planeHeight = if (i == 0) image.height else image.height / 2

            buffer.position(0) // Pastikan buffer plane juga diatur ulang

            for (row in 0 until planeHeight) {
                val bytesToRead = minOf(rowStride, buffer.remaining())

                if (bytesToRead <= 0) {
                    Log.e("YuvToRgbConverter", "No more bytes to read for plane $i, row $row. Bytes remaining: ${buffer.remaining()}, rowStride: $rowStride")
                    break
                }

                buffer.get(rowData, 0, bytesToRead)

                for (col in 0 until planeWidth) {
                    val byteIndex = col * pixelStride
                    if (byteIndex < bytesToRead) {
                        outputBuffer.put(rowData[byteIndex])
                    } else {
                        Log.e("YuvToRgbConverter", "byteIndex ($byteIndex) out of bounds for rowData (bytes read ${bytesToRead}). Plane: $i, Row: $row, Col: $col")
                        break
                    }
                }
            }
        }
        outputBuffer.rewind()
    }


    fun close() {
        if (::inputAllocation.isInitialized) inputAllocation.destroy()
        if (::outputAllocation.isInitialized) outputAllocation.destroy()
        rs.destroy()
    }
}