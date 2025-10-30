package com.example.cinephile.data.repository

import com.example.cinephile.data.local.dao.MovieDao
import com.example.cinephile.data.local.dao.GenreDao
import com.example.cinephile.data.local.entities.MovieEntity
import com.example.cinephile.data.local.entities.GenreEntity
import com.example.cinephile.data.remote.TmdbService
import com.example.cinephile.domain.repository.MovieRepository
import com.example.cinephile.domain.repository.MovieFilters
import com.example.cinephile.domain.repository.MovieSearchResult
import com.example.cinephile.ui.search.MovieUiModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MovieRepositoryImpl @Inject constructor(
    private val movieDao: MovieDao,
    private val genreDao: GenreDao,
    override val tmdbService: TmdbService
) : MovieRepository {

    // Cache for director mappings (movieId -> directorId)
    private val directorCache = mutableMapOf<Long, Long?>()

    override suspend fun searchMovies(filters: MovieFilters, page: Int): MovieSearchResult {
        return withContext(Dispatchers.IO) {
            try {
                val response = if (filters.query != null) {
                    // Use search endpoint for title-based queries
                    tmdbService.searchMovies(query = filters.query, page = page)
                } else {
                    // Use discover endpoint for filtered searches
                    tmdbService.discoverMovies(
                        year = filters.primaryReleaseYear,
                        genreIds = filters.genreIds.takeIf { it.isNotEmpty() }?.joinToString(","),
                        castIds = filters.actorIds.takeIf { it.isNotEmpty() }?.joinToString(","),
                        crewIds = null, // We'll filter directors after fetching
                        page = page
                    )
                }

                // Map TMDB movies to entities
                var entities = response.results.map { tmdbMovie ->
                    MovieEntity(
                        id = tmdbMovie.id,
                        title = tmdbMovie.title,
                        posterPath = tmdbMovie.posterPath,
                        overview = tmdbMovie.overview,
                        releaseDate = tmdbMovie.releaseDate,
                        directorId = null,
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

                // Filter by directors if needed
                if (filters.directorIds.isNotEmpty()) {
                    entities = filterMoviesByDirectors(entities, filters.directorIds)
                }

                // Cache entities
                entities.forEach { entity ->
                    movieDao.upsert(entity)
                }

                // Map to UI models
                val uiModels = entities.map { entity ->
                    entityToUiModel(entity)
                }

                MovieSearchResult(
                    movies = uiModels,
                    currentPage = response.page,
                    totalPages = response.totalPages
                )
            } catch (e: Exception) {
                MovieSearchResult(
                    movies = emptyList(),
                    currentPage = page,
                    totalPages = 1,
                    isLoading = false
                )
            }
        }
    }

    private suspend fun filterMoviesByDirectors(
        entities: List<MovieEntity>,
        directorIds: List<Long>
    ): List<MovieEntity> {
        val filteredMovies = mutableListOf<MovieEntity>()
        
        for (entity in entities) {
            val directorId = getDirectorIdForMovie(entity.id)
            if (directorId in directorIds) {
                filteredMovies.add(entity.copy(directorId = directorId))
            }
        }
        
        return filteredMovies
    }

    private suspend fun getDirectorIdForMovie(movieId: Long): Long? {
        // Check cache first
        directorCache[movieId]?.let { return it }

        // Fetch from API if not cached
        return withContext(Dispatchers.IO) {
            try {
                val credits = tmdbService.getCredits(movieId)
                val director = credits.crew.firstOrNull { it.job == "Director" }
                val directorId = director?.id
                
                // Cache the result
                directorCache[movieId] = directorId
                
                directorId
            } catch (e: Exception) {
                directorCache[movieId] = null
                null
            }
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

    override suspend fun fetchAndCacheGenres() {
        return withContext(Dispatchers.IO) {
            try {
                val response = tmdbService.getGenres()
                val genres = response.genres.map { GenreEntity(it.id, it.name) }
                genreDao.upsertAll(genres)
            } catch (e: Exception) {
                // You may want to log this error for debugging.
            }
        }
    }

    override fun getGenresFlow(): Flow<List<GenreEntity>> {
        return genreDao.getAll()
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
