package com.example.cinephile.di

import com.example.cinephile.data.repository.MovieRepositoryImpl
import com.example.cinephile.data.repository.WatchlistRepositoryImpl
import com.example.cinephile.data.repository.QuizRepositoryImpl
import com.example.cinephile.data.repository.RecommendationRepositoryImpl
import com.example.cinephile.domain.repository.MovieRepository
import com.example.cinephile.domain.repository.WatchlistRepository
import com.example.cinephile.domain.repository.QuizRepository
import com.example.cinephile.domain.repository.RecommendationRepository
import com.example.cinephile.data.repository.PersonRepositoryImpl
import com.example.cinephile.domain.repository.PersonRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMovieRepository(
        movieRepositoryImpl: MovieRepositoryImpl
    ): MovieRepository

    @Binds
    @Singleton
    abstract fun bindWatchlistRepository(
        watchlistRepositoryImpl: WatchlistRepositoryImpl
    ): WatchlistRepository

    @Binds
    @Singleton
    abstract fun bindQuizRepository(
        quizRepositoryImpl: QuizRepositoryImpl
    ): QuizRepository

    @Binds
    @Singleton
    abstract fun bindRecommendationRepository(
        recommendationRepositoryImpl: RecommendationRepositoryImpl
    ): RecommendationRepository

    @Binds
    @Singleton
    abstract fun bindPersonRepository(
        personRepositoryImpl: PersonRepositoryImpl
    ): PersonRepository
}
