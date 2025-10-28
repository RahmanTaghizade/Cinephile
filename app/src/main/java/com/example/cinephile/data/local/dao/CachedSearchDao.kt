package com.example.cinephile.data.local.dao

import androidx.room.*
import com.example.cinephile.data.local.entities.CachedSearchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedSearchDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(cachedSearch: CachedSearchEntity)
    
    @Query("SELECT * FROM cached_searches WHERE queryHash = :hash")
    suspend fun getByHash(hash: String): CachedSearchEntity?
    
    @Query("SELECT * FROM cached_searches WHERE queryHash = :hash")
    fun observeByHash(hash: String): Flow<CachedSearchEntity?>
    
    @Query("SELECT * FROM cached_searches ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentSearches(limit: Int = 20): List<CachedSearchEntity>
    
    @Query("SELECT * FROM cached_searches ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecentSearches(limit: Int = 20): Flow<List<CachedSearchEntity>>
    
    @Query("DELETE FROM cached_searches WHERE createdAt < :cutoffTime")
    suspend fun deleteOld(cutoffTime: Long)
    
    @Query("DELETE FROM cached_searches WHERE queryHash = :hash")
    suspend fun deleteByHash(hash: String)
    
    @Query("DELETE FROM cached_searches")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM cached_searches")
    suspend fun getCount(): Int
}
