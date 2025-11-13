package com.example.cinephile.domain.repository

import com.example.cinephile.ui.search.MovieUiModel
import kotlinx.coroutines.flow.Flow

interface RecommendationRepository {
    suspend fun computeRecommendations(limit: Int = 20): List<MovieUiModel>
    fun getCachedRecommendations(): Flow<List<MovieUiModel>>
    suspend fun invalidateCache()
}
