package com.example.cinephile.data.local.dao

import androidx.room.*
import com.example.cinephile.data.local.entities.MovieEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(movie: MovieEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(movies: List<MovieEntity>)
    
    @Query("SELECT * FROM movies WHERE id = :id")
    suspend fun getById(id: Long): MovieEntity?
    
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
    
    @Query("DELETE FROM movies WHERE lastUpdated < :cutoffTime")
    suspend fun deleteOldMovies(cutoffTime: Long)

    // Observe minimal user flag updates for all movies (for badge updates in lists)
    @Query("SELECT id, isFavorite, userRating FROM movies")
    fun observeUserFlags(): Flow<List<MovieUserFlags>>
}

data class MovieUserFlags(
    val id: Long,
    val isFavorite: Boolean,
    val userRating: Float
)
