package com.example.cinephile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.cinephile.data.local.entities.RecommendedMovieEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecommendationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(recommendations: List<RecommendedMovieEntity>)

    @Query("SELECT * FROM recommended_movies ORDER BY rank ASC")
    fun observeAll(): Flow<List<RecommendedMovieEntity>>

    @Query("SELECT * FROM recommended_movies ORDER BY rank ASC")
    suspend fun getAll(): List<RecommendedMovieEntity>

    @Query("DELETE FROM recommended_movies")
    suspend fun clear()
}

