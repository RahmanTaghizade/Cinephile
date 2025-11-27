package com.example.cinephile.data.repository

import android.util.Log
import com.example.cinephile.data.local.dao.WatchlistDao
import com.example.cinephile.data.local.dao.MovieDao
import com.example.cinephile.data.local.entities.MovieEntity
import com.example.cinephile.data.local.entities.WatchlistEntity
import com.example.cinephile.data.local.entities.WatchlistMovieCrossRef
import com.example.cinephile.domain.repository.WatchlistRepository
import com.example.cinephile.domain.repository.WatchlistUiModel
import com.example.cinephile.domain.repository.MovieRepository
import com.example.cinephile.ui.search.MovieUiModel
import com.example.cinephile.data.remote.TmdbService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchlistRepositoryImpl @Inject constructor(
    private val watchlistDao: WatchlistDao,
    private val movieDao: MovieDao,
    private val movieRepository: MovieRepository,
    private val tmdbService: TmdbService
) : WatchlistRepository {

    companion object {
        private const val TAG = "WatchlistMonitoring"
        // Offset series IDs by 1 billion to avoid conflicts with movie IDs
        // TMDB movies and series can have overlapping IDs
        private const val SERIES_ID_OFFSET = 1_000_000_000L
        
        fun getSeriesStorageId(seriesId: Long): Long = seriesId + SERIES_ID_OFFSET
        fun getSeriesOriginalId(storageId: Long): Long = storageId - SERIES_ID_OFFSET
        fun isSeriesId(storageId: Long): Boolean = storageId >= SERIES_ID_OFFSET
    }

    override suspend fun getCurrentWatchlist(): Flow<WatchlistUiModel?> {
        Log.d(TAG, "getCurrentWatchlist: Called")
        val existing = watchlistDao.getCurrent()
        if (existing == null) {
            Log.d(TAG, "getCurrentWatchlist: No current watchlist, checking for existing watchlists")
            val all = watchlistDao.getAllOnce()
            if (all.isNotEmpty()) {
                Log.d(TAG, "getCurrentWatchlist: Setting first watchlist '${all.first().name}' as current")
                watchlistDao.setCurrentWatchlist(all.first().id)
            } else {
                Log.d(TAG, "getCurrentWatchlist: No watchlists exist! Creating default 'Favorites' watchlist")
                val favoritesId = createWatchlist("Favorites")
                watchlistDao.setCurrentWatchlist(favoritesId)
                Log.d(TAG, "getCurrentWatchlist: Created and set 'Favorites' watchlist (id=$favoritesId) as current")
            }
        } else {
            Log.d(TAG, "getCurrentWatchlist: Current watchlist is '${existing.name}' (id=${existing.id})")
        }
        
        return watchlistDao.observeCurrent().flatMapLatest { entity ->
            if (entity == null) {
                Log.d(TAG, "getCurrentWatchlist: No current watchlist")
                flowOf(null)
            } else {
                watchlistDao.observeMovieCount(entity.id).map { count ->
                    val movieIds = watchlistDao.getMovieIds(entity.id)
                    Log.d(TAG, "getCurrentWatchlist: Watchlist '${entity.name}' has $count movies. IDs: $movieIds")
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
        Log.d(TAG, "deleteWatchlist: watchlistId=$watchlistId")
        val toDelete = watchlistDao.getById(watchlistId) ?: run {
            Log.w(TAG, "deleteWatchlist: Watchlist $watchlistId not found")
            return
        }
        
        val movieCountBefore = watchlistDao.getMovieCount(watchlistId)
        Log.d(TAG, "deleteWatchlist: Deleting watchlist '${toDelete.name}' with $movieCountBefore movies")
        
        val wasCurrent = toDelete.isCurrent
        
        watchlistDao.clearWatchlistMovies(watchlistId)
        Log.d(TAG, "deleteWatchlist: Cleared $movieCountBefore movies from watchlist")
        
        watchlistDao.delete(toDelete)
        Log.d(TAG, "deleteWatchlist: Deleted watchlist $watchlistId")
        
        if (wasCurrent) {
            val all = watchlistDao.getAllOnce()
            if (all.isNotEmpty()) {
                Log.d(TAG, "deleteWatchlist: Setting '${all.first().name}' as new current watchlist")
                watchlistDao.setCurrentWatchlist(all.first().id)
            }
        }
    }

    override suspend fun setCurrentWatchlist(watchlistId: Long) {
        watchlistDao.setCurrentWatchlist(watchlistId)
    }

    override suspend fun getAllWatchlists(): Flow<List<WatchlistUiModel>> {
        Log.d(TAG, "getAllWatchlists: Called")
        
        val all = watchlistDao.getAllOnce()
        if (all.isEmpty()) {
            Log.d(TAG, "getAllWatchlists: No watchlists exist! Creating default 'Favorites' watchlist")
            val favoritesId = createWatchlist("Favorites")
            watchlistDao.setCurrentWatchlist(favoritesId)
            Log.d(TAG, "getAllWatchlists: Created and set 'Favorites' watchlist (id=$favoritesId) as current")
        }
        
        return watchlistDao.listAll().flatMapLatest { entities ->
            Log.d(TAG, "getAllWatchlists: Received ${entities.size} entities from DAO")
            flow {
                val uiModels = entities.map { entity ->
                    val movieCount = watchlistDao.getMovieCount(entity.id)
                    val movieIds = watchlistDao.getMovieIds(entity.id)
                    Log.d(TAG, "getAllWatchlists: Watchlist '${entity.name}' (id=${entity.id}) has $movieCount movies. IDs: $movieIds")
                    WatchlistUiModel(
                        id = entity.id,
                        name = entity.name,
                        isCurrent = entity.isCurrent,
                        movieCount = movieCount
                    )
                }
                Log.d(TAG, "getAllWatchlists: Emitting ${uiModels.size} UI models")
                emit(uiModels)
            }
        }
    }

    override suspend fun addToWatchlist(watchlistId: Long, movieId: Long) {
        Log.d(TAG, "addToWatchlist: watchlistId=$watchlistId, movieId=$movieId")
        
        try {
            val watchlist = watchlistDao.getById(watchlistId)
            if (watchlist == null) {
                Log.e(TAG, "addToWatchlist: Watchlist $watchlistId not found!")
                return
            }
            Log.d(TAG, "addToWatchlist: Found watchlist '${watchlist.name}'")
            
            var existingMovie = movieDao.getById(movieId)
            if (existingMovie == null) {
                Log.w(TAG, "addToWatchlist: Movie $movieId not found in database! Fetching and saving...")
                try {
                    movieRepository.getMovieDetails(movieId).first()
                    existingMovie = movieDao.getById(movieId)
                    if (existingMovie != null) {
                        Log.d(TAG, "addToWatchlist: Successfully fetched and saved movie '${existingMovie.title}'")
                    } else {
                        Log.e(TAG, "addToWatchlist: Failed to fetch movie $movieId from API")
                        Log.e(TAG, "addToWatchlist: Cannot add movie to watchlist - movie doesn't exist and couldn't be fetched")
                        return
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "addToWatchlist: Error fetching movie $movieId from API", e)
                    Log.e(TAG, "addToWatchlist: Cannot add movie to watchlist - fetch failed")
                    return
                }
            } else {
                Log.d(TAG, "addToWatchlist: Movie '${existingMovie.title}' already exists in database")
            }
            
            val currentTime = System.currentTimeMillis()
            // Update timestamp BEFORE adding to watchlist - use upsert to ensure movie exists with fresh timestamp
            val movieWithFreshTimestamp = existingMovie.copy(lastUpdated = currentTime)
            movieDao.upsert(movieWithFreshTimestamp)
            Log.d(TAG, "addToWatchlist: Updated lastUpdated timestamp for movie $movieId to $currentTime BEFORE adding to watchlist to prevent deletion")
            
            val crossRef = WatchlistMovieCrossRef(watchlistId = watchlistId, movieId = movieId)
            watchlistDao.addMovieToWatchlist(crossRef)
            
            // Refresh timestamp again AFTER adding to watchlist to ensure it's protected
            movieDao.updateLastUpdated(movieId, currentTime)
            Log.d(TAG, "addToWatchlist: Refreshed timestamp for movie $movieId after adding to watchlist")
            
            val countAfter = watchlistDao.getMovieCount(watchlistId)
            Log.d(TAG, "addToWatchlist: Successfully added movie $movieId to watchlist $watchlistId. New count: $countAfter")
        } catch (e: Exception) {
            Log.e(TAG, "addToWatchlist: Error adding movie $movieId to watchlist $watchlistId", e)
            throw e
        }
    }

    override suspend fun addSeriesToWatchlist(watchlistId: Long, seriesId: Long) {
        Log.d(TAG, "addSeriesToWatchlist: watchlistId=$watchlistId, seriesId=$seriesId")
        
        try {
            val watchlist = watchlistDao.getById(watchlistId)
            if (watchlist == null) {
                Log.e(TAG, "addSeriesToWatchlist: Watchlist $watchlistId not found!")
                return
            }
            Log.d(TAG, "addSeriesToWatchlist: Found watchlist '${watchlist.name}'")
            
            // Use offset ID to avoid conflicts with movie IDs
            val storageId = WatchlistRepositoryImpl.getSeriesStorageId(seriesId)
            Log.d(TAG, "addSeriesToWatchlist: Using storage ID $storageId for series $seriesId")
            
            var existingMovie = movieDao.getById(storageId)
            if (existingMovie == null) {
                Log.w(TAG, "addSeriesToWatchlist: Series $seriesId (storage ID $storageId) not found in database! Fetching and saving...")
                try {
                    val seriesDetails = tmdbService.getTvSeries(seriesId)
                    val seriesCredits = tmdbService.getTvCredits(seriesId)
                    
                    val director = seriesCredits.crew.firstOrNull { it.job == "Creator" || it.job == "Executive Producer" }
                    val castSorted = seriesCredits.cast.sortedBy { it.order }.take(10)
                    
                    val entity = MovieEntity(
                        id = storageId, // Use offset ID for storage
                        title = seriesDetails.name,
                        posterPath = seriesDetails.posterPath,
                        overview = seriesDetails.overview ?: "",
                        releaseDate = seriesDetails.firstAirDate,
                        directorId = director?.id,
                        directorName = director?.name,
                        castIds = castSorted.map { it.id },
                        castNames = castSorted.map { it.name },
                        genreIds = seriesDetails.genres.map { it.id },
                        keywordIds = emptyList(),
                        runtime = seriesDetails.episodeRunTime?.firstOrNull(),
                        lastUpdated = System.currentTimeMillis(),
                        isFavorite = false,
                        userRating = 0f,
                        voteAverage = seriesDetails.voteAverage
                    )
                    
                    movieDao.upsert(entity)
                    existingMovie = entity
                    Log.d(TAG, "addSeriesToWatchlist: Successfully fetched and saved series '${seriesDetails.name}' with storage ID $storageId")
                } catch (e: Exception) {
                    Log.e(TAG, "addSeriesToWatchlist: Error fetching series $seriesId from API", e)
                    Log.e(TAG, "addSeriesToWatchlist: Cannot add series to watchlist - fetch failed")
                    return
                }
            } else {
                Log.d(TAG, "addSeriesToWatchlist: Series '${existingMovie.title}' already exists in database with storage ID $storageId")
            }
            
            val currentTime = System.currentTimeMillis()
            // Update timestamp BEFORE adding to watchlist - use upsert to ensure movie exists with fresh timestamp
            val movieWithFreshTimestamp = existingMovie.copy(lastUpdated = currentTime)
            movieDao.upsert(movieWithFreshTimestamp)
            Log.d(TAG, "addSeriesToWatchlist: Updated lastUpdated timestamp for series $seriesId (storage ID $storageId) to $currentTime BEFORE adding to watchlist to prevent deletion")
            
            // Use storage ID in the cross-reference
            val crossRef = WatchlistMovieCrossRef(watchlistId = watchlistId, movieId = storageId)
            watchlistDao.addMovieToWatchlist(crossRef)
            
            // Refresh timestamp again AFTER adding to watchlist to ensure it's protected
            movieDao.updateLastUpdated(storageId, currentTime)
            Log.d(TAG, "addSeriesToWatchlist: Refreshed timestamp for series $seriesId (storage ID $storageId) after adding to watchlist")
            
            val countAfter = watchlistDao.getMovieCount(watchlistId)
            Log.d(TAG, "addSeriesToWatchlist: Successfully added series $seriesId (storage ID $storageId) to watchlist $watchlistId. New count: $countAfter")
        } catch (e: Exception) {
            Log.e(TAG, "addSeriesToWatchlist: Error adding series $seriesId to watchlist $watchlistId", e)
            throw e
        }
    }

    override suspend fun removeFromWatchlist(watchlistId: Long, movieId: Long) {
        Log.d(TAG, "removeFromWatchlist: watchlistId=$watchlistId, movieId=$movieId")
        try {
            val countBefore = watchlistDao.getMovieCount(watchlistId)
            
            // Check if this ID exists in the watchlist (could be movie or series display ID)
            // If it's a series display ID, we need to use the storage ID
            val storageId = if (watchlistDao.isMovieInWatchlist(watchlistId, movieId)) {
                movieId // It's a movie ID
            } else {
                // Try as series storage ID
                val seriesStorageId = WatchlistRepositoryImpl.getSeriesStorageId(movieId)
                if (watchlistDao.isMovieInWatchlist(watchlistId, seriesStorageId)) {
                    seriesStorageId
                } else {
                    movieId // Fallback to original ID
                }
            }
            
            watchlistDao.removeMovieFromWatchlist(watchlistId, storageId)
            val countAfter = watchlistDao.getMovieCount(watchlistId)
            Log.d(TAG, "removeFromWatchlist: Removed item $movieId (storage ID $storageId) from watchlist $watchlistId. Count: $countBefore -> $countAfter")
        } catch (e: Exception) {
            Log.e(TAG, "removeFromWatchlist: Error removing item $movieId from watchlist $watchlistId", e)
            throw e
        }
    }

    override suspend fun getWatchlistMovies(watchlistId: Long): Flow<List<MovieUiModel>> {
        Log.d(TAG, "getWatchlistMovies: watchlistId=$watchlistId")
        
        val currentTime = System.currentTimeMillis()
        movieDao.refreshWatchlistMoviesTimestamp(currentTime)
        Log.d(TAG, "getWatchlistMovies: Refreshed lastUpdated timestamp for all movies in watchlists to prevent deletion")
        
        return watchlistDao.observeMovieIds(watchlistId).flatMapLatest { ids: List<Long> ->
            Log.d(TAG, "getWatchlistMovies: watchlistId=$watchlistId, found ${ids.size} movie IDs: $ids")
            if (ids.isEmpty()) {
                Log.d(TAG, "getWatchlistMovies: watchlistId=$watchlistId, no movies found")
                flowOf(emptyList())
            } else {
                movieDao.observeByIds(ids).map { list ->
                    Log.d(TAG, "getWatchlistMovies: watchlistId=$watchlistId, loaded ${list.size} movies from database (expected ${ids.size})")
                    if (list.size < ids.size) {
                        val missingIds = ids.filter { id -> list.none { it.id == id } }
                        Log.e(TAG, "getWatchlistMovies: CRITICAL ERROR - Missing ${missingIds.size} movies in database: $missingIds")
                        Log.e(TAG, "getWatchlistMovies: This indicates movies were deleted from movies table, causing CASCADE deletion from watchlist")
                        Log.e(TAG, "getWatchlistMovies: Movies should NEVER be deleted if they're in watchlists!")
                    }
                    list.map { entityToUiModel(it) }
                }
            }
        }
        }

    override suspend fun addToCurrent(movieId: Long) {
        Log.d(TAG, "addToCurrent: movieId=$movieId")
        val current = watchlistDao.getCurrent()
        if (current == null) {
            Log.w(TAG, "addToCurrent: No current watchlist found!")
            return
        }
        Log.d(TAG, "addToCurrent: Current watchlist is '${current.name}' (id=${current.id})")
        addToWatchlist(current.id, movieId)
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
    // Convert series storage ID back to original series ID for UI
    val isSeries = WatchlistRepositoryImpl.isSeriesId(entity.id)
    val displayId = if (isSeries) {
        WatchlistRepositoryImpl.getSeriesOriginalId(entity.id)
    } else {
        entity.id
    }
    return MovieUiModel(
        id = displayId,
        title = entity.title,
        posterUrl = posterUrl,
        director = entity.directorName,
        releaseDate = entity.releaseDate,
        isFavorite = entity.isFavorite,
        userRating = if (entity.userRating > 0f) entity.userRating else 0f,
        isSeries = isSeries
    )
}
