package com.superapp.feature.featurea

import com.superapp.core.domain.model.Entity
import com.superapp.core.ui.base.UiEffect
import com.superapp.core.ui.base.UiEvent
import com.superapp.core.ui.base.UiState

data class FeatureAUiState(
    val entities: List<Entity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 1,
    val hasMorePages: Boolean = true
) : UiState

sealed interface FeatureAUiEvent : UiEvent {
    data object LoadEntities : FeatureAUiEvent
    data object LoadNextPage : FeatureAUiEvent
    data object Refresh : FeatureAUiEvent
    data class OnEntityClick(val entityId: String) : FeatureAUiEvent
}

sealed interface FeatureAUiEffect : UiEffect {
    data class NavigateToDetail(val entityId: String) : FeatureAUiEffect
    data class ShowError(val message: String) : FeatureAUiEffect
}
