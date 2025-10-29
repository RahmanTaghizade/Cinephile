package com.example.cinephile.data.repository

import com.example.cinephile.data.local.dao.MovieDao
import com.example.cinephile.data.local.entities.MovieEntity
import com.example.cinephile.data.remote.TmdbService
import com.example.cinephile.domain.repository.MovieRepository
import com.example.cinephile.ui.search.MovieUiModel
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
        return try {
            // Call TMDB API
            val response = tmdbService.searchMovies(query = query, page = 1)
            
            // Map TMDB movies to entities and cache them
            val entities = response.results.map { tmdbMovie ->
                MovieEntity(
                    id = tmdbMovie.id,
                    title = tmdbMovie.title,
                    posterPath = tmdbMovie.posterPath,
                    overview = tmdbMovie.overview,
                    releaseDate = tmdbMovie.releaseDate,
                    directorId = null, // Will be populated on details fetch
                    directorName = null,
                    castIds = emptyList(),
                    castNames = emptyList(),
                    genreIds = tmdbMovie.genreIds,
                    keywordIds = emptyList(),
                    runtime = null,
                    lastUpdated = System.currentTimeMillis(),
                    isFavorite = false,
                    userRating = 0f
                )
            }
            
            // Cache entities
            entities.forEach { entity ->
                movieDao.upsert(entity)
            }
            
            // Map to UI models
            val uiModels = entities.map { entity ->
                entityToUiModel(entity)
            }
            
            flowOf(uiModels)
        } catch (e: Exception) {
            // Return empty list on error for now
            flowOf(emptyList())
        }
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
    
    private fun entityToUiModel(entity: MovieEntity): MovieUiModel {
        // Build poster URL
        val posterUrl = entity.posterPath?.let { path ->
            "https://image.tmdb.org/t/p/w500$path"
        }
        
        return MovieUiModel(
            id = entity.id,
            title = entity.title,
            posterUrl = posterUrl,
            director = entity.directorName,
            releaseDate = entity.releaseDate,
            isFavorite = entity.isFavorite,
            userRating = if (entity.userRating > 0f) entity.userRating else 0f
        )
    }
}
