package com.example.cinephile.domain.repository

import com.example.cinephile.ui.search.MovieUiModel
import kotlinx.coroutines.flow.Flow

interface WatchlistRepository {
    suspend fun getCurrentWatchlist(): Flow<WatchlistUiModel?>
    suspend fun createWatchlist(name: String): Long
    suspend fun renameWatchlist(watchlistId: Long, newName: String)
    suspend fun deleteWatchlist(watchlistId: Long)
    suspend fun setCurrentWatchlist(watchlistId: Long)
    suspend fun getAllWatchlists(): Flow<List<WatchlistUiModel>>
    suspend fun addToWatchlist(watchlistId: Long, movieId: Long)
    suspend fun removeFromWatchlist(watchlistId: Long, movieId: Long)
    suspend fun getWatchlistMovies(watchlistId: Long): Flow<List<MovieUiModel>>
    suspend fun addToCurrent(movieId: Long)
    suspend fun isMovieInWatchlist(watchlistId: Long, movieId: Long): Boolean
    suspend fun isMovieInCurrentWatchlist(movieId: Long): Boolean
}

data class WatchlistUiModel(
    val id: Long,
    val name: String,
    val isCurrent: Boolean,
    val movieCount: Int = 0
)
