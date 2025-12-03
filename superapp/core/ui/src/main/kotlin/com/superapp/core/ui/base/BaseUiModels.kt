package com.superapp.core.ui.base

interface UiState

interface UiEvent

interface UiEffect

data class LoadingState(
    val isLoading: Boolean = false,
    val message: String? = null
)

data class ErrorState(
    val hasError: Boolean = false,
    val message: String? = null,
    val throwable: Throwable? = null
)
