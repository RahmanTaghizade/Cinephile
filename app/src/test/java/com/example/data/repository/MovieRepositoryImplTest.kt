package com.example.cinephile.data.repository

import com.example.cinephile.data.local.dao.CachedSearchDao
import com.example.cinephile.data.local.dao.GenreDao
import com.example.cinephile.data.local.dao.MovieDao
import com.example.cinephile.data.local.dao.MovieUserFlags
import com.example.cinephile.data.local.entities.CachedSearchEntity
import com.example.cinephile.data.local.entities.GenreEntity
import com.example.cinephile.data.local.entities.MovieEntity
import com.example.cinephile.data.remote.TmdbCast
import com.example.cinephile.data.remote.TmdbCredits
import com.example.cinephile.data.remote.TmdbCrew
import com.example.cinephile.data.remote.TmdbGenre
import com.example.cinephile.data.remote.TmdbKeywords
import com.example.cinephile.data.remote.TmdbMovieDetails
import com.example.cinephile.data.remote.TmdbPersonSearchResponse
import com.example.cinephile.data.remote.TmdbService
import com.google.common.truth.Truth.assertThat
import com.example.cinephile.data.remote.TmdbKeyword
import com.example.cinephile.data.remote.TmdbGenresResponse
import com.example.cinephile.data.local.dao.RecommendationDao
import com.example.cinephile.data.local.entities.RecommendedMovieEntity
import com.example.cinephile.data.remote.TmdbSearchResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MovieRepositoryImplTest {

    @Test
    fun `getMovieDetails upserts full metadata`() = runTest {
        val movieDao = FakeMovieDao()
        val genreDao = FakeGenreDao(
            listOf(
                GenreEntity(12, "Adventure"),
                GenreEntity(16, "Animation"),
                GenreEntity(18, "Drama")
            )
        )
        val cachedSearchDao = FakeCachedSearchDao()
        val tmdbService = FakeTmdbService()
        val recommendationDao = FakeRecommendationDao()
        val repository = MovieRepositoryImpl(
            movieDao = movieDao,
            genreDao = genreDao,
            cachedSearchDao = cachedSearchDao,
            recommendationDao = recommendationDao,
            tmdbService = tmdbService
        )

        val result = repository.getMovieDetails(123L).first()

        assertThat(result).isNotNull()
        val stored = movieDao.getById(123L)
        assertThat(stored).isNotNull()
        stored!!
        assertThat(stored.genreIds).containsExactly(12, 16)
        assertThat(stored.castIds).containsExactly(500L, 501L, 502L).inOrder()
        assertThat(stored.directorId).isEqualTo(9000L)
        assertThat(stored.keywordIds).containsExactly(800, 801, 802)
    }

    @Test
    fun `computeContentVector returns multi-hot representation`() = runTest {
        val movieDao = FakeMovieDao()
        val genreDao = FakeGenreDao(emptyList())
        val recommendationDao = FakeRecommendationDao()
        val repository = MovieRepositoryImpl(
            movieDao = movieDao,
            genreDao = genreDao,
            cachedSearchDao = FakeCachedSearchDao(),
            recommendationDao = recommendationDao,
            tmdbService = FakeTmdbService()
        )

        repository.getMovieDetails(123L).first()
        val vector = repository.computeContentVector(123L)

        assertThat(vector).isNotNull()
        vector!!
        val featureIndex = vector.featureKeys.withIndex().associate { it.value to it.index }
        assertThat(featureIndex).containsKey("director:9000")
        val directorIndex = featureIndex.getValue("director:9000")
        assertThat(vector.vector[directorIndex]).isTrue()
        assertThat(vector.vector.count { it }).isEqualTo(9)
    }

    @Test
    fun `toggleFavorite clears recommendation cache`() = runTest {
        val movieDao = FakeMovieDao()
        val genreDao = FakeGenreDao(emptyList())
        val recommendationDao = FakeRecommendationDao()
        val repository = MovieRepositoryImpl(
            movieDao = movieDao,
            genreDao = genreDao,
            cachedSearchDao = FakeCachedSearchDao(),
            recommendationDao = recommendationDao,
            tmdbService = FakeTmdbService()
        )

        movieDao.upsert(
            MovieEntity(
                id = 1L,
                title = "Movie",
                posterPath = null,
                overview = null,
                releaseDate = null,
                directorId = null,
                directorName = null,
                castIds = emptyList(),
                castNames = emptyList(),
                genreIds = emptyList(),
                keywordIds = emptyList(),
                runtime = null,
                lastUpdated = System.currentTimeMillis(),
                isFavorite = false,
                userRating = 0f
            )
        )

        repository.toggleFavorite(1L, true)

        assertThat(recommendationDao.cleared).isNotEmpty()
    }

    private class FakeRecommendationDao : RecommendationDao {
        val cleared = mutableListOf<Boolean>()
        val stored = mutableListOf<List<RecommendedMovieEntity>>()

        override suspend fun upsertAll(recommendations: List<RecommendedMovieEntity>) {
            stored += recommendations
        }

        override fun observeAll(): kotlinx.coroutines.flow.Flow<List<RecommendedMovieEntity>> {
            return kotlinx.coroutines.flow.MutableStateFlow(emptyList())
        }

        override suspend fun getAll(): List<RecommendedMovieEntity> = stored.lastOrNull() ?: emptyList()

        override suspend fun clear() {
            cleared += true
            stored.clear()
        }
    }

    private class FakeMovieDao : MovieDao {
        private val movies = java.util.LinkedHashMap<Long, MovieEntity>()
        private val state = MutableStateFlow<List<MovieEntity>>(emptyList())

        override suspend fun upsert(movie: MovieEntity) {
            movies[movie.id] = movie
            emit()
        }

        override suspend fun upsertAll(entities: List<MovieEntity>) {
            entities.forEach { upsert(it) }
        }

        override suspend fun getById(id: Long): MovieEntity? = movies[id]

        override suspend fun getFavoritesOnce(): List<MovieEntity> {
            return movies.values.filter { it.isFavorite }
        }

        override suspend fun getRatedMoviesOnce(): List<MovieEntity> {
            return movies.values.filter { it.userRating > 0f }
        }

        override suspend fun getByIds(ids: List<Long>): List<MovieEntity> {
            return movies.values.filter { it.id in ids }
        }

        override fun observeByIds(ids: List<Long>): Flow<List<MovieEntity>> {
            return state.map { list -> list.filter { it.id in ids } }
        }

        override suspend fun searchCached(query: String): List<MovieEntity> {
            return state.value.filter { it.title.contains(query, ignoreCase = true) }
        }

        override suspend fun updateFavorite(id: Long, isFavorite: Boolean) {
            movies[id]?.let {
                movies[id] = it.copy(isFavorite = isFavorite)
                emit()
            }
        }

        override suspend fun updateRating(id: Long, rating: Float) {
            movies[id]?.let {
                movies[id] = it.copy(userRating = rating)
                emit()
            }
        }

        override fun observeFavorites(): Flow<List<MovieEntity>> {
            return state.map { list -> list.filter { it.isFavorite } }
        }

        override fun observeRated(): Flow<List<MovieEntity>> {
            return state.map { list -> list.filter { it.userRating > 0f } }
        }

        override suspend fun getRecentMovies(limit: Int): List<MovieEntity> {
            return movies.values.sortedByDescending { it.lastUpdated }.take(limit)
        }

        override suspend fun deleteOldMovies(cutoffTime: Long) {
            val iterator = movies.entries.iterator()
            var changed = false
            while (iterator.hasNext()) {
                if (iterator.next().value.lastUpdated < cutoffTime) {
                    iterator.remove()
                    changed = true
                }
            }
            if (changed) emit()
        }

        override fun observeUserFlags(): Flow<List<MovieUserFlags>> {
            return state.map { list ->
                list.map { MovieUserFlags(it.id, it.isFavorite, it.userRating) }
            }
        }

        private fun emit() {
            state.value = movies.values.toList()
        }
    }

    private class FakeGenreDao(initial: List<GenreEntity>) : GenreDao {
        private val state = MutableStateFlow(initial.sortedBy { it.name })

        override suspend fun upsertAll(genres: List<GenreEntity>) {
            state.value = genres.sortedBy { it.name }
        }

        override fun getAll(): Flow<List<GenreEntity>> = state
    }

    private class FakeCachedSearchDao : CachedSearchDao {
        override suspend fun upsert(cachedSearch: CachedSearchEntity) = Unit
        override suspend fun getByHash(hash: String): CachedSearchEntity? = null
        override fun observeByHash(hash: String): Flow<CachedSearchEntity?> = MutableStateFlow(null)
        override suspend fun getRecentSearches(limit: Int): List<CachedSearchEntity> = emptyList()
        override fun observeRecentSearches(limit: Int): Flow<List<CachedSearchEntity>> =
            MutableStateFlow(emptyList())

        override suspend fun deleteOld(cutoffTime: Long) = Unit
        override suspend fun deleteByHash(hash: String) = Unit
        override suspend fun deleteAll() = Unit
        override suspend fun getCount(): Int = 0
    }

    private class FakeTmdbService : TmdbService {
        override suspend fun searchMovies(query: String, page: Int): TmdbSearchResponse {
            throw NotImplementedError()
        }

        override suspend fun discoverMovies(
            year: Int?,
            genreIds: String?,
            castIds: String?,
            crewIds: String?,
            page: Int
        ): TmdbSearchResponse {
            throw NotImplementedError()
        }

        override suspend fun getMovie(movieId: Long): TmdbMovieDetails {
            return TmdbMovieDetails(
                id = movieId,
                title = "Sample Movie",
                posterPath = "/poster.jpg",
                overview = "Overview",
                releaseDate = "2024-01-01",
                runtime = 130,
                voteAverage = 8.5,
                genres = listOf(
                    TmdbGenre(id = 12, name = "Adventure"),
                    TmdbGenre(id = 16, name = "Animation")
                )
            )
        }

        override suspend fun getCredits(movieId: Long): TmdbCredits {
            return TmdbCredits(
                id = movieId,
                cast = listOf(
                    TmdbCast(id = 500L, name = "Actor A", character = "Hero", profilePath = null, order = 0),
                    TmdbCast(id = 501L, name = "Actor B", character = "Sidekick", profilePath = null, order = 1),
                    TmdbCast(id = 502L, name = "Actor C", character = "Villain", profilePath = null, order = 2)
                ),
                crew = listOf(
                    TmdbCrew(id = 9000L, name = "Director X", job = "Director")
                )
            )
        }

        override suspend fun getKeywords(movieId: Long): TmdbKeywords {
            return TmdbKeywords(
                id = movieId,
                keywords = listOf(
                    TmdbKeyword(800, "keyword 1"),
                    TmdbKeyword(801, "keyword 2"),
                    TmdbKeyword(802, "keyword 3")
                )
            )
        }

        override suspend fun searchPerson(query: String, page: Int): TmdbPersonSearchResponse {
            throw NotImplementedError()
        }

        override suspend fun getGenres(): TmdbGenresResponse {
            throw NotImplementedError()
        }

        override suspend fun getPopularMovies(page: Int): TmdbSearchResponse {
            throw NotImplementedError()
        }

        override suspend fun getTrendingMovies(page: Int): TmdbSearchResponse {
            throw NotImplementedError()
        }
    }
}

