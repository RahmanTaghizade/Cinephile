package com.example.cinephile.data.repository

import com.example.cinephile.data.local.dao.WatchlistDao
import com.example.cinephile.data.local.entities.WatchlistEntity
import com.example.cinephile.data.local.entities.WatchlistMovieCrossRef
import com.example.cinephile.domain.repository.WatchlistRepository
import com.example.cinephile.domain.repository.WatchlistUiModel
import com.example.cinephile.ui.search.MovieUiModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchlistRepositoryImpl @Inject constructor(
    private val watchlistDao: WatchlistDao
) : WatchlistRepository {

    override suspend fun getCurrentWatchlist(): Flow<WatchlistUiModel?> =
        flow {
            // Ensure there is a current watchlist; create a default one if none exists
            val existing = watchlistDao.getCurrent()
            if (existing == null) {
                val id = watchlistDao.insert(WatchlistEntity(name = "My Watchlist", isCurrent = true))
                if (id > 0) {
                    watchlistDao.setCurrentWatchlist(id)
                }
            }
            emitAllSafe()
        }
        .map { entity ->
            entity?.let {
                WatchlistUiModel(
                    id = it.id,
                    name = it.name,
                    isCurrent = it.isCurrent,
                    movieCount = 0 // Could be populated with a count observer if needed
                )
            }
        }

    private suspend fun FlowCollector<WatchlistEntity?>.emitAllSafe() {
        // Delegate to DAO observable after ensuring existence
        watchlistDao.observeCurrent().collect { value -> emit(value) }
    }

    override suspend fun createWatchlist(name: String): Long {
        val id = watchlistDao.insert(WatchlistEntity(name = name, isCurrent = false))
        return id
    }

    override suspend fun renameWatchlist(watchlistId: Long, newName: String) {
        val current = watchlistDao.getCurrent()
        if (current != null && current.id == watchlistId) {
            watchlistDao.update(current.copy(name = newName))
        } else {
            // Fallback: update by id if different current
            val entity = WatchlistEntity(id = watchlistId, name = newName, isCurrent = false)
            watchlistDao.update(entity)
        }
    }

    override suspend fun deleteWatchlist(watchlistId: Long) {
        val current = watchlistDao.getCurrent()
        if (current != null && current.id == watchlistId) {
            watchlistDao.delete(current)
        } else {
            // Delete by id (minimal implementation)
            watchlistDao.delete(WatchlistEntity(id = watchlistId, name = "", isCurrent = false))
        }
    }

    override suspend fun setCurrentWatchlist(watchlistId: Long) {
        watchlistDao.setCurrentWatchlist(watchlistId)
    }

    override suspend fun getAllWatchlists(): Flow<List<WatchlistUiModel>> {
        return watchlistDao.listAll().map { list ->
            list.map {
                WatchlistUiModel(
                    id = it.id,
                    name = it.name,
                    isCurrent = it.isCurrent
                )
            }
        }
    }

    override suspend fun addToWatchlist(watchlistId: Long, movieId: Long) {
        watchlistDao.addMovieToWatchlist(WatchlistMovieCrossRef(watchlistId = watchlistId, movieId = movieId))
    }

    override suspend fun removeFromWatchlist(watchlistId: Long, movieId: Long) {
        watchlistDao.removeMovieFromWatchlist(watchlistId, movieId)
    }

    override suspend fun getWatchlistMovies(watchlistId: Long): Flow<List<MovieUiModel>> {
        // Minimal placeholder until mapping from MovieEntity -> MovieUiModel is implemented
        return kotlinx.coroutines.flow.flowOf(emptyList())
    }

    override suspend fun addToCurrent(movieId: Long) {
        val current = watchlistDao.getCurrent()
        val ensured = current ?: run {
            val id = watchlistDao.insert(WatchlistEntity(name = "My Watchlist", isCurrent = true))
            watchlistDao.setCurrentWatchlist(id)
            watchlistDao.getCurrent()
        }
        ensured?.let { addToWatchlist(it.id, movieId) }
    }

    override suspend fun isMovieInWatchlist(watchlistId: Long, movieId: Long): Boolean {
        return watchlistDao.isMovieInWatchlist(watchlistId, movieId)
    }

    override suspend fun isMovieInCurrentWatchlist(movieId: Long): Boolean {
        val current = watchlistDao.getCurrent() ?: return false
        return watchlistDao.isMovieInWatchlist(current.id, movieId)
    }
}
