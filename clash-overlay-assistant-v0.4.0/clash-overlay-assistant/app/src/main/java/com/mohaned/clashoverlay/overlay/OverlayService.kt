package com.mohaned.clashoverlay.overlay

import android.app.*
import android.content.*
import android.graphics.PixelFormat
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.view.*
import com.mohaned.clashoverlay.capture.ScreenCaptureSource
import com.mohaned.clashoverlay.core.engine.*
import com.mohaned.clashoverlay.core.model.GameState
import com.mohaned.clashoverlay.core.session.MatchSessionManager
import com.mohaned.clashoverlay.recognition.*
import kotlin.math.max

class OverlayService:Service(){
    private var wm:WindowManager?=null;private var overlay:OverlayView?=null;private var params:WindowManager.LayoutParams?=null;private var projection:MediaProjection?=null;private var capture:ScreenCaptureSource?=null;private var pipeline:VisionPipeline?=null
    private val deck=DeckTracker();private val hand=HandTracker();private val cycle=CycleEngine();private val elixir=ElixirEngine();private val tips=TipsEngine();private val session=MatchSessionManager(deck,hand,cycle,elixir)
    private var frameCount=0;private var fpsWindowMs=System.currentTimeMillis();private var fps=0f; private var modelReady=false
    override fun onCreate(){super.onCreate();wm=getSystemService(WINDOW_SERVICE)as WindowManager;overlay=OverlayView(this){dx,dy->moveOverlay(dx,dy)};addOverlay()}
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int{if(intent?.action=="STOP"){stopSelf();return START_NOT_STICKY};val rc=intent?.getIntExtra("resultCode",Activity.RESULT_CANCELED)?:Activity.RESULT_CANCELED;val data=if(Build.VERSION.SDK_INT>=33)intent?.getParcelableExtra("data",Intent::class.java) else @Suppress("DEPRECATION") intent?.getParcelableExtra("data");if(rc!=Activity.RESULT_OK||data==null){stopSelf();return START_NOT_STICKY};startAsForeground();startCapture(data);return START_STICKY}
    private fun startAsForeground(){val nm=getSystemService(NotificationManager::class.java);nm.createNotificationChannel(NotificationChannel("overlay","Overlay",NotificationManager.IMPORTANCE_LOW));val stop=PendingIntent.getService(this,9,Intent(this,OverlayService::class.java).setAction("STOP"),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT);val n=Notification.Builder(this,"overlay").setContentTitle("Clash Overlay Assistant").setContentText("Visual assistant • observe only").setSmallIcon(android.R.drawable.ic_dialog_info).addAction(Notification.Action.Builder(null,"STOP",stop).build()).setOngoing(true).build();if(Build.VERSION.SDK_INT>=29)startForeground(42,n,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)else startForeground(42,n)}
    private fun startCapture(data:Intent){if(capture!=null)return;val mgr=getSystemService(MEDIA_PROJECTION_SERVICE)as MediaProjectionManager;projection=mgr.getMediaProjection(Activity.RESULT_OK,data);val p=projection?:return;session.start();val recognizer=TfliteCardRecognizer(this); modelReady=recognizer.isAvailable(); pipeline=VisionPipeline(recognizer);val m=resources.displayMetrics;capture=ScreenCaptureSource(p,m.widthPixels,m.heightPixels,m.densityDpi){image->try{val now=System.currentTimeMillis();val obs=pipeline?.process(image,now).orEmpty();obs.forEach{deck.observe(it);if(it.roiId.startsWith("opponent_hand"))hand.observe(it);if(it.roiId.startsWith("opponent_play")){cycle.observePlayed(it.cardId,it.confidence);elixir.observeSpent(estimateCost(it.cardId))}};cycle.updateFromHand(hand.snapshot());val e=elixir.estimate();val tip=tips.evaluate(GameState(session.matchId,true,deck.snapshot(),hand.snapshot(),cycle.predictedNext(deck.snapshot(),hand.snapshot()),cycle.index(),cycle.confidence(),e,"BATTLE",null,pipeline?.lastLatencyMs?:0,System.currentTimeMillis()));updateFps(now);val state=GameState(session.matchId,true,deck.snapshot(),hand.snapshot(),cycle.predictedNext(deck.snapshot(),hand.snapshot()),cycle.index(),cycle.confidence(),e,"BATTLE",tip?.text,pipeline?.lastLatencyMs?:0,now);overlay?.post{overlay?.render(state)}}finally{image.close()}}}
    private fun estimateCost(cardId:String)=when(cardId){"skeletons"->1;"goblins"->2;"knight","archers","arrows","little-prince"->3;"fireball","hog-rider","musketeer","golden-knight","skeleton-king","mighty-miner"->4;"archer-queen","monk","goblinstein"->5;"boss-bandit"->6;else->4}
    private fun updateFps(now:Long){frameCount++;val elapsed=now-fpsWindowMs;if(elapsed>=1000){fps=frameCount*1000f/elapsed;frameCount=0;fpsWindowMs=now}}
    private fun addOverlay(){val type=if(Build.VERSION.SDK_INT>=26)WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE;params=WindowManager.LayoutParams(320,116,type,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,PixelFormat.TRANSLUCENT).apply{gravity=Gravity.TOP or Gravity.END;x=16;y=180};wm?.addView(overlay,params)}
    private fun moveOverlay(dx:Int,dy:Int){val p=params?:return;p.x=max(0,p.x-dx);p.y=max(0,p.y+dy);runCatching{wm?.updateViewLayout(overlay,p)}}
    override fun onDestroy(){capture?.close();capture=null;pipeline?.close();pipeline=null;projection?.stop();projection=null;session.end();overlay?.let{runCatching{wm?.removeView(it)}};overlay=null;super.onDestroy()}
    override fun onBind(intent:Intent?):IBinder?=null
}
