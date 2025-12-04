package com.momoterminal.di

import android.content.Context
import coil.ImageLoader
import com.momoterminal.core.database.dao.TransactionDao
import com.momoterminal.feature.charts.ChartDataProvider
import com.momoterminal.feature.receipt.PdfReceiptGenerator
import com.momoterminal.feature.reviews.InAppReviewManager
import com.momoterminal.feature.reviews.ReviewPromptManager
import com.momoterminal.feature.updates.InAppUpdateManager
import com.momoterminal.util.image.CoilImageLoaderFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

/**
 * Hilt module providing feature-related dependencies.
 * Includes providers for charts, receipts, updates, reviews, and image loading.
 */
@Module
@InstallIn(SingletonComponent::class)
object FeatureModule {

    /**
     * Provides ChartDataProvider for transaction analytics charts.
     */
    @Provides
    @Singleton
    fun provideChartDataProvider(
        transactionDao: TransactionDao
    ): ChartDataProvider {
        return ChartDataProvider(transactionDao)
    }

    /**
     * Provides PdfReceiptGenerator for creating PDF receipts.
     */
    @Provides
    @Singleton
    fun providePdfReceiptGenerator(
        @ApplicationContext context: Context
    ): PdfReceiptGenerator {
        return PdfReceiptGenerator(context)
    }

    /**
     * Provides InAppUpdateManager for handling Play Store updates.
     */
    @Provides
    @Singleton
    fun provideInAppUpdateManager(
        @ApplicationContext context: Context
    ): InAppUpdateManager {
        return InAppUpdateManager(context)
    }

    /**
     * Provides InAppReviewManager for handling Play Store reviews.
     */
    @Provides
    @Singleton
    fun provideInAppReviewManager(
        @ApplicationContext context: Context
    ): InAppReviewManager {
        return InAppReviewManager(context)
    }

    /**
     * Provides ReviewPromptManager for managing review prompt timing.
     */
    @Provides
    @Singleton
    fun provideReviewPromptManager(
        @ApplicationContext context: Context
    ): ReviewPromptManager {
        return ReviewPromptManager(context)
    }

    /**
     * Provides CoilImageLoaderFactory for creating image loaders.
     */
    @Provides
    @Singleton
    fun provideCoilImageLoaderFactory(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient
    ): CoilImageLoaderFactory {
        return CoilImageLoaderFactory(context, okHttpClient)
    }

    /**
     * Provides ImageLoader configured with caching and certificate pinning.
     */
    @Provides
    @Singleton
    fun provideImageLoader(
        coilImageLoaderFactory: CoilImageLoaderFactory
    ): ImageLoader {
        return coilImageLoaderFactory.create()
    }
}
