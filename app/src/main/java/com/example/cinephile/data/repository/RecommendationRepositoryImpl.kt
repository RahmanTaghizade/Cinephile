package com.example.cinephile.data.repository

import com.example.cinephile.data.local.dao.MovieDao
import com.example.cinephile.data.remote.TmdbService
import com.example.cinephile.domain.repository.RecommendationRepository
import com.example.cinephile.ui.search.MovieUiModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecommendationRepositoryImpl @Inject constructor(
    private val movieDao: MovieDao,
    private val tmdbService: TmdbService
) : RecommendationRepository {

    override suspend fun getRecommendations(limit: Int): Flow<List<MovieUiModel>> {
        // Stub implementation - return empty list for now
        return flowOf(emptyList())
    }

    override suspend fun refreshRecommendations() {
        // Stub implementation - no-op for now
    }

    override suspend fun getCachedRecommendations(): Flow<List<MovieUiModel>> {
        // Stub implementation - return empty list for now
        return flowOf(emptyList())
    }

    override suspend fun invalidateCache() {
        // Stub implementation - no-op for now
    }
}
