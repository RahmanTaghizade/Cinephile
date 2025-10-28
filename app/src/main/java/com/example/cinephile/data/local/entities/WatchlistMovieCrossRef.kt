package com.example.cinephile.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "watchlist_movies",
    primaryKeys = ["watchlistId", "movieId"],
    foreignKeys = [
        ForeignKey(
            entity = WatchlistEntity::class,
            parentColumns = ["id"],
            childColumns = ["watchlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MovieEntity::class,
            parentColumns = ["id"],
            childColumns = ["movieId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("watchlistId"), Index("movieId")]
)
data class WatchlistMovieCrossRef(
    val watchlistId: Long,
    val movieId: Long
)
