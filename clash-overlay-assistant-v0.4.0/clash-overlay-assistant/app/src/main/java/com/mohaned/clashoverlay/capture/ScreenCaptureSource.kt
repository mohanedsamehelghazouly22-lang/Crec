package com.mohaned.clashoverlay.capture

import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.HandlerThread

class ScreenCaptureSource(
    projection: MediaProjection,
    width: Int,
    height: Int,
    densityDpi: Int,
    private val onFrame: (Image) -> Unit
) : ImageReader.OnImageAvailableListener {
    private val thread = HandlerThread("capture-frames").apply { start() }
    private val handler = Handler(thread.looper)
    private val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 3)
    private val display: VirtualDisplay

    init {
        reader.setOnImageAvailableListener(this, handler)
        display = projection.createVirtualDisplay(
            "ClashOverlayCapture", width, height, densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface, null, handler
        )
    }

    override fun onImageAvailable(r: ImageReader) {
        r.acquireLatestImage()?.let { image ->
            try { onFrame(image) } catch (_: Throwable) { image.close() }
        }
    }

    fun close() {
        display.release(); reader.close(); thread.quitSafely()
    }
}
