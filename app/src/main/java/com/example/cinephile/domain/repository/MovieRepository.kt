package com.example.cinephile.domain.repository

import kotlinx.coroutines.flow.Flow

interface MovieRepository {
    suspend fun searchMovies(query: String): Flow<List<MovieUiModel>>
    suspend fun getMovieDetails(movieId: Long): Flow<MovieUiModel?>
    suspend fun toggleFavorite(movieId: Long, isFavorite: Boolean)
    suspend fun rateMovie(movieId: Long, rating: Float)
    suspend fun getFavorites(): Flow<List<MovieUiModel>>
    suspend fun getRatedMovies(): Flow<List<MovieUiModel>>
    suspend fun getHelloMessage(): String
}

data class MovieUiModel(
    val id: Long,
    val title: String,
    val posterUrl: String?,
    val director: String?,
    val releaseDate: String?,
    val isFavorite: Boolean = false,
    val userRating: Float? = null
)
