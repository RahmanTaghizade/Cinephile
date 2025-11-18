package com.example.cinephile.data.repository

import com.example.cinephile.data.local.dao.WatchlistDao
import com.example.cinephile.data.local.dao.MovieDao
import com.example.cinephile.data.local.entities.MovieEntity
import com.example.cinephile.data.local.entities.WatchlistEntity
import com.example.cinephile.data.local.entities.WatchlistMovieCrossRef
import com.example.cinephile.domain.repository.WatchlistRepository
import com.example.cinephile.domain.repository.WatchlistUiModel
import com.example.cinephile.ui.search.MovieUiModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchlistRepositoryImpl @Inject constructor(
    private val watchlistDao: WatchlistDao,
    private val movieDao: MovieDao
) : WatchlistRepository {

    override suspend fun getCurrentWatchlist(): Flow<WatchlistUiModel?> {
        // Ensure there is a current watchlist; create a default one if none exists
        val existing = watchlistDao.getCurrent()
        if (existing == null) {
            val count = watchlistDao.getWatchlistCount()
            if (count == 0) {
                val id = watchlistDao.insert(WatchlistEntity(name = "My Watchlist", isCurrent = true))
                if (id > 0) {
                    watchlistDao.setCurrentWatchlist(id)
                }
            } else {
                // There are watchlists but none is current, set the first one as current
                val all = watchlistDao.getAllOnce()
                if (all.isNotEmpty()) {
                    watchlistDao.setCurrentWatchlist(all.first().id)
                }
            }
        }
        
        return watchlistDao.observeCurrent().flatMapLatest { entity ->
            if (entity == null) {
                flowOf(null)
            } else {
                watchlistDao.observeMovieCount(entity.id).map { count ->
                    WatchlistUiModel(
                        id = entity.id,
                        name = entity.name,
                        isCurrent = entity.isCurrent,
                        movieCount = count
                    )
                }
            }
        }
    }

    override suspend fun createWatchlist(name: String): Long {
        val id = watchlistDao.insert(WatchlistEntity(name = name, isCurrent = false))
        return id
    }

    override suspend fun renameWatchlist(watchlistId: Long, newName: String) {
        val entity = watchlistDao.getById(watchlistId)
        if (entity != null) {
            watchlistDao.update(entity.copy(name = newName))
        }
    }

    override suspend fun deleteWatchlist(watchlistId: Long) {
        val toDelete = watchlistDao.getById(watchlistId) ?: return
        
        val wasCurrent = toDelete.isCurrent
        
        // Clear all movies from this watchlist
        watchlistDao.clearWatchlistMovies(watchlistId)
        
        // Delete the watchlist
        watchlistDao.delete(toDelete)

        // Ensure at least one watchlist exists
        val count = watchlistDao.getWatchlistCount()
        if (count == 0) {
            val newId = watchlistDao.insert(WatchlistEntity(name = "My Watchlist", isCurrent = true))
            if (newId > 0) {
                watchlistDao.setCurrentWatchlist(newId)
            }
            return
        }

        // If we deleted the current one, promote another to be current
        if (wasCurrent) {
            val all = watchlistDao.getAllOnce()
            if (all.isNotEmpty()) {
                watchlistDao.setCurrentWatchlist(all.first().id)
            }
        }
    }

    override suspend fun setCurrentWatchlist(watchlistId: Long) {
        watchlistDao.setCurrentWatchlist(watchlistId)
    }

    override suspend fun getAllWatchlists(): Flow<List<WatchlistUiModel>> = flow {
        // Ensure at least one watchlist exists
        val count = watchlistDao.getWatchlistCount()
        if (count == 0) {
            val id = watchlistDao.insert(WatchlistEntity(name = "My Watchlist", isCurrent = true))
            if (id > 0) {
                watchlistDao.setCurrentWatchlist(id)
            }
        }
        
        // Collect from the observable flow and map each list
        watchlistDao.listAll().collect { list ->
            val uiModels = list.map { entity ->
                // Get movie count for each watchlist (suspend call is OK here in flow builder)
                val movieCount = watchlistDao.getMovieCount(entity.id)
                WatchlistUiModel(
                    id = entity.id,
                    name = entity.name,
                    isCurrent = entity.isCurrent,
                    movieCount = movieCount
                )
            }
            emit(uiModels)
        }
    }

    override suspend fun addToWatchlist(watchlistId: Long, movieId: Long) {
        watchlistDao.addMovieToWatchlist(WatchlistMovieCrossRef(watchlistId = watchlistId, movieId = movieId))
    }

    override suspend fun removeFromWatchlist(watchlistId: Long, movieId: Long) {
        watchlistDao.removeMovieFromWatchlist(watchlistId, movieId)
    }

    override suspend fun getWatchlistMovies(watchlistId: Long): Flow<List<MovieUiModel>> =
        watchlistDao.observeMovieIds(watchlistId).flatMapLatest { ids: List<Long> ->
            if (ids.isEmpty()) flowOf(emptyList())
            else movieDao.observeByIds(ids).map { list -> list.map { entityToUiModel(it) } }
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

    override fun getWatchlistById(watchlistId: Long): Flow<WatchlistUiModel?> {
        return watchlistDao.observeById(watchlistId).flatMapLatest { entity ->
            if (entity == null) {
                flowOf(null)
            } else {
                watchlistDao.observeMovieCount(entity.id).map { count ->
                    WatchlistUiModel(
                        id = entity.id,
                        name = entity.name,
                        isCurrent = entity.isCurrent,
                        movieCount = count
                    )
                }
            }
        }
    }
}

private fun entityToUiModel(entity: MovieEntity): MovieUiModel {
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
