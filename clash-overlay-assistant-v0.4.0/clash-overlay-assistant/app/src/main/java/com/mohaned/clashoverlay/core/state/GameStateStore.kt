package com.mohaned.clashoverlay.core.state

import com.mohaned.clashoverlay.core.model.GameState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameStateStore { private val _state = MutableStateFlow(GameState(matchId = "idle")); val state: StateFlow<GameState> = _state.asStateFlow(); fun set(s: GameState) { _state.value = s }; fun reset(id: String) { _state.value = GameState(id, running = true, lastUpdatedMs = System.currentTimeMillis()) } }
