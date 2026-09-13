package com.mohaned.clashoverlay.core.engine

import com.mohaned.clashoverlay.core.model.GameState

class StateReducer(private val tips:TipsEngine=TipsEngine()){
    fun reduce(base:GameState):GameState = base.copy(lastTip=tips.evaluate(base)?.text)
}
