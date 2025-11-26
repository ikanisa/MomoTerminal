package com.momoterminal.feature.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

/**
 * UI state for the Analytics Dashboard.
 */
sealed class AnalyticsUiState {
    data object Loading : AnalyticsUiState()
    
    data class Success(
        val summary: TransactionSummary,
        val incomeExpenseProducer: CartesianChartModelProducer,
        val volumeProducer: CartesianChartModelProducer,
        val categoryBreakdown: List<CategoryBreakdown>
    ) : AnalyticsUiState()
    
    data class Error(val message: String) : AnalyticsUiState()
}

/**
 * ViewModel for the Analytics Dashboard screen.
 */
@HiltViewModel
class AnalyticsDashboardViewModel @Inject constructor(
    private val chartDataProvider: ChartDataProvider
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<AnalyticsUiState>(AnalyticsUiState.Loading)
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()
    
    init {
        loadAnalyticsData()
    }
    
    /**
     * Load all analytics data.
     */
    fun loadAnalyticsData() {
        viewModelScope.launch {
            _uiState.value = AnalyticsUiState.Loading
            
            try {
                val summary = chartDataProvider.getTransactionSummary(7)
                val incomeExpenseProducer = chartDataProvider.buildIncomeExpenseLineChartProducer(7)
                val volumeProducer = chartDataProvider.buildTransactionVolumeColumnChartProducer(7)
                val categoryBreakdown = chartDataProvider.getCategoryBreakdown()
                
                _uiState.value = AnalyticsUiState.Success(
                    summary = summary,
                    incomeExpenseProducer = incomeExpenseProducer,
                    volumeProducer = volumeProducer,
                    categoryBreakdown = categoryBreakdown
                )
            } catch (e: Exception) {
                _uiState.value = AnalyticsUiState.Error(
                    message = e.message ?: "Failed to load analytics data"
                )
            }
        }
    }
    
    /**
     * Refresh analytics data.
     */
    fun refresh() {
        loadAnalyticsData()
    }
}

/**
 * Analytics Dashboard screen composable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsDashboardScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnalyticsDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is AnalyticsUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                is AnalyticsUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Error loading analytics",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        androidx.compose.material3.Button(
                            onClick = { viewModel.refresh() }
                        ) {
                            Text("Retry")
                        }
                    }
                }
                
                is AnalyticsUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        item {
                            TransactionSummaryCards(summary = state.summary)
                        }
                        
                        item {
                            IncomeExpenseChart(
                                modelProducer = state.incomeExpenseProducer
                            )
                        }
                        
                        item {
                            TransactionVolumeChart(
                                modelProducer = state.volumeProducer
                            )
                        }
                        
                        item {
                            CategoryBreakdownCard(
                                categories = state.categoryBreakdown
                            )
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Category breakdown card with progress indicators.
 */
@Composable
fun CategoryBreakdownCard(
    categories: List<CategoryBreakdown>,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "GH")).apply {
        maximumFractionDigits = 0
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Category Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            categories.forEach { category ->
                CategoryBreakdownItem(
                    category = category,
                    formattedAmount = currencyFormatter.format(category.amount)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

/**
 * Individual category breakdown item with progress indicator.
 */
@Composable
private fun CategoryBreakdownItem(
    category: CategoryBreakdown,
    formattedAmount: String,
    modifier: Modifier = Modifier
) {
    val color = Color(category.color)
    
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = category.category,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Text(
                text = formattedAmount,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LinearProgressIndicator(
                progress = { category.percentage },
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(MaterialTheme.shapes.small),
                color = color,
                trackColor = color.copy(alpha = 0.2f)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = "${(category.percentage * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
