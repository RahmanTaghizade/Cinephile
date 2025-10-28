package com.example.cinephile.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlists")
data class WatchlistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val isCurrent: Boolean = false
)
