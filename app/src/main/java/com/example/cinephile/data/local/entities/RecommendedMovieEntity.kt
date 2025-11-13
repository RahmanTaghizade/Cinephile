package com.example.cinephile.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recommended_movies")
data class RecommendedMovieEntity(
    @PrimaryKey
    val movieId: Long,
    val score: Double,
    val rank: Int,
    val createdAt: Long
)

