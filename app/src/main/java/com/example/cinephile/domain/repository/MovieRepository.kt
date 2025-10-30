package com.example.cinephile.domain.repository

import com.example.cinephile.ui.search.MovieUiModel
import kotlinx.coroutines.flow.Flow

interface MovieRepository {
    suspend fun searchMovies(query: String): Flow<List<MovieUiModel>>
    suspend fun getMovieDetails(movieId: Long): Flow<MovieUiModel?>
    suspend fun toggleFavorite(movieId: Long, isFavorite: Boolean)
    suspend fun rateMovie(movieId: Long, rating: Float)
    suspend fun getFavorites(): Flow<List<MovieUiModel>>
    suspend fun getRatedMovies(): Flow<List<MovieUiModel>>
    suspend fun fetchAndCacheGenres()
    fun getGenresFlow(): Flow<List<com.example.cinephile.data.local.entities.GenreEntity>>
    val tmdbService: com.example.cinephile.data.remote.TmdbService
}
