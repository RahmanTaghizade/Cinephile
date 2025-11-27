package com.example.cinephile.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movies")
data class MovieEntity(
    @PrimaryKey
    val id: Long,
    val title: String,
    val posterPath: String?,
    val overview: String?,
    val releaseDate: String?,
    val directorId: Long?,
    val directorName: String?,
    val castIds: List<Long>,
    val castNames: List<String>,
    val genreIds: List<Int>,
    val keywordIds: List<Int>,
    val runtime: Int?,
    val lastUpdated: Long,
    val isFavorite: Boolean = false,
    val userRating: Float = 0f,
    val voteAverage: Double = 0.0
)
