package com.example.cinephile.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_searches")
data class CachedSearchEntity(
    @PrimaryKey
    val queryHash: String,
    val resultMovieIds: List<Long>,
    val createdAt: Long
)
