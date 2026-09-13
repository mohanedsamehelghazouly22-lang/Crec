package com.mohaned.clashoverlay.debug

data class DebugSnapshot(val fps:Float,val latencyMs:Long,val recognizedCard:String?,val confidence:Float,val roi:String?,val elixir:String,val hand:List<String>,val deck:List<String>,val cycle:String)
