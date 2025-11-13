package com.example.cinephile.di

import android.content.Context
import com.example.cinephile.data.local.CinephileDb
import com.example.cinephile.data.local.dao.*
import com.example.cinephile.data.local.converters.ListConverters
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
    fun provideCinephileDb(@ApplicationContext context: Context): CinephileDb {
        return CinephileDb.create(context)
    }
    
    @Provides
    fun provideMovieDao(database: CinephileDb): MovieDao {
        return database.movieDao()
    }
    
    @Provides
    fun provideWatchlistDao(database: CinephileDb): WatchlistDao {
        return database.watchlistDao()
    }
    
    @Provides
    fun provideQuizDao(database: CinephileDb): QuizDao {
        return database.quizDao()
    }
    
    @Provides
    fun provideCachedSearchDao(database: CinephileDb): CachedSearchDao {
        return database.cachedSearchDao()
    }

    @Provides
    fun provideGenreDao(database: CinephileDb): GenreDao {
        return database.genreDao()
    }

    @Provides
    @Singleton
    fun provideListConverters(): ListConverters {
        return ListConverters()
    }
}
