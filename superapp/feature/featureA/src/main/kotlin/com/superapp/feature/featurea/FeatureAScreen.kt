package com.superapp.feature.featurea

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.superapp.core.domain.model.Entity

@Composable
fun FeatureAScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: FeatureAViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is FeatureAUiEffect.NavigateToDetail -> onNavigateToDetail(effect.entityId)
                is FeatureAUiEffect.ShowError -> {}
            }
        }
    }

    FeatureAContent(
        uiState = uiState,
        onEvent = viewModel::onEvent
    )
}

@Composable
private fun FeatureAContent(
    uiState: FeatureAUiState,
    onEvent: (FeatureAUiEvent) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Feature A") })
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading && uiState.entities.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null && uiState.entities.isEmpty() -> {
                    Text(
                        text = uiState.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.entities) { entity ->
                            EntityListItem(
                                entity = entity,
                                onClick = { onEvent(FeatureAUiEvent.OnEntityClick(entity.id)) }
                            )
                        }

                        if (uiState.hasMorePages) {
                            item {
                                Button(
                                    onClick = { onEvent(FeatureAUiEvent.LoadNextPage) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Load More")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EntityListItem(
    entity: Entity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = entity.title,
                style = MaterialTheme.typography.titleMedium
            )
            entity.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Text(
                text = entity.status.name,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
