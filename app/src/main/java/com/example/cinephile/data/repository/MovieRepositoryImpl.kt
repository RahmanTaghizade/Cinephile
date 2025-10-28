package com.example.cinephile.data.repository

import com.example.cinephile.data.local.dao.MovieDao
import com.example.cinephile.data.remote.TmdbService
import com.example.cinephile.domain.repository.MovieRepository
import com.example.cinephile.domain.repository.MovieUiModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MovieRepositoryImpl @Inject constructor(
    private val movieDao: MovieDao,
    private val tmdbService: TmdbService
) : MovieRepository {

    override suspend fun searchMovies(query: String): Flow<List<MovieUiModel>> {
        // Stub implementation - return empty list for now
        return flowOf(emptyList())
    }

    override suspend fun getMovieDetails(movieId: Long): Flow<MovieUiModel?> {
        // Stub implementation - return null for now
        return flowOf(null)
    }

    override suspend fun toggleFavorite(movieId: Long, isFavorite: Boolean) {
        // Stub implementation - no-op for now
    }

    override suspend fun rateMovie(movieId: Long, rating: Float) {
        // Stub implementation - no-op for now
    }

    override suspend fun getFavorites(): Flow<List<MovieUiModel>> {
        // Stub implementation - return empty list for now
        return flowOf(emptyList())
    }

    override suspend fun getRatedMovies(): Flow<List<MovieUiModel>> {
        // Stub implementation - return empty list for now
        return flowOf(emptyList())
    }

    override suspend fun getHelloMessage(): String {
        return "Hello from MovieRepository stub implementation!"
    }
}
