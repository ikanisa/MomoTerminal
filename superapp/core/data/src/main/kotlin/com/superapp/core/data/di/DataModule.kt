package com.superapp.core.data.di

import com.superapp.core.data.repository.EntityRepositoryImpl
import com.superapp.core.data.source.*
import com.superapp.core.domain.repository.EntityRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindEntityRepository(impl: EntityRepositoryImpl): EntityRepository

    @Binds
    abstract fun bindEntityRemoteDataSource(impl: EntityRemoteDataSourceImpl): EntityRemoteDataSource

    @Binds
    abstract fun bindEntityLocalDataSource(impl: EntityLocalDataSourceImpl): EntityLocalDataSource
}
