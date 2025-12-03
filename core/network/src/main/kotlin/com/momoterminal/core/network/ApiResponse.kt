package com.momoterminal.core.network

import com.momoterminal.core.common.Result

data class ApiResponse<T>(
    val data: T? = null,
    val message: String? = null,
    val success: Boolean = true
)

fun <T> ApiResponse<T>.toResult(): Result<T> = when {
    success && data != null -> Result.Success(data)
    else -> Result.Error(Exception(message ?: "Unknown error"), message)
}
