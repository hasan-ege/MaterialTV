package com.hasanege.materialtv.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApplicationContext(@ApplicationContext context: Context): Context {
        return context
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): com.hasanege.materialtv.data.AppDatabase {
        return com.hasanege.materialtv.data.AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideCategoryDao(database: com.hasanege.materialtv.data.AppDatabase): com.hasanege.materialtv.data.dao.CategoryDao {
        return database.categoryDao()
    }

    @Provides
    @Singleton
    fun provideContentDao(database: com.hasanege.materialtv.data.AppDatabase): com.hasanege.materialtv.data.dao.ContentDao {
        return database.contentDao()
    }

    @Provides
    @Singleton
    fun provideSyncMetaDao(database: com.hasanege.materialtv.data.AppDatabase): com.hasanege.materialtv.data.dao.SyncMetaDao {
        return database.syncMetaDao()
    }

    @Provides
    @Singleton
    fun provideCastDao(database: com.hasanege.materialtv.data.AppDatabase): com.hasanege.materialtv.data.dao.CastDao {
        return database.castDao()
    }

    @Provides
    @Singleton
    fun provideEpisodeDao(database: com.hasanege.materialtv.data.AppDatabase): com.hasanege.materialtv.data.dao.EpisodeDao {
        return database.episodeDao()
    }

    @Provides
    @Singleton
    fun provideUserDao(database: com.hasanege.materialtv.data.AppDatabase): com.hasanege.materialtv.data.dao.UserDao {
        return database.userDao()
    }
}
