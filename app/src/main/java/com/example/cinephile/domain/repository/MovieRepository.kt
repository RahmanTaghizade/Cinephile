package com.example.cinephile.domain.repository

import com.example.cinephile.ui.search.MovieUiModel
import kotlinx.coroutines.flow.Flow

data class MovieFilters(
    val query: String? = null,
    val primaryReleaseYear: Int? = null,
    val genreIds: List<Int> = emptyList(),
    val actorIds: List<Long> = emptyList(),
    val directorIds: List<Long> = emptyList()
)

data class MovieSearchResult(
    val movies: List<MovieUiModel>,
    val currentPage: Int,
    val totalPages: Int,
    val isLoading: Boolean = false,
    val isFromCache: Boolean = false,
    val cacheTimestamp: Long? = null
)

interface MovieRepository {
    suspend fun searchMovies(filters: MovieFilters, page: Int = 1): MovieSearchResult
    suspend fun getMovieDetails(movieId: Long): Flow<MovieUiModel?>
    suspend fun toggleFavorite(movieId: Long, isFavorite: Boolean)
    suspend fun rateMovie(movieId: Long, rating: Float)
    suspend fun getFavorites(): Flow<List<MovieUiModel>>
    suspend fun getRatedMovies(): Flow<List<MovieUiModel>>
    suspend fun fetchAndCacheGenres()
    fun getGenresFlow(): Flow<List<com.example.cinephile.data.local.entities.GenreEntity>>
    val tmdbService: com.example.cinephile.data.remote.TmdbService
}
