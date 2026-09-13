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
    private val projection: MediaProjection,
    width: Int,
    height: Int,
    densityDpi: Int,
    private val onFrame: (Image) -> Unit
) : ImageReader.OnImageAvailableListener {

    private val thread =
        HandlerThread("capture-frames").apply {
            start()
        }

    private val handler =
        Handler(thread.looper)

    private val reader =
        ImageReader.newInstance(
            width,
            height,
            PixelFormat.RGBA_8888,
            3
        )

    private var display: VirtualDisplay? = null
    private var closed = false

    private val projectionCallback =
        object : MediaProjection.Callback() {

            override fun onStop() {
                closeInternal(
                    unregisterCallback = false
                )
            }
        }

    init {

        // REQUIRED on Android 14+
        // Must happen before createVirtualDisplay()
        projection.registerCallback(
            projectionCallback,
            handler
        )

        reader.setOnImageAvailableListener(
            this,
            handler
        )

        display =
            projection.createVirtualDisplay(
                "ClashOverlayCapture",
                width,
                height,
                densityDpi,
                DisplayManager
                    .VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                handler
            )
    }

    override fun onImageAvailable(
        reader: ImageReader
    ) {

        if (closed) return

        reader.acquireLatestImage()?.let { image ->

            try {

                onFrame(image)

            } catch (_: Throwable) {

                image.close()
            }
        }
    }

    fun close() {

        closeInternal(
            unregisterCallback = true
        )
    }

    private fun closeInternal(
        unregisterCallback: Boolean
    ) {

        if (closed) return

        closed = true

        if (unregisterCallback) {

            runCatching {

                projection.unregisterCallback(
                    projectionCallback
                )
            }
        }

        runCatching {
            display?.release()
        }

        display = null

        runCatching {

            reader.setOnImageAvailableListener(
                null,
                null
            )
        }

        runCatching {
            reader.close()
        }

        thread.quitSafely()
    }
}
