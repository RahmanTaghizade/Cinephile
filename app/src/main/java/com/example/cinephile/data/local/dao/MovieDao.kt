package com.example.cinephile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.example.cinephile.data.local.entities.MovieEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {
    
    @Upsert
    suspend fun upsert(movie: MovieEntity)
    
    @Upsert
    suspend fun upsertAll(movies: List<MovieEntity>)
    
    @Query("SELECT * FROM movies WHERE id = :id")
    suspend fun getById(id: Long): MovieEntity?

    @Query("SELECT * FROM movies WHERE isFavorite = 1 ORDER BY lastUpdated DESC")
    suspend fun getFavoritesOnce(): List<MovieEntity>

    @Query("SELECT * FROM movies WHERE userRating > 0 ORDER BY lastUpdated DESC")
    suspend fun getRatedMoviesOnce(): List<MovieEntity>
    
    @Query("SELECT * FROM movies WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<MovieEntity>
    
    @Query("SELECT * FROM movies WHERE id IN (:ids)")
    fun observeByIds(ids: List<Long>): Flow<List<MovieEntity>>
    
    @Query("SELECT * FROM movies WHERE title LIKE '%' || :query || '%' ORDER BY lastUpdated DESC")
    suspend fun searchCached(query: String): List<MovieEntity>
    
    @Query("UPDATE movies SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)
    
    @Query("UPDATE movies SET userRating = :rating WHERE id = :id")
    suspend fun updateRating(id: Long, rating: Float)
    
    @Query("SELECT * FROM movies WHERE isFavorite = 1 ORDER BY lastUpdated DESC")
    fun observeFavorites(): Flow<List<MovieEntity>>
    
    @Query("SELECT * FROM movies WHERE userRating > 0 ORDER BY userRating DESC, lastUpdated DESC")
    fun observeRated(): Flow<List<MovieEntity>>
    
    @Query("SELECT * FROM movies ORDER BY lastUpdated DESC LIMIT :limit")
    suspend fun getRecentMovies(limit: Int = 50): List<MovieEntity>
    
    @Query("DELETE FROM movies WHERE lastUpdated < :cutoffTime AND id NOT IN (SELECT DISTINCT movieId FROM watchlist_movies) AND isFavorite = 0 AND userRating = 0")
    suspend fun deleteOldMovies(cutoffTime: Long)
    
    @Query("UPDATE movies SET lastUpdated = :timestamp WHERE id IN (SELECT DISTINCT movieId FROM watchlist_movies)")
    suspend fun refreshWatchlistMoviesTimestamp(timestamp: Long)
    
    @Query("UPDATE movies SET lastUpdated = :timestamp WHERE id = :movieId")
    suspend fun updateLastUpdated(movieId: Long, timestamp: Long)

    
    @Query("SELECT id, isFavorite, userRating FROM movies")
    fun observeUserFlags(): Flow<List<MovieUserFlags>>
}

data class MovieUserFlags(
    val id: Long,
    val isFavorite: Boolean,
    val userRating: Float
)
