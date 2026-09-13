package com.mohaned.clashoverlay

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import com.mohaned.clashoverlay.overlay.OverlayService

class MainActivity:Activity(){
    private val captureRequest=7001
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContentView(R.layout.activity_main);val start=findViewById<Button>(R.id.startButton);val stop=findViewById<Button>(R.id.stopButton);val status=findViewById<TextView>(R.id.statusText);stop.setOnClickListener{stopService(Intent(this,OverlayService::class.java));status.text="Status: stopped"};start.setOnClickListener{if(!Settings.canDrawOverlays(this)){startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName")));status.text="Status: enable overlay permission";return@setOnClickListener};val mgr=getSystemService(MEDIA_PROJECTION_SERVICE)as MediaProjectionManager;startActivityForResult(mgr.createScreenCaptureIntent(),captureRequest)}}
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){super.onActivityResult(requestCode,resultCode,data);if(requestCode!=captureRequest)return;val status=findViewById<TextView>(R.id.statusText);if(resultCode!=RESULT_OK||data==null){status.text="Status: capture permission denied";return};startForegroundService(Intent(this,OverlayService::class.java).apply{putExtra("resultCode",resultCode);putExtra("data",data)});status.text="Status: running • observe only"}
}
