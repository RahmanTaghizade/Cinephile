package com.example.cinephile.ui.search

data class MovieUiModel(
    val id: Long,
    val title: String,
    val posterUrl: String?,
    val director: String?,
    val releaseDate: String?,
    val overview: String? = null,
    val genres: List<String> = emptyList(),
    val voteAverage: Double = 0.0,
    val cast: List<CastMember> = emptyList(),
    val isFavorite: Boolean = false,
    val userRating: Float = 0f,
    val isSeries: Boolean = false
)

data class CastMember(
    val id: Long,
    val name: String,
    val character: String,
    val profileImageUrl: String? = null
)
