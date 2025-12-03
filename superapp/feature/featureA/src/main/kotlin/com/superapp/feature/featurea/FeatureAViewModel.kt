package com.superapp.feature.featurea

import androidx.lifecycle.viewModelScope
import com.superapp.core.common.result.Result
import com.superapp.core.domain.usecase.entity.GetEntitiesParams
import com.superapp.core.domain.usecase.entity.GetEntitiesUseCase
import com.superapp.core.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeatureAViewModel @Inject constructor(
    private val getEntitiesUseCase: GetEntitiesUseCase
) : BaseViewModel<FeatureAUiState, FeatureAUiEvent, FeatureAUiEffect>(
    initialState = FeatureAUiState()
) {

    init {
        onEvent(FeatureAUiEvent.LoadEntities)
    }

    override fun onEvent(event: FeatureAUiEvent) {
        when (event) {
            is FeatureAUiEvent.LoadEntities -> loadEntities()
            is FeatureAUiEvent.LoadNextPage -> loadNextPage()
            is FeatureAUiEvent.Refresh -> refresh()
            is FeatureAUiEvent.OnEntityClick -> handleEntityClick(event.entityId)
        }
    }

    private fun loadEntities() {
        viewModelScope.launch {
            getEntitiesUseCase(GetEntitiesParams(page = currentState.currentPage))
                .collect { result ->
                    when (result) {
                        is Result.Loading -> updateState { copy(isLoading = true, error = null) }
                        is Result.Success -> {
                            updateState {
                                copy(
                                    entities = result.data.data,
                                    isLoading = false,
                                    hasMorePages = currentPage < result.data.pagination.totalPages
                                )
                            }
                        }
                        is Result.Error -> {
                            updateState { copy(isLoading = false, error = result.message) }
                            sendEffect(FeatureAUiEffect.ShowError(result.message ?: "Unknown error"))
                        }
                    }
                }
        }
    }

    private fun loadNextPage() {
        if (!currentState.isLoading && currentState.hasMorePages) {
            updateState { copy(currentPage = currentPage + 1) }
            loadEntities()
        }
    }

    private fun refresh() {
        updateState { copy(currentPage = 1, entities = emptyList()) }
        loadEntities()
    }

    private fun handleEntityClick(entityId: String) {
        sendEffect(FeatureAUiEffect.NavigateToDetail(entityId))
    }
}
