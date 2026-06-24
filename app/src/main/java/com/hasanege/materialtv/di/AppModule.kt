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
    fun provideXtreamDao(database: com.hasanege.materialtv.data.AppDatabase): com.hasanege.materialtv.data.XtreamDao {
        return database.xtreamDao()
    }
}
