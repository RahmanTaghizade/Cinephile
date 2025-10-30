package com.example.cinephile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.cinephile.data.local.entities.GenreEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GenreDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(genres: List<GenreEntity>)

    @Query("SELECT * FROM genres ORDER BY name ASC")
    fun getAll(): Flow<List<GenreEntity>>
}
