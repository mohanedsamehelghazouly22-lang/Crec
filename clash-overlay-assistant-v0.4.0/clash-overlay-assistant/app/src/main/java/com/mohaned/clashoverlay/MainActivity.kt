package com.mohaned.clashoverlay

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.mohaned.clashoverlay.overlay.OverlayService

class MainActivity : Activity() {

    companion object {
        private const val CAPTURE_REQUEST_CODE = 7001
    }

    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        buildUserInterface()
    }

    private fun buildUserInterface() {

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
            setBackgroundColor(Color.rgb(18, 18, 24))
        }

        val title = TextView(this).apply {
            text = "Clash Overlay Assistant"
            textSize = 24f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        val description = TextView(this).apply {
            text = "Visual assistant • Observe only"
            textSize = 15f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 40)
        }

        val startButton = Button(this).apply {
            text = "START ASSISTANT"
            textSize = 16f
            isAllCaps = false
        }

        val stopButton = Button(this).apply {
            text = "STOP ASSISTANT"
            textSize = 16f
            isAllCaps = false
        }

        statusText = TextView(this).apply {
            text = "Status: stopped"
            textSize = 15f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 32, 0, 0)
        }

        root.addView(
            title,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            description,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            startButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
        )

        root.addView(
            stopButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            statusText,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        setContentView(root)

        startButton.setOnClickListener {
            startAssistant()
        }

        stopButton.setOnClickListener {
            stopAssistant()
        }
    }

    private fun startAssistant() {

        if (!Settings.canDrawOverlays(this)) {

            statusText.text = "Status: enable overlay permission"

            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )

            startActivity(intent)

            return
        }

        val manager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        statusText.text = "Status: requesting screen capture"

        startActivityForResult(
            manager.createScreenCaptureIntent(),
            CAPTURE_REQUEST_CODE
        )
    }

    private fun stopAssistant() {

        stopService(
            Intent(
                this,
                OverlayService::class.java
            )
        )

        statusText.text = "Status: stopped"
    }

    @Deprecated("Deprecated in Android API, retained for compatibility")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (requestCode != CAPTURE_REQUEST_CODE) {
            return
        }

        if (resultCode != RESULT_OK || data == null) {

            statusText.text =
                "Status: capture permission denied"

            return
        }

        val serviceIntent =
            Intent(
                this,
                OverlayService::class.java
            ).apply {

                putExtra(
                    "resultCode",
                    resultCode
                )

                putExtra(
                    "data",
                    data
                )
            }

        startForegroundService(serviceIntent)

        statusText.text =
            "Status: running • observe only"
    }

    override fun onDestroy() {

        super.onDestroy()
    }
}
