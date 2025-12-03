package com.momoterminal.feature.wallet.di

import com.momoterminal.core.common.auth.TokenManager
import com.momoterminal.core.database.dao.TokenDao
import com.momoterminal.feature.wallet.data.WalletRepositoryImpl
import com.momoterminal.feature.wallet.domain.repository.WalletRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for wallet feature dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object WalletModule {

    @Provides
    @Singleton
    fun provideWalletRepository(
        tokenDao: TokenDao,
        tokenManager: TokenManager
    ): WalletRepository {
        val userId = tokenManager.getUserId() ?: ""
        return WalletRepositoryImpl(tokenDao, userId)
    }
}
