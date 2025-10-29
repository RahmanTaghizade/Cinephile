package com.example.cinephile.domain.repository

import com.example.cinephile.ui.search.MovieUiModel
import kotlinx.coroutines.flow.Flow

interface RecommendationRepository {
    suspend fun getRecommendations(limit: Int = 20): Flow<List<MovieUiModel>>
    suspend fun refreshRecommendations()
    suspend fun getCachedRecommendations(): Flow<List<MovieUiModel>>
    suspend fun invalidateCache()
}
