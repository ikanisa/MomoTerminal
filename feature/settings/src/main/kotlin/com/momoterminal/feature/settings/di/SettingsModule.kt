package com.momoterminal.feature.settings.di

import com.momoterminal.core.domain.usecase.settings.*
import com.momoterminal.core.domain.usecase.settings.impl.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
abstract class SettingsModule {

    @Binds
    @ViewModelScoped
    abstract fun bindGetMerchantSettingsUseCase(
        impl: GetMerchantSettingsUseCaseImpl
    ): GetMerchantSettingsUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindUpdateMerchantProfileUseCase(
        impl: UpdateMerchantProfileUseCaseImpl
    ): UpdateMerchantProfileUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindUpdateBusinessDetailsUseCase(
        impl: UpdateBusinessDetailsUseCaseImpl
    ): UpdateBusinessDetailsUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindUpdateNotificationPreferencesUseCase(
        impl: UpdateNotificationPreferencesUseCaseImpl
    ): UpdateNotificationPreferencesUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindUpdateTransactionLimitsUseCase(
        impl: UpdateTransactionLimitsUseCaseImpl
    ): UpdateTransactionLimitsUseCase

    @Binds
    @ViewModelScoped
    abstract fun bindUpdateFeatureFlagsUseCase(
        impl: UpdateFeatureFlagsUseCaseImpl
    ): UpdateFeatureFlagsUseCase
}
