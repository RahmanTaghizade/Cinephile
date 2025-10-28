package com.example.cinephile.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cinephile.domain.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val movieRepository: MovieRepository
) : ViewModel() {

    private val _movies = MutableStateFlow<List<MovieUiModel>>(emptyList())
    val movies: StateFlow<List<MovieUiModel>> = _movies.asStateFlow()

    init {
        // Set dummy data for testing the grid UI
        _movies.value = getDummyMovies()
    }

    private fun getDummyMovies(): List<MovieUiModel> {
        return listOf(
            MovieUiModel(
                id = 1L,
                title = "The Matrix",
                posterUrl = "https://image.tmdb.org/t/p/w500/f89U3ADr1oiB1s9GkdPOEpXUk5H.jpg",
                director = "The Wachowskis",
                releaseDate = "1999-03-31",
                isFavorite = true,
                userRating = 4.5f
            ),
            MovieUiModel(
                id = 2L,
                title = "Inception",
                posterUrl = "https://image.tmdb.org/t/p/w500/9gk7adHYeDvHkCSEqAvQNLV5Uge.jpg",
                director = "Christopher Nolan",
                releaseDate = "2010-07-16",
                isFavorite = false,
                userRating = 4.8f
            ),
            MovieUiModel(
                id = 3L,
                title = "Interstellar",
                posterUrl = "https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg",
                director = "Christopher Nolan",
                releaseDate = "2014-11-07",
                isFavorite = true,
                userRating = 0f
            ),
            MovieUiModel(
                id = 4L,
                title = "The Godfather",
                posterUrl = "https://image.tmdb.org/t/p/w500/3bhkrj58Vtu7enYsRolD1fZdja1.jpg",
                director = "Francis Ford Coppola",
                releaseDate = "1972-03-24",
                isFavorite = false,
                userRating = 0f
            ),
            MovieUiModel(
                id = 5L,
                title = "Pulp Fiction",
                posterUrl = "https://image.tmdb.org/t/p/w500/d5iIlFn5s0ImszYzBPb8JPIfbXD.jpg",
                director = "Quentin Tarantino",
                releaseDate = "1994-10-14",
                isFavorite = false,
                userRating = 4.7f
            ),
            MovieUiModel(
                id = 6L,
                title = "The Shawshank Redemption",
                posterUrl = "https://image.tmdb.org/t/p/w500/q6y0Go1tsGEsmtFryDOJo3dEmqu.jpg",
                director = "Frank Darabont",
                releaseDate = "1994-09-23",
                isFavorite = true,
                userRating = 4.9f
            ),
            MovieUiModel(
                id = 7L,
                title = "The Dark Knight",
                posterUrl = "https://image.tmdb.org/t/p/w500/qJ2tW6WMUDux911r6m7haRef0WH.jpg",
                director = "Christopher Nolan",
                releaseDate = "2008-07-18",
                isFavorite = true,
                userRating = 4.8f
            ),
            MovieUiModel(
                id = 8L,
                title = "Fight Club",
                posterUrl = "https://image.tmdb.org/t/p/w500/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg",
                director = "David Fincher",
                releaseDate = "1999-10-15",
                isFavorite = false,
                userRating = 4.6f
            ),
            MovieUiModel(
                id = 9L,
                title = "Forrest Gump",
                posterUrl = "https://image.tmdb.org/t/p/w500/arw2vcBveWOVZr6pxd9XTd1TdQa.jpg",
                director = "Robert Zemeckis",
                releaseDate = "1994-07-06",
                isFavorite = true,
                userRating = 4.7f
            )
        )
    }
}
