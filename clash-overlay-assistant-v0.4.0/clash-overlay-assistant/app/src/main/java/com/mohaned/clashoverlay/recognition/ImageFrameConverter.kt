package com.mohaned.clashoverlay.recognition

import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.media.Image
import java.nio.ByteBuffer

object ImageFrameConverter {
    fun rgbaToBitmap(image: Image): Bitmap {
        require(image.format == android.graphics.ImageFormat.FLEX_RGBA_8888 || image.planes.size == 1) {
            "Screen capture must provide RGBA image data"
        }
        val plane = image.planes[0]
        val width = image.width
        val height = image.height
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * width
        val padded = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Config.ARGB_8888)
        val buffer = plane.buffer.duplicate()
        copyPlane(buffer, padded, width, height, rowStride, pixelStride)
        return if (padded.width == width) padded else Bitmap.createBitmap(padded, 0, 0, width, height).also { padded.recycle() }
    }

    private fun copyPlane(buffer: ByteBuffer, bitmap: Bitmap, width: Int, height: Int, rowStride: Int, pixelStride: Int) {
        val pixels = IntArray(width * height)
        val row = ByteArray(rowStride)
        for (y in 0 until height) {
            buffer.position(y * rowStride)
            buffer.get(row, 0, minOf(rowStride, buffer.remaining()))
            for (x in 0 until width) {
                val i = x * pixelStride
                val r = row[i].toInt() and 0xff
                val g = row[i + 1].toInt() and 0xff
                val b = row[i + 2].toInt() and 0xff
                val a = if (pixelStride >= 4) row[i + 3].toInt() and 0xff else 255
                pixels[y * width + x] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    }
}
