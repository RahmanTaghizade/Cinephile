package com.example.cinephile.data.repository

import com.example.cinephile.data.local.dao.GenreDao
import com.example.cinephile.data.local.dao.MovieDao
import com.example.cinephile.data.local.dao.RecommendationDao
import com.example.cinephile.data.local.dao.WatchlistDao
import com.example.cinephile.data.local.entities.GenreEntity
import com.example.cinephile.data.local.entities.MovieEntity
import com.example.cinephile.data.local.entities.RecommendedMovieEntity
import com.example.cinephile.data.remote.TmdbMovie
import com.example.cinephile.data.remote.TmdbService
import com.example.cinephile.domain.model.MovieContentVector
import com.example.cinephile.domain.repository.RecommendationRepository
import com.example.cinephile.ui.search.MovieUiModel
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.math.max

@Singleton
class RecommendationRepositoryImpl @Inject constructor(
    private val movieDao: MovieDao,
    private val genreDao: GenreDao,
    private val watchlistDao: WatchlistDao,
    private val recommendationDao: RecommendationDao,
    private val tmdbService: TmdbService
) : RecommendationRepository {

    override fun getCachedRecommendations(): Flow<List<MovieUiModel>> {
        return recommendationDao.observeAll()
            .flatMapLatest { recommended ->
                if (recommended.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    val ordered = recommended.sortedBy { it.rank }
                    combine(
                        movieDao.observeByIds(ordered.map { it.movieId }),
                        genreDao.getAll()
                    ) { movies, genres ->
                        val moviesById = movies.associateBy { it.id }
                        ordered.mapNotNull { entry ->
                            moviesById[entry.movieId]?.let { entity ->
                                entityToUiModel(entity, genres)
                            }
                        }
                    }
                }
            }
    }

    override suspend fun computeRecommendations(limit: Int): List<MovieUiModel> {
        return withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val builder = MovieContentVectorBuilder()

            val profile = buildUserProfile(builder)
            val seeds = profile.seeds
            val excludedIds = buildExcludedMovieIds(profile)

            val candidateSummaries = fetchCandidateSummaries(seeds, excludedIds)

            val candidateEntities = mutableListOf<MovieEntity>()
            val baselineScores = mutableMapOf<Long, Double>()
            for (summary in candidateSummaries) {
                if (summary.id in excludedIds) continue
                val entity = ensureDetailedEntity(summary.id)
                if (entity != null) {
                    candidateEntities.add(entity)
                    baselineScores[summary.id] = summary.baseScore
                }
            }

            val scoredRecommendations = if (seeds.isEmpty()) {
                fallbackScores(candidateEntities, baselineScores, limit)
            } else {
                val scored = scoreCandidates(builder, seeds, candidateEntities)
                if (scored.size >= limit) {
                    scored.take(limit)
                } else {
                    val remaining = limit - scored.size
                    val fallback = candidateEntities
                        .filterNot { candidate -> scored.any { it.entity.id == candidate.id } }
                        .let { fallbackScores(it, baselineScores, remaining) }
                    (scored + fallback).take(limit)
                }
            }

            cacheRecommendations(scoredRecommendations, now)

            val genres = genreDao.getAllOnce()
            scoredRecommendations.mapNotNull { entityToUiModel(it.entity, genres) }
        }
    }

    override suspend fun invalidateCache() {
        withContext(Dispatchers.IO) {
            recommendationDao.clear()
        }
    }

    private suspend fun buildUserProfile(
        builder: MovieContentVectorBuilder
    ): UserProfile {
        val seeds = LinkedHashMap<Long, SeedMovie>()

        val favoritesRaw = movieDao.getFavoritesOnce()
        val ratedRaw = movieDao.getRatedMoviesOnce()

        val favoriteEntities = favoritesRaw.mapNotNull { ensureDetailedEntity(it.id, it) }
        val ratedEntities = ratedRaw.mapNotNull { ensureDetailedEntity(it.id, it) }

        favoriteEntities.forEach { entity ->
            val vector = builder.compute(entity)
            seeds[entity.id] = SeedMovie(
                entity = entity,
                weight = FAVORITE_WEIGHT,
                features = activeFeatures(vector)
            )
        }

        ratedEntities.forEach { entity ->
            val ratingWeight = max(entity.userRating.toDouble() / 5.0, 0.0)
            if (ratingWeight == 0.0) return@forEach

            val vector = builder.compute(entity)
            val features = activeFeatures(vector)
            val existing = seeds[entity.id]
            if (existing != null) {
                seeds[entity.id] = existing.copy(
                    weight = existing.weight + ratingWeight,
                    features = features
                )
            } else {
                seeds[entity.id] = SeedMovie(
                    entity = entity,
                    weight = ratingWeight,
                    features = features
                )
            }
        }

        val seedList = seeds.values
            .filter { it.weight > 0.0 && it.features.isNotEmpty() }

        return UserProfile(
            seeds = seedList,
            favoriteIds = favoritesRaw.map { it.id }.toSet(),
            ratedIds = ratedRaw.map { it.id }.toSet()
        )
    }

    private suspend fun buildExcludedMovieIds(profile: UserProfile): MutableSet<Long> {
        val excluded = mutableSetOf<Long>()
        excluded.addAll(profile.favoriteIds)
        excluded.addAll(profile.ratedIds)
        excluded.addAll(watchlistDao.getAllMovieIds())
        return excluded
    }

    private suspend fun fetchCandidateSummaries(
        seeds: List<SeedMovie>,
        excluded: Set<Long>
    ): List<CandidateSummary> {
        val summaries = LinkedHashMap<Long, CandidateSummary>()

        fun absorb(movies: List<TmdbMovie>) {
            movies.forEach { tmdbMovie ->
                if (tmdbMovie.id !in excluded) {
                    summaries.putIfAbsent(
                        tmdbMovie.id,
                        CandidateSummary(tmdbMovie.id, tmdbMovie.voteAverage)
                    )
                }
            }
        }

        try {
            absorb(tmdbService.getPopularMovies().results)
        } catch (_: Exception) {
        }

        try {
            absorb(tmdbService.getTrendingMovies().results)
        } catch (_: Exception) {
        }

        val topGenres = topGenreIds(seeds, MAX_DISCOVER_GENRES)
        if (topGenres.isNotEmpty()) {
            val genreString = topGenres.joinToString(",")
            try {
                absorb(tmdbService.discoverMovies(genreIds = genreString).results)
            } catch (_: Exception) {
            }
        }

        return summaries.values.toList()
    }

    private suspend fun ensureDetailedEntity(
        movieId: Long,
        cached: MovieEntity? = null
    ): MovieEntity? {
        val existing = cached ?: movieDao.getById(movieId)
        val needsRefresh = existing == null ||
            existing.castIds.isEmpty() ||
            existing.keywordIds.isEmpty() ||
            existing.genreIds.isEmpty()

        if (!needsRefresh) return existing

        return try {
            val details = tmdbService.getMovie(movieId)
            val credits = tmdbService.getCredits(movieId)
            val keywords = tmdbService.getKeywords(movieId)

            val director = credits.crew.firstOrNull { it.job == "Director" }
            val castSorted = credits.cast.sortedBy { it.order }.take(10)
            val merged = MovieEntity(
                id = details.id,
                title = details.title,
                posterPath = details.posterPath,
                overview = details.overview,
                releaseDate = details.releaseDate,
                directorId = director?.id,
                directorName = director?.name,
                castIds = castSorted.map { it.id },
                castNames = castSorted.map { it.name },
                genreIds = details.genres.map { it.id },
                keywordIds = keywords.keywords.map { it.id },
                runtime = details.runtime,
                lastUpdated = System.currentTimeMillis(),
                isFavorite = existing?.isFavorite ?: false,
                userRating = existing?.userRating ?: 0f
            )
            movieDao.upsert(merged)
            merged
        } catch (_: Exception) {
            existing
        }
    }

    private fun scoreCandidates(
        builder: MovieContentVectorBuilder,
        seeds: List<SeedMovie>,
        candidates: List<MovieEntity>
    ): List<CandidateScore> {
        return candidates.mapNotNull { candidate ->
            val vector = builder.compute(candidate)
            val candidateFeatures = activeFeatures(vector)
            if (candidateFeatures.isEmpty()) return@mapNotNull null
            val score = seeds.sumOf { seed ->
                seed.weight * similarity(seed.features, candidateFeatures)
            }
            if (score > 0.0) CandidateScore(candidate, score) else null
        }.sortedByDescending { it.score }
    }

    private fun fallbackScores(
        candidates: List<MovieEntity>,
        baselines: Map<Long, Double>,
        limit: Int
    ): List<CandidateScore> {
        if (limit <= 0) return emptyList()
        return candidates
            .mapNotNull { entity ->
                val baseline = baselines[entity.id] ?: 0.0
                CandidateScore(entity, baseline)
            }
            .sortedByDescending { it.score }
            .take(limit)
    }

    private suspend fun cacheRecommendations(
        recommendations: List<CandidateScore>,
        timestamp: Long
    ) {
        recommendationDao.clear()
        if (recommendations.isEmpty()) return
        val entities = recommendations.mapIndexed { index, entry ->
            RecommendedMovieEntity(
                movieId = entry.entity.id,
                score = entry.score,
                rank = index,
                createdAt = timestamp
            )
        }
        recommendationDao.upsertAll(entities)
    }

    private fun entityToUiModel(
        entity: MovieEntity,
        genres: List<GenreEntity>
    ): MovieUiModel {
        val posterUrl = entity.posterPath?.let { path ->
            "https://image.tmdb.org/t/p/w500$path"
        }
        val genreNames = entity.genreIds.mapNotNull { genreId ->
            genres.firstOrNull { it.id == genreId }?.name
        }
        return MovieUiModel(
            id = entity.id,
            title = entity.title,
            posterUrl = posterUrl,
            director = entity.directorName,
            releaseDate = entity.releaseDate,
            overview = entity.overview,
            genres = genreNames,
            voteAverage = 0.0,
            cast = emptyList(),
            isFavorite = entity.isFavorite,
            userRating = if (entity.userRating > 0f) entity.userRating else 0f
        )
    }

    private fun activeFeatures(vector: MovieContentVector): Set<String> {
        val active = LinkedHashSet<String>()
        vector.vector.forEachIndexed { index, flag ->
            if (flag) {
                active.add(vector.featureKeys[index])
            }
        }
        return active
    }

    private fun similarity(
        seedFeatures: Set<String>,
        candidateFeatures: Set<String>
    ): Double {
        var score = 0.0
        FEATURE_WEIGHTS.forEach { (prefix, weight) ->
            val seedSubset = seedFeatures.filter { it.startsWith(prefix) }.toSet()
            val candidateSubset = candidateFeatures.filter { it.startsWith(prefix) }.toSet()
            if (seedSubset.isEmpty() && candidateSubset.isEmpty()) {
                return@forEach
            }
            val intersection = seedSubset.intersect(candidateSubset).size.toDouble()
            val union = (seedSubset union candidateSubset).size.toDouble()
            if (union > 0) {
                score += weight * (intersection / union)
            }
        }
        return score
    }

    private fun topGenreIds(
        seeds: List<SeedMovie>,
        maxGenres: Int
    ): List<Int> {
        if (seeds.isEmpty()) return emptyList()
        val totals = mutableMapOf<Int, Double>()
        seeds.forEach { seed ->
            seed.features.forEach { feature ->
                if (feature.startsWith(GENRE_PREFIX)) {
                    val id = feature.substringAfter(GENRE_PREFIX).toIntOrNull()
                    if (id != null) {
                        totals[id] = (totals[id] ?: 0.0) + seed.weight
                    }
                }
            }
        }
        return totals.entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(maxGenres)
    }

    private suspend fun GenreDao.getAllOnce(): List<GenreEntity> {
        return getAll().first()
    }

    private data class SeedMovie(
        val entity: MovieEntity,
        val weight: Double,
        val features: Set<String>
    )

    private data class CandidateSummary(
        val id: Long,
        val baseScore: Double
    )

    private data class CandidateScore(
        val entity: MovieEntity,
        val score: Double
    )

    private data class UserProfile(
        val seeds: List<SeedMovie>,
        val favoriteIds: Set<Long>,
        val ratedIds: Set<Long>
    )

    companion object {
        private const val FAVORITE_WEIGHT = 1.0
        private const val MAX_DISCOVER_GENRES = 3

        private const val GENRE_PREFIX = "genre:"
        private const val CAST_PREFIX = "cast:"
        private const val DIRECTOR_PREFIX = "director:"
        private const val KEYWORD_PREFIX = "keyword:"

        private val FEATURE_WEIGHTS = mapOf(
            GENRE_PREFIX to 0.4,
            CAST_PREFIX to 0.25,
            DIRECTOR_PREFIX to 0.2,
            KEYWORD_PREFIX to 0.15
        )
    }
}
