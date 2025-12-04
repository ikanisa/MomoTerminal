package com.momoterminal.core.domain.repository

import com.momoterminal.core.common.Result
import com.momoterminal.core.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun getCurrentUser(): Flow<User?>
    suspend fun sendOtp(phone: String): Result<Unit>
    suspend fun verifyOtp(phone: String, code: String): Result<User>
    suspend fun signOut(): Result<Unit>
    fun isAuthenticated(): Flow<Boolean>
    suspend fun refreshToken(): Boolean
    fun logout()
}
