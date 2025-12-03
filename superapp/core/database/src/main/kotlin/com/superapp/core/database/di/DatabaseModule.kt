package com.superapp.core.database.di

import android.content.Context
import androidx.room.Room
import com.superapp.core.database.SuperAppDatabase
import com.superapp.core.database.dao.EntityDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SuperAppDatabase {
        return Room.databaseBuilder(
            context,
            SuperAppDatabase::class.java,
            "superapp_database"
        ).build()
    }

    @Provides
    fun provideEntityDao(database: SuperAppDatabase): EntityDao {
        return database.entityDao()
    }
}
