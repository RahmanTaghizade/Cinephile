package com.example.cinephile.data.local.dao

import androidx.room.*
import com.example.cinephile.data.local.entities.WatchlistEntity
import com.example.cinephile.data.local.entities.WatchlistMovieCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(watchlist: WatchlistEntity): Long
    
    @Update
    suspend fun update(watchlist: WatchlistEntity)
    
    @Delete
    suspend fun delete(watchlist: WatchlistEntity)
    
    @Query("SELECT * FROM watchlists ORDER BY isCurrent DESC, name ASC")
    fun listAll(): Flow<List<WatchlistEntity>>
    
    @Query("SELECT * FROM watchlists WHERE isCurrent = 1 LIMIT 1")
    suspend fun getCurrent(): WatchlistEntity?
    
    @Query("SELECT * FROM watchlists WHERE isCurrent = 1 LIMIT 1")
    fun observeCurrent(): Flow<WatchlistEntity?>
    
    @Query("UPDATE watchlists SET isCurrent = 0")
    suspend fun clearCurrent()
    
    @Query("UPDATE watchlists SET isCurrent = 1 WHERE id = :id")
    suspend fun setCurrent(id: Long)
    
    suspend fun setCurrentWatchlist(id: Long) {
        clearCurrent()
        setCurrent(id)
    }

    @Query("SELECT COUNT(*) FROM watchlists")
    suspend fun getWatchlistCount(): Int

    @Query("SELECT * FROM watchlists ORDER BY name ASC")
    suspend fun getAllOnce(): List<WatchlistEntity>

    @Query("SELECT * FROM watchlists WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): WatchlistEntity?
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addMovieToWatchlist(crossRef: WatchlistMovieCrossRef)
    
    @Delete
    suspend fun removeMovieFromWatchlist(crossRef: WatchlistMovieCrossRef)
    
    @Query("DELETE FROM watchlist_movies WHERE watchlistId = :watchlistId AND movieId = :movieId")
    suspend fun removeMovieFromWatchlist(watchlistId: Long, movieId: Long)
    
    @Query("SELECT movieId FROM watchlist_movies WHERE watchlistId = :watchlistId")
    suspend fun getMovieIds(watchlistId: Long): List<Long>
    
    @Query("SELECT movieId FROM watchlist_movies WHERE watchlistId = :watchlistId")
    fun observeMovieIds(watchlistId: Long): Flow<List<Long>>

    @Query("SELECT movieId FROM watchlist_movies")
    suspend fun getAllMovieIds(): List<Long>
    
    @Query("SELECT COUNT(*) FROM watchlist_movies WHERE watchlistId = :watchlistId")
    suspend fun getMovieCount(watchlistId: Long): Int
    
    @Query("SELECT COUNT(*) FROM watchlist_movies WHERE watchlistId = :watchlistId")
    fun observeMovieCount(watchlistId: Long): Flow<Int>
    
    @Query("SELECT EXISTS(SELECT 1 FROM watchlist_movies WHERE watchlistId = :watchlistId AND movieId = :movieId)")
    suspend fun isMovieInWatchlist(watchlistId: Long, movieId: Long): Boolean
}
