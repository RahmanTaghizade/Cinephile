package com.example.cinephile.data.repository

import com.example.cinephile.data.local.dao.MovieDao
import com.example.cinephile.data.local.dao.MovieUserFlags
import com.example.cinephile.data.local.dao.RecommendationDao
import com.example.cinephile.data.local.dao.WatchlistDao
import com.example.cinephile.data.local.dao.GenreDao
import com.example.cinephile.data.local.entities.GenreEntity
import com.example.cinephile.data.local.entities.MovieEntity
import com.example.cinephile.data.local.entities.RecommendedMovieEntity
import com.example.cinephile.data.local.entities.WatchlistEntity
import com.example.cinephile.data.local.entities.WatchlistMovieCrossRef
import com.example.cinephile.data.remote.TmdbCast
import com.example.cinephile.data.remote.TmdbCredits
import com.example.cinephile.data.remote.TmdbCrew
import com.example.cinephile.data.remote.TmdbGenre
import com.example.cinephile.data.remote.TmdbGenresResponse
import com.example.cinephile.data.remote.TmdbKeyword
import com.example.cinephile.data.remote.TmdbKeywords
import com.example.cinephile.data.remote.TmdbMovie
import com.example.cinephile.data.remote.TmdbMovieDetails
import com.example.cinephile.data.remote.TmdbSearchResponse
import com.example.cinephile.data.remote.TmdbService
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RecommendationRepositoryImplTest {

    @Test
    fun `computeRecommendations caches and returns scored movies`() = runTest {
        val movieDao = FakeMovieDao()
        val favorite = MovieEntity(
            id = 1L,
            title = "Favorite",
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
            isFavorite = true,
            userRating = 0f
        )
        movieDao.upsert(favorite)

        val genreDao = FakeGenreDao(
            listOf(
                GenreEntity(12, "Adventure"),
                GenreEntity(28, "Action")
            )
        )
        val recommendationDao = FakeRecommendationDao()
        val watchlistDao = FakeWatchlistDao()
        val tmdbService = SeededTmdbService()

        val repository = RecommendationRepositoryImpl(
            movieDao = movieDao,
            genreDao = genreDao,
            watchlistDao = watchlistDao,
            recommendationDao = recommendationDao,
            tmdbService = tmdbService
        )

        val recommendations = repository.computeRecommendations(limit = 5)

        assertThat(recommendations).isNotEmpty()
        assertThat(recommendations.first().id).isEqualTo(2L)
        assertThat(recommendationDao.entities).isNotEmpty()

        val cached = repository.getCachedRecommendations().first()
        assertThat(cached).isNotEmpty()
        assertThat(cached.first().id).isEqualTo(2L)
    }

    private class FakeMovieDao : MovieDao {
        private val state = MutableStateFlow<List<MovieEntity>>(emptyList())
        private val movies = ConcurrentHashMap<Long, MovieEntity>()

        override suspend fun upsert(movie: MovieEntity) {
            movies[movie.id] = movie
            emit()
        }

        override suspend fun upsertAll(movies: List<MovieEntity>) {
            movies.forEach { upsert(it) }
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

        override fun observeByIds(ids: List<Long>): Flow<List<MovieEntity>> =
            state.map { list -> list.filter { it.id in ids } }

        override suspend fun searchCached(query: String): List<MovieEntity> = emptyList()

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

        override fun observeFavorites(): Flow<List<MovieEntity>> = state

        override fun observeRated(): Flow<List<MovieEntity>> = state

        override suspend fun getRecentMovies(limit: Int): List<MovieEntity> = emptyList()

        override suspend fun deleteOldMovies(cutoffTime: Long) {}

        override fun observeUserFlags(): Flow<List<MovieUserFlags>> = MutableStateFlow(emptyList())

        private fun emit() {
            state.value = movies.values.sortedBy { it.id }
        }
    }

    private class FakeGenreDao(genres: List<GenreEntity>) : GenreDao {
        private val state = MutableStateFlow(genres)

        override suspend fun upsertAll(genres: List<GenreEntity>) {
            state.value = genres
        }

        override fun getAll(): Flow<List<GenreEntity>> = state
    }

    private class FakeRecommendationDao : RecommendationDao {
        private val state = MutableStateFlow<List<RecommendedMovieEntity>>(emptyList())
        val entities: List<RecommendedMovieEntity>
            get() = state.value

        override suspend fun upsertAll(recommendations: List<RecommendedMovieEntity>) {
            state.value = recommendations
        }

        override fun observeAll(): Flow<List<RecommendedMovieEntity>> = state

        override suspend fun getAll(): List<RecommendedMovieEntity> = state.value

        override suspend fun clear() {
            state.value = emptyList()
        }
    }

    private class FakeWatchlistDao : WatchlistDao {
        override suspend fun insert(watchlist: WatchlistEntity): Long = 0
        override suspend fun update(watchlist: WatchlistEntity) {}
        override suspend fun delete(watchlist: WatchlistEntity) {}
        override fun listAll(): Flow<List<WatchlistEntity>> = MutableStateFlow(emptyList())
        override suspend fun getCurrent(): WatchlistEntity? = null
        override fun observeCurrent(): Flow<WatchlistEntity?> = MutableStateFlow(null)
        override suspend fun clearCurrent() {}
        override suspend fun setCurrent(id: Long) {}
        override suspend fun setCurrentWatchlist(id: Long) {}
        override suspend fun getWatchlistCount(): Int = 0
        override suspend fun getAllOnce(): List<WatchlistEntity> = emptyList()
        override suspend fun getById(id: Long): WatchlistEntity? = null
        override suspend fun addMovieToWatchlist(crossRef: WatchlistMovieCrossRef) {}
        override suspend fun removeMovieFromWatchlist(crossRef: WatchlistMovieCrossRef) {}
        override suspend fun removeMovieFromWatchlist(watchlistId: Long, movieId: Long) {}
        override suspend fun getMovieIds(watchlistId: Long): List<Long> = emptyList()
        override fun observeMovieIds(watchlistId: Long): Flow<List<Long>> = MutableStateFlow(emptyList())
        override suspend fun getAllMovieIds(): List<Long> = emptyList()
        override suspend fun getMovieCount(watchlistId: Long): Int = 0
        override fun observeMovieCount(watchlistId: Long): Flow<Int> = MutableStateFlow(0)
        override suspend fun isMovieInWatchlist(watchlistId: Long, movieId: Long): Boolean = false
    }

    private class SeededTmdbService : TmdbService {
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
            return TmdbSearchResponse(
                page = 1,
                results = listOf(candidateMovie()),
                totalPages = 1,
                totalResults = 1
            )
        }

        override suspend fun getMovie(movieId: Long): TmdbMovieDetails {
            return when (movieId) {
                1L -> favoriteDetails(movieId)
                2L -> candidateDetails(movieId)
                else -> candidateDetails(movieId)
            }
        }

        override suspend fun getCredits(movieId: Long): TmdbCredits {
            return when (movieId) {
                1L -> TmdbCredits(
                    id = movieId,
                    cast = listOf(
                        TmdbCast(id = 500L, name = "Fav Actor", character = "Hero", profilePath = null, order = 0)
                    ),
                    crew = listOf(
                        TmdbCrew(id = 900L, name = "Fav Director", job = "Director")
                    )
                )

                2L -> TmdbCredits(
                    id = movieId,
                    cast = listOf(
                        TmdbCast(id = 500L, name = "Fav Actor", character = "Hero", profilePath = null, order = 0),
                        TmdbCast(id = 501L, name = "New Actor", character = "Sidekick", profilePath = null, order = 1)
                    ),
                    crew = listOf(
                        TmdbCrew(id = 900L, name = "Fav Director", job = "Director")
                    )
                )

                else -> TmdbCredits(movieId, emptyList(), emptyList())
            }
        }

        override suspend fun getKeywords(movieId: Long): TmdbKeywords {
            return when (movieId) {
                1L -> TmdbKeywords(movieId, listOf(TmdbKeyword(700, "space")))
                2L -> TmdbKeywords(movieId, listOf(TmdbKeyword(700, "space"), TmdbKeyword(701, "future")))
                else -> TmdbKeywords(movieId, emptyList())
            }
        }

        override suspend fun searchPerson(query: String, page: Int): TmdbPersonSearchResponse {
            throw NotImplementedError()
        }

        override suspend fun getGenres(): TmdbGenresResponse {
            return TmdbGenresResponse(emptyList())
        }

        override suspend fun getPopularMovies(page: Int): TmdbSearchResponse {
            return TmdbSearchResponse(
                page = 1,
                results = listOf(candidateMovie()),
                totalPages = 1,
                totalResults = 1
            )
        }

        override suspend fun getTrendingMovies(page: Int): TmdbSearchResponse {
            return TmdbSearchResponse(
                page = 1,
                results = emptyList(),
                totalPages = 1,
                totalResults = 0
            )
        }

        private fun candidateMovie(): TmdbMovie {
            return TmdbMovie(
                id = 2L,
                title = "Candidate",
                posterPath = "/candidate.jpg",
                overview = "",
                releaseDate = "2024-01-01",
                voteAverage = 8.0,
                genreIds = listOf(12, 28)
            )
        }

        private fun favoriteDetails(movieId: Long): TmdbMovieDetails {
            return TmdbMovieDetails(
                id = movieId,
                title = "Favorite",
                posterPath = "/favorite.jpg",
                overview = "Fav",
                releaseDate = "2023-01-01",
                runtime = 120,
                voteAverage = 8.5,
                genres = listOf(
                    TmdbGenre(id = 12, name = "Adventure"),
                    TmdbGenre(id = 28, name = "Action")
                )
            )
        }

        private fun candidateDetails(movieId: Long): TmdbMovieDetails {
            return TmdbMovieDetails(
                id = movieId,
                title = "Candidate",
                posterPath = "/candidate.jpg",
                overview = "Candidate",
                releaseDate = "2024-01-01",
                runtime = 110,
                voteAverage = 8.0,
                genres = listOf(
                    TmdbGenre(id = 12, name = "Adventure"),
                    TmdbGenre(id = 28, name = "Action")
                )
            )
        }
    }
}

