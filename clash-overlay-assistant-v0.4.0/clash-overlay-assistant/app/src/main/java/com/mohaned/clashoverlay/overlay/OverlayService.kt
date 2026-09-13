package com.mohaned.clashoverlay.overlay

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager

import com.mohaned.clashoverlay.capture.ScreenCaptureSource
import com.mohaned.clashoverlay.core.engine.CycleEngine
import com.mohaned.clashoverlay.core.engine.DeckTracker
import com.mohaned.clashoverlay.core.engine.ElixirEngine
import com.mohaned.clashoverlay.core.engine.HandTracker
import com.mohaned.clashoverlay.core.engine.TipsEngine
import com.mohaned.clashoverlay.core.model.GameState
import com.mohaned.clashoverlay.core.session.MatchSessionManager
import com.mohaned.clashoverlay.recognition.TfliteCardRecognizer
import com.mohaned.clashoverlay.recognition.VisionPipeline

import kotlin.math.max

class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: OverlayView? = null
    private var windowParams: WindowManager.LayoutParams? = null

    private var projection: MediaProjection? = null
    private var captureSource: ScreenCaptureSource? = null
    private var visionPipeline: VisionPipeline? = null

    private val deckTracker = DeckTracker()
    private val handTracker = HandTracker()
    private val cycleEngine = CycleEngine()
    private val elixirEngine = ElixirEngine()
    private val tipsEngine = TipsEngine()

    private val sessionManager = MatchSessionManager(
        deckTracker,
        handTracker,
        cycleEngine,
        elixirEngine
    )

    private var frameCount = 0
    private var fpsWindowStart = System.currentTimeMillis()
    private var currentFps = 0f

    override fun onCreate() {
        super.onCreate()

        windowManager =
            getSystemService(WINDOW_SERVICE) as WindowManager

        overlayView = OverlayView(this) { dx, dy ->
            moveOverlay(dx, dy)
        }

        addOverlay()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val resultCode =
            intent?.getIntExtra(
                EXTRA_RESULT_CODE,
                Activity.RESULT_CANCELED
            ) ?: Activity.RESULT_CANCELED

        val dataIntent: Intent? =
            if (Build.VERSION.SDK_INT >= 33) {
                intent?.getParcelableExtra(
                    EXTRA_DATA,
                    Intent::class.java
                )
            } else {
                @Suppress("DEPRECATION")
                intent?.getParcelableExtra(EXTRA_DATA)
            }

        if (
            resultCode != Activity.RESULT_OK ||
            dataIntent == null
        ) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundServiceNotification()
        startCapture(dataIntent)

        return START_STICKY
    }

    private fun startForegroundServiceNotification() {

        val notificationManager =
            getSystemService(
                NotificationManager::class.java
            )

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Clash Overlay",
            NotificationManager.IMPORTANCE_LOW
        )

        notificationManager.createNotificationChannel(channel)

        val stopIntent =
            Intent(this, OverlayService::class.java)
                .setAction(ACTION_STOP)

        val stopPendingIntent =
            PendingIntent.getService(
                this,
                100,
                stopIntent,
                PendingIntent.FLAG_IMMUTABLE or
                    PendingIntent.FLAG_UPDATE_CURRENT
            )

        val notification =
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle(
                    "Clash Overlay Assistant"
                )
                .setContentText(
                    "Visual assistant • observe only"
                )
                .setSmallIcon(
                    android.R.drawable.ic_dialog_info
                )
                .addAction(
                    Notification.Action.Builder(
                        null,
                        "STOP",
                        stopPendingIntent
                    ).build()
                )
                .setOngoing(true)
                .build()

        if (Build.VERSION.SDK_INT >= 29) {

            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo
                    .FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )

        } else {

            startForeground(
                NOTIFICATION_ID,
                notification
            )
        }
    }

    private fun startCapture(data: Intent) {

        if (captureSource != null) {
            return
        }

        val manager =
            getSystemService(
                MEDIA_PROJECTION_SERVICE
            ) as MediaProjectionManager

        projection =
            manager.getMediaProjection(
                Activity.RESULT_OK,
                data
            )

        val mediaProjection =
            projection ?: return

        sessionManager.start()

        val recognizer =
            TfliteCardRecognizer(this)

        visionPipeline =
            VisionPipeline(recognizer)

        val metrics =
            resources.displayMetrics

        captureSource =
            ScreenCaptureSource(
                mediaProjection,
                metrics.widthPixels,
                metrics.heightPixels,
                metrics.densityDpi
            ) { image ->

                try {

                    val now =
                        System.currentTimeMillis()

                    val observations =
                        visionPipeline
                            ?.process(
                                image,
                                now
                            )
                            .orEmpty()

                    for (observation in observations) {

                        deckTracker.observe(
                            observation
                        )

                        if (
                            observation.roiId
                                .startsWith(
                                    "opponent_hand"
                                )
                        ) {
                            handTracker.observe(
                                observation
                            )
                        }

                        if (
                            observation.roiId
                                .startsWith(
                                    "opponent_play"
                                )
                        ) {

                            cycleEngine.observePlayed(
                                observation.cardId,
                                observation.confidence
                            )

                            elixirEngine.observeSpent(
                                estimateCardCost(
                                    observation.cardId
                                )
                            )
                        }
                    }

                    cycleEngine.updateFromHand(
                        handTracker.snapshot()
                    )

                    val elixir =
                        elixirEngine.estimate()

                    val predictedNext =
                        cycleEngine.predictedNext(
                            deckTracker.snapshot(),
                            handTracker.snapshot()
                        )

                    val tip =
                        tipsEngine.evaluate(
                            GameState(
                                matchId =
                                    sessionManager.matchId,

                                running = true,

                                opponentDeck =
                                    deckTracker.snapshot(),

                                opponentHand =
                                    handTracker.snapshot(),

                                predictedNextCardId =
                                    predictedNext,

                                cycleIndex =
                                    cycleEngine.index(),

                                cycleConfidence =
                                    cycleEngine.confidence(),

                                elixir =
                                    elixir,

                                gamePhase =
                                    "BATTLE",

                                lastTip =
                                    null,

                                fps =
                                    currentFps,

                                recognitionLatencyMs =
                                    visionPipeline
                                        ?.lastLatencyMs
                                        ?: 0L,

                                lastUpdatedMs =
                                    now
                            )
                        )

                    updateFps(now)

                    val state =
                        GameState(
                            matchId =
                                sessionManager.matchId,

                            running = true,

                            opponentDeck =
                                deckTracker.snapshot(),

                            opponentHand =
                                handTracker.snapshot(),

                            predictedNextCardId =
                                predictedNext,

                            cycleIndex =
                                cycleEngine.index(),

                            cycleConfidence =
                                cycleEngine.confidence(),

                            elixir =
                                elixir,

                            gamePhase =
                                "BATTLE",

                            lastTip =
                                tip?.text,

                            fps =
                                currentFps,

                            recognitionLatencyMs =
                                visionPipeline
                                    ?.lastLatencyMs
                                    ?: 0L,

                            lastUpdatedMs =
                                now
                        )

                    overlayView?.post {
                        overlayView?.render(state)
                    }

                } catch (_: Throwable) {

                    // Keep capture loop alive.
                    // Individual frame errors must not
                    // kill the foreground service.

                } finally {

                    image.close()
                }
            }
    }

    private fun estimateCardCost(
        cardId: String
    ): Int {

        return when (cardId) {

            "skeletons" ->
                1

            "goblins" ->
                2

            "knight",
            "archers",
            "arrows",
            "little-prince" ->
                3

            "fireball",
            "hog-rider",
            "musketeer",
            "golden-knight",
            "skeleton-king",
            "mighty-miner" ->
                4

            "archer-queen",
            "monk",
            "goblinstein" ->
                5

            "boss-bandit" ->
                6

            else ->
                4
        }
    }

    private fun updateFps(now: Long) {

        frameCount++

        val elapsed =
            now - fpsWindowStart

        if (elapsed >= 1000L) {

            currentFps =
                frameCount *
                    1000f /
                    elapsed.toFloat()

            frameCount = 0
            fpsWindowStart = now
        }
    }

    private fun addOverlay() {

        val type =
            if (Build.VERSION.SDK_INT >= 26) {
                WindowManager.LayoutParams
                    .TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

        windowParams =
            WindowManager.LayoutParams(
                320,
                116,
                type,
                WindowManager.LayoutParams
                    .FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams
                        .FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {

                gravity =
                    Gravity.TOP or Gravity.END

                x = 16
                y = 180
            }

        runCatching {
            windowManager?.addView(
                overlayView,
                windowParams
            )
        }
    }

    private fun moveOverlay(
        dx: Int,
        dy: Int
    ) {

        val params =
            windowParams ?: return

        params.x =
            max(
                0,
                params.x - dx
            )

        params.y =
            max(
                0,
                params.y + dy
            )

        runCatching {
            windowManager?.updateViewLayout(
                overlayView,
                params
            )
        }
    }

    override fun onDestroy() {

        captureSource?.close()
        captureSource = null

        visionPipeline?.close()
        visionPipeline = null

        projection?.stop()
        projection = null

        sessionManager.end()

        overlayView?.let {
            runCatching {
                windowManager?.removeView(it)
            }
        }

        overlayView = null

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? = null

    companion object {

        const val ACTION_STOP =
            "com.mohaned.clashoverlay.STOP"

        const val EXTRA_RESULT_CODE =
            "resultCode"

        const val EXTRA_DATA =
            "data"

        private const val CHANNEL_ID =
            "clash_overlay"

        private const val NOTIFICATION_ID =
            42
    }
}
