package com.example.cinephile.ui.search

data class MovieUiModel(
    val id: Long,
    val title: String,
    val posterUrl: String?,
    val director: String?,
    val releaseDate: String?,
    val isFavorite: Boolean = false,
    val userRating: Float = 0f
)
