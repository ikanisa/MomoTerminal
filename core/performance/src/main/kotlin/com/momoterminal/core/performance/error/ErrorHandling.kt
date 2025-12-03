package com.momoterminal.core.performance.error

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

// 1. GENERIC ERROR MODEL

sealed class AppError {
    data class Network(
        val code: Int? = null,
        val message: String = "Network error"
    ) : AppError()
    
    data class Timeout(
        val message: String = "Request timed out"
    ) : AppError()
    
    data class Validation(
        val field: String,
        val message: String
    ) : AppError()
    
    data class NotFound(
        val resource: String = "Resource"
    ) : AppError()
    
    data class Unauthorized(
        val message: String = "Unauthorized"
    ) : AppError()
    
    data class ServerError(
        val code: Int = 500,
        val message: String = "Server error"
    ) : AppError()
    
    data class Unknown(
        val throwable: Throwable,
        val message: String = "An error occurred"
    ) : AppError()
    
    companion object {
        fun from(throwable: Throwable): AppError = when (throwable) {
            is HttpException -> when (throwable.code()) {
                401, 403 -> Unauthorized()
                404 -> NotFound()
                in 500..599 -> ServerError(throwable.code())
                else -> Network(throwable.code())
            }
            is SocketTimeoutException -> Timeout()
            is IOException -> Network(message = "Connection failed")
            else -> Unknown(throwable)
        }
    }
    
    fun toUserMessage(): String = when (this) {
        is Network -> "Connection issue. Please check your internet."
        is Timeout -> "Request took too long. Please try again."
        is Validation -> message
        is NotFound -> "$resource not found"
        is Unauthorized -> "Please log in again"
        is ServerError -> "Server error. Please try again later."
        is Unknown -> message
    }
    
    fun isRetryable(): Boolean = when (this) {
        is Network, is Timeout, is ServerError -> true
        else -> false
    }
}

// 2. RETRY POLICY

sealed class RetryPolicy {
    data object None : RetryPolicy()
    data class Fixed(val delayMs: Long = 1000, val maxAttempts: Int = 3) : RetryPolicy()
    data class Exponential(val initialDelayMs: Long = 1000, val maxAttempts: Int = 3) : RetryPolicy()
    data class Linear(val delayMs: Long = 1000, val maxAttempts: Int = 3) : RetryPolicy()
}

@Singleton
class RetryHandler @Inject constructor() {
    
    suspend fun <T> executeWithRetry(
        policy: RetryPolicy = RetryPolicy.Exponential(),
        block: suspend () -> T
    ): T {
        var attempt = 0
        var lastException: Exception? = null
        
        while (true) {
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                attempt++
                
                val maxAttempts = when (policy) {
                    is RetryPolicy.None -> 1
                    is RetryPolicy.Fixed -> policy.maxAttempts
                    is RetryPolicy.Exponential -> policy.maxAttempts
                    is RetryPolicy.Linear -> policy.maxAttempts
                }
                
                if (attempt >= maxAttempts) {
                    throw e
                }
                
                val delayMs = when (policy) {
                    is RetryPolicy.None -> 0
                    is RetryPolicy.Fixed -> policy.delayMs
                    is RetryPolicy.Exponential -> policy.initialDelayMs * (1 shl attempt)
                    is RetryPolicy.Linear -> policy.delayMs * attempt
                }
                
                delay(delayMs)
            }
        }
    }
}

// 3. ERROR HANDLER

@Singleton
class ErrorHandler @Inject constructor(
    private val crashReporter: CrashReporter
) {
    
    fun handle(error: AppError) {
        when (error) {
            is AppError.Unknown -> {
                // Log to crash reporter
                crashReporter.recordException(error.throwable)
            }
            is AppError.ServerError -> {
                // Log server errors
                crashReporter.log("Server error: ${error.code}")
            }
            else -> {
                // Just log
                crashReporter.log("Error: ${error.toUserMessage()}")
            }
        }
    }
}

// 4. UI COMPONENTS FOR ERROR DISPLAY

@Composable
fun ErrorBanner(
    error: AppError,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Error",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = error.toUserMessage(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            
            Row {
                if (error.isRetryable() && onRetry != null) {
                    TextButton(onClick = onRetry) {
                        Text("Retry")
                    }
                }
                
                onDismiss?.let {
                    IconButton(onClick = it) {
                        Icon(Icons.Default.Close, "Dismiss")
                    }
                }
            }
        }
    }
}

@Composable
fun RetryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "Retry"
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(Icons.Default.Refresh, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text)
    }
}

@Composable
fun ErrorState(
    error: AppError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = error.toUserMessage(),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (error.isRetryable()) {
            RetryButton(onClick = onRetry)
        }
    }
}

// 5. VALIDATION ERROR DISPLAY

@Composable
fun ValidationError(
    field: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier.padding(start = 16.dp, top = 4.dp)
    )
}

// 6. USAGE IN VIEWMODEL

@HiltViewModel
class ExampleViewModel @Inject constructor(
    private val repository: ItemRepository,
    private val retryHandler: RetryHandler,
    private val errorHandler: ErrorHandler
) : BaseViewModel<ExampleUiState, ExampleEvent, ExampleEffect>(
    initialState = ExampleUiState()
) {
    
    override fun onEvent(event: ExampleEvent) {
        when (event) {
            is ExampleEvent.LoadData -> loadData()
            is ExampleEvent.Retry -> retry()
        }
    }
    
    private fun loadData() {
        viewModelScope.launch {
            updateState { copy(isLoading = true, error = null) }
            
            try {
                val data = retryHandler.executeWithRetry(
                    policy = RetryPolicy.Exponential(maxAttempts = 3)
                ) {
                    repository.getData()
                }
                
                updateState { copy(data = data, isLoading = false) }
                
            } catch (e: Exception) {
                val error = AppError.from(e)
                errorHandler.handle(error)
                updateState { copy(error = error, isLoading = false) }
            }
        }
    }
    
    private fun retry() {
        loadData()
    }
}

// 7. USAGE IN COMPOSE

@Composable
fun ExampleScreen(
    viewModel: ExampleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                uiState.error != null -> {
                    ErrorState(
                        error = uiState.error!!,
                        onRetry = { viewModel.onEvent(ExampleEvent.Retry) }
                    )
                }
                
                else -> {
                    // Success state
                    DataContent(uiState.data)
                }
            }
            
            // Error banner (non-blocking)
            uiState.error?.let { error ->
                if (!error.isRetryable()) {
                    ErrorBanner(
                        error = error,
                        onDismiss = { /* Clear error */ }
                    )
                }
            }
        }
    }
}

// 8. NETWORK ERROR INTERCEPTOR

class ErrorInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        
        return try {
            val response = chain.proceed(request)
            
            if (!response.isSuccessful) {
                // Log non-successful responses
                Log.w("Network", "Request failed: ${response.code}")
            }
            
            response
        } catch (e: Exception) {
            // Log network exceptions
            Log.e("Network", "Request exception", e)
            throw e
        }
    }
}
