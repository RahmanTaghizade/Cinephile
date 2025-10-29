package com.example.cinephile.data.repository

import com.example.cinephile.data.local.dao.WatchlistDao
import com.example.cinephile.domain.repository.WatchlistRepository
import com.example.cinephile.domain.repository.WatchlistUiModel
import com.example.cinephile.ui.search.MovieUiModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchlistRepositoryImpl @Inject constructor(
    private val watchlistDao: WatchlistDao
) : WatchlistRepository {

    override suspend fun getCurrentWatchlist(): Flow<WatchlistUiModel?> {
        // Stub implementation - return null for now
        return flowOf(null)
    }

    override suspend fun createWatchlist(name: String): Long {
        // Stub implementation - return dummy ID for now
        return 1L
    }

    override suspend fun renameWatchlist(watchlistId: Long, newName: String) {
        // Stub implementation - no-op for now
    }

    override suspend fun deleteWatchlist(watchlistId: Long) {
        // Stub implementation - no-op for now
    }

    override suspend fun setCurrentWatchlist(watchlistId: Long) {
        // Stub implementation - no-op for now
    }

    override suspend fun getAllWatchlists(): Flow<List<WatchlistUiModel>> {
        // Stub implementation - return empty list for now
        return flowOf(emptyList())
    }

    override suspend fun addToWatchlist(watchlistId: Long, movieId: Long) {
        // Stub implementation - no-op for now
    }

    override suspend fun removeFromWatchlist(watchlistId: Long, movieId: Long) {
        // Stub implementation - no-op for now
    }

    override suspend fun getWatchlistMovies(watchlistId: Long): Flow<List<MovieUiModel>> {
        // Stub implementation - return empty list for now
        return flowOf(emptyList())
    }
}
