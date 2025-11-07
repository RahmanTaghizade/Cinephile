package com.example.cinephile.ui.search

import com.example.cinephile.data.remote.TmdbPerson

sealed class SearchResult {
    data class MovieItem(val movie: MovieUiModel) : SearchResult()
    data class PersonItem(val person: TmdbPerson) : SearchResult()
}

enum class SearchFilter {
    ALL, CARTOONS, SERIES, MOVIES, ACTORS, PRODUCERS
}



