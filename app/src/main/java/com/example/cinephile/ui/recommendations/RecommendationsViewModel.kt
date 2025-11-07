package com.example.cinephile.ui.recommendations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cinephile.domain.repository.MovieRepository
import com.example.cinephile.ui.search.MovieUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecommendationsViewModel @Inject constructor(
    private val movieRepository: MovieRepository
) : ViewModel() {

    private val _favorites = MutableStateFlow<List<MovieUiModel>>(emptyList())
    val favorites: StateFlow<List<MovieUiModel>> = _favorites.asStateFlow()

    init {
        loadFavorites()
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            movieRepository.getFavorites().collect { favorites ->
                _favorites.value = favorites
            }
        }
    }
}

