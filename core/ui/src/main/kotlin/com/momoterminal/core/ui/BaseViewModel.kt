package com.momoterminal.core.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

interface UiState
interface UiEvent
interface UiEffect

abstract class BaseViewModel<State : UiState, Event : UiEvent, Effect : UiEffect>(
    initialState: State
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<State> = _uiState.asStateFlow()

    private val _uiEffect = Channel<Effect>()
    val uiEffect = _uiEffect.receiveAsFlow()

    protected val currentState: State get() = _uiState.value

    abstract fun onEvent(event: Event)

    protected fun updateState(reducer: State.() -> State) {
        _uiState.value = currentState.reducer()
    }

    protected fun sendEffect(effect: Effect) {
        viewModelScope.launch { _uiEffect.send(effect) }
    }
}
