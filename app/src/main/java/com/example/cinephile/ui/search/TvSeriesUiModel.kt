package com.example.cinephile.ui.search

import com.example.cinephile.data.remote.TmdbTvSeries

data class TvSeriesUiModel(
    val id: Long,
    val name: String,
    val posterUrl: String?,
    val firstAirDate: String?,
    val overview: String? = null,
    val genres: List<String> = emptyList(),
    val voteAverage: Double = 0.0
)

fun TmdbTvSeries.toUiModel(genreResolver: (Int) -> String?): TvSeriesUiModel {
    val posterUrl = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
    val genreNames = genreIds.mapNotNull(genreResolver)

    return TvSeriesUiModel(
        id = id,
        name = name,
        posterUrl = posterUrl,
        firstAirDate = firstAirDate,
        overview = overview,
        genres = genreNames,
        voteAverage = voteAverage
    )
}

