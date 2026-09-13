package com.mohaned.clashoverlay.overlay

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import com.mohaned.clashoverlay.core.model.GameState
import kotlin.math.roundToInt

class OverlayView(context:Context,private val moveWindow:(Int,Int)->Unit):View(context){
    private val bg=Paint(1).apply{color=Color.argb(218,12,15,22)}; private val stroke=Paint(1).apply{color=Color.argb(95,255,255,255);style=Paint.Style.STROKE;strokeWidth=1.5f}; private val text=Paint(1).apply{color=Color.WHITE;typeface=Typeface.DEFAULT_BOLD};
    private var state=GameState("idle"); private var downX=0f;private var downY=0f
    fun render(s:GameState){state=s;invalidate()}
    override fun onTouchEvent(e:MotionEvent):Boolean{when(e.actionMasked){MotionEvent.ACTION_DOWN->{downX=e.rawX;downY=e.rawY;return true};MotionEvent.ACTION_MOVE->{val dx=(e.rawX-downX).roundToInt();val dy=(e.rawY-downY).roundToInt();if(dx!=0||dy!=0){moveWindow(dx,dy);downX=e.rawX;downY=e.rawY};return true}};return true}
    override fun onDraw(c:Canvas){c.drawRoundRect(0f,0f,width.toFloat(),height.toFloat(),18f,18f,bg);c.drawRoundRect(.75f,.75f,width-.75f,height-.75f,18f,18f,stroke)
        text.textSize=30f;c.drawText("%.1f".format(state.elixir.current),18f,39f,text);text.textSize=11f;c.drawText("ELIXIR ${state.elixir.min.f1()}–${state.elixir.max.f1()}  ${pct(state.elixir.confidence)}",18f,57f,text)
        c.drawText("CYCLE ${state.cycleIndex}/4  ${pct(state.cycleConfidence)}",18f,75f,text);text.textSize=10f
        c.drawText("NEXT  ${state.predictedNextCardId ?: "—"}",18f,93f,text);c.drawText(state.lastTip ?: "OBSERVE ONLY • ON-DEVICE",18f,height-10f,text)
    }
    private fun pct(v:Float)="${(v.coerceIn(0f,1f)*100).roundToInt()}%";private fun Float.f1()="%.1f".format(this)
}
