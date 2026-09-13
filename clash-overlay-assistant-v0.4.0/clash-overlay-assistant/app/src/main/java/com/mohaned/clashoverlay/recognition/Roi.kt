package com.mohaned.clashoverlay.recognition

import android.graphics.Bitmap
import android.graphics.Rect
import kotlin.math.roundToInt

data class NormalizedRect(val left: Float,val top: Float,val right: Float,val bottom: Float) {
    fun toRect(width:Int,height:Int)=Rect((left*width).roundToInt().coerceIn(0,width-1),(top*height).roundToInt().coerceIn(0,height-1),(right*width).roundToInt().coerceIn(1,width),(bottom*height).roundToInt().coerceIn(1,height))
}
enum class RoiKind { OPPONENT_PLAY, OPPONENT_HAND, PLAYER_HAND, ARENA }
data class RoiSpec(val id:String,val kind:RoiKind,val rect:NormalizedRect)
object ClashRois {
    val opponentHand = RoiSpec("opponent_hand",RoiKind.OPPONENT_HAND,NormalizedRect(.08f,.015f,.92f,.225f))
    val opponentPlay = RoiSpec("opponent_play",RoiKind.OPPONENT_PLAY,NormalizedRect(.12f,.18f,.88f,.68f))
    val playerHand = RoiSpec("player_hand",RoiKind.PLAYER_HAND,NormalizedRect(.08f,.77f,.92f,.985f))
    val arena = RoiSpec("arena",RoiKind.ARENA,NormalizedRect(.04f,.08f,.96f,.78f))
    val all=listOf(opponentHand,opponentPlay,playerHand)
}
class RoiExtractor { fun extract(source:Bitmap,spec:RoiSpec):Bitmap? { val r=spec.rect.toRect(source.width,source.height); return if(r.width()>8&&r.height()>8) Bitmap.createBitmap(source,r.left,r.top,r.width(),r.height()) else null } }
