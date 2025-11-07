package com.example.cinephile.data.repository

import com.example.cinephile.data.local.dao.MovieDao
import com.example.cinephile.data.local.dao.MovieUserFlags
import com.example.cinephile.data.local.dao.GenreDao
import com.example.cinephile.data.local.dao.CachedSearchDao
import com.example.cinephile.data.local.entities.MovieEntity
import com.example.cinephile.data.local.entities.GenreEntity
import com.example.cinephile.data.local.entities.CachedSearchEntity
import com.example.cinephile.data.remote.TmdbService
import com.example.cinephile.domain.repository.MovieRepository
import com.example.cinephile.domain.repository.MovieFilters
import com.example.cinephile.domain.repository.MovieSearchResult
import com.example.cinephile.ui.search.MovieUiModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import java.security.MessageDigest
import android.util.Log

@Singleton
class MovieRepositoryImpl @Inject constructor(
    private val movieDao: MovieDao,
    private val genreDao: GenreDao,
    private val cachedSearchDao: CachedSearchDao,
    override val tmdbService: TmdbService
) : MovieRepository {

    private val TAG = "MovieRepo"

    // Cache for director mappings (movieId -> directorId)
    private val directorCache = mutableMapOf<Long, Long?>()
    
    // Generate deterministic hash from filters
    private fun generateQueryHash(filters: MovieFilters): String {
        val filterString = buildString {
            append("query:").append(filters.query ?: "")
            append("|year:").append(filters.primaryReleaseYear ?: "")
            append("|genres:").append(filters.genreIds.sorted().joinToString(","))
            append("|actors:").append(filters.actorIds.sorted().joinToString(","))
            append("|directors:").append(filters.directorIds.sorted().joinToString(","))
        }
        return md5(filterString)
    }
    
    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    override suspend fun searchMovies(filters: MovieFilters, page: Int): MovieSearchResult {
        return withContext(Dispatchers.IO) {
            val queryHash = generateQueryHash(filters)
            var isFromCache = false
            
            try {
                Log.d(TAG, "searchMovies call | page=$page | filters=$filters")
                val response = if (filters.query != null) {
                    // Use search endpoint for title-based queries
                    tmdbService.searchMovies(query = filters.query, page = page)
                } else {
                    // Use discover endpoint for filtered searches
                    tmdbService.discoverMovies(
                        year = filters.primaryReleaseYear,
                        genreIds = filters.genreIds.takeIf { it.isNotEmpty() }?.joinToString(","),
                        castIds = filters.actorIds.takeIf { it.isNotEmpty() }?.joinToString(","),
                        crewIds = null, // We'll filter directors after fetching
                        page = page
                    )
                }

                // Map TMDB movies to entities
                var entities = response.results.map { tmdbMovie ->
                    MovieEntity(
                        id = tmdbMovie.id,
                        title = tmdbMovie.title,
                        posterPath = tmdbMovie.posterPath,
                        overview = tmdbMovie.overview,
                        releaseDate = tmdbMovie.releaseDate,
                        directorId = null,
                        directorName = null,
                        castIds = emptyList(),
                        castNames = emptyList(),
                        genreIds = tmdbMovie.genreIds,
                        keywordIds = emptyList(),
                        runtime = null,
                        lastUpdated = System.currentTimeMillis(),
                        isFavorite = false,
                        userRating = 0f
                    )
                }

                // Filter by directors if needed
                if (filters.directorIds.isNotEmpty()) {
                    Log.d(TAG, "Filtering by directors: ${filters.directorIds}")
                    entities = filterMoviesByDirectors(entities, filters.directorIds)
                }

                // Cache entities
                entities.forEach { entity ->
                    movieDao.upsert(entity)
                }
                
                // Store search results in cache (only for first page)
                if (page == 1) {
                    val movieIds = entities.map { it.id }
                    val cachedSearch = CachedSearchEntity(
                        queryHash = queryHash,
                        resultMovieIds = movieIds,
                        createdAt = System.currentTimeMillis()
                    )
                    cachedSearchDao.upsert(cachedSearch)
                    Log.d(TAG, "Cached first page for hash=$queryHash idsCount=${movieIds.size}")
                }

                // Map to UI models
                val uiModels = entities.map { entity ->
                    entityToUiModel(entity)
                }

                MovieSearchResult(
                    movies = uiModels,
                    currentPage = response.page,
                    totalPages = response.totalPages,
                    isFromCache = isFromCache
                )
            } catch (e: Exception) {
                Log.e(TAG, "searchMovies failed | attempting cache (page=$page)", e)
                // Try to load from cache on error (only for first page)
                if (page == 1) {
                    val cachedSearch = cachedSearchDao.getByHash(queryHash)
                    if (cachedSearch != null) {
                        // Check if cache is recent (within 24 hours)
                        val cacheAge = System.currentTimeMillis() - cachedSearch.createdAt
                        val maxCacheAge = 24 * 60 * 60 * 1000L // 24 hours
                        
                        if (cacheAge < maxCacheAge) {
                            // Load movies from cache
                            val cachedEntities = movieDao.getByIds(cachedSearch.resultMovieIds)
                            val uiModels = cachedEntities.map { entity ->
                                entityToUiModel(entity)
                            }
                            
                            Log.d(TAG, "Returning results from cache | count=${uiModels.size} ageMs=$cacheAge")
                            MovieSearchResult(
                                movies = uiModels,
                                currentPage = 1,
                                totalPages = 1,
                                isLoading = false,
                                isFromCache = true,
                                cacheTimestamp = cachedSearch.createdAt
                            )
                        } else {
                            Log.d(TAG, "Cache too old; returning empty | ageMs=$cacheAge")
                            // Cache too old, return empty
                            MovieSearchResult(
                                movies = emptyList(),
                                currentPage = page,
                                totalPages = 1,
                                isLoading = false,
                                isFromCache = false
                            )
                        }
                    } else {
                        Log.d(TAG, "No cache entry for hash=$queryHash")
                        MovieSearchResult(
                            movies = emptyList(),
                            currentPage = page,
                            totalPages = 1,
                            isLoading = false,
                            isFromCache = false
                        )
                    }
                } else {
                    MovieSearchResult(
                        movies = emptyList(),
                        currentPage = page,
                        totalPages = 1,
                        isLoading = false,
                        isFromCache = false
                    )
                }
            }
        }
    }

    private suspend fun filterMoviesByDirectors(
        entities: List<MovieEntity>,
        directorIds: List<Long>
    ): List<MovieEntity> {
        val filteredMovies = mutableListOf<MovieEntity>()
        
        for (entity in entities) {
            val directorId = getDirectorIdForMovie(entity.id)
            if (directorId in directorIds) {
                filteredMovies.add(entity.copy(directorId = directorId))
            }
        }
        
        return filteredMovies
    }

    private suspend fun getDirectorIdForMovie(movieId: Long): Long? {
        // Check cache first
        directorCache[movieId]?.let { return it }

        // Fetch from API if not cached
        return withContext(Dispatchers.IO) {
            try {
                val credits = tmdbService.getCredits(movieId)
                val director = credits.crew.firstOrNull { it.job == "Director" }
                val directorId = director?.id
                
                // Cache the result
                directorCache[movieId] = directorId
                
                directorId
            } catch (e: Exception) {
                directorCache[movieId] = null
                null
            }
        }
    }

    override suspend fun getMovieDetails(movieId: Long): Flow<MovieUiModel?> {
        // Observe DB and emit UI model; also refresh from network and cache
        return withContext(Dispatchers.IO) {
            // Kick off a refresh in background
            try {
                val details = tmdbService.getMovie(movieId)
                val credits = tmdbService.getCredits(movieId)
                val keywords = tmdbService.getKeywords(movieId)

                val director = credits.crew.firstOrNull { it.job == "Director" }
                val castSorted = credits.cast.sortedBy { it.order }.take(10)

                val entity = MovieEntity(
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
                    isFavorite = movieDao.getById(movieId)?.isFavorite ?: false,
                    userRating = movieDao.getById(movieId)?.userRating ?: 0f
                )
                movieDao.upsert(entity)
            } catch (_: Exception) {
                // Ignore network errors; we'll rely on DB if available
            }

            // Return flow observing DB changes for this movie, combining with genres
            combine(
                movieDao.observeByIds(listOf(movieId)),
                genreDao.getAll()
            ) { movieList, allGenres ->
                val entity = movieList.firstOrNull() ?: return@combine null
                entityToUiModel(entity, allGenres)
            }
        }
    }

    override suspend fun toggleFavorite(movieId: Long, isFavorite: Boolean): MovieEntity? {
        return withContext(Dispatchers.IO) {
            try {
                movieDao.updateFavorite(movieId, isFavorite)
                movieDao.getById(movieId)
            } catch (_: Exception) {
                null
            }
        }
    }

    override suspend fun rateMovie(movieId: Long, rating: Float): MovieEntity? {
        return withContext(Dispatchers.IO) {
            try {
                movieDao.updateRating(movieId, rating)
                movieDao.getById(movieId)
            } catch (_: Exception) {
                null
            }
        }
    }

    override suspend fun getFavorites(): Flow<List<MovieUiModel>> {
        return combine(
            movieDao.observeFavorites(),
            genreDao.getAll()
        ) { movieList, allGenres ->
            movieList.map { entityToUiModel(it, allGenres) }
        }
    }

    override suspend fun getRatedMovies(): Flow<List<MovieUiModel>> {
        return combine(
            movieDao.observeRated(),
            genreDao.getAll()
        ) { movieList, allGenres ->
            movieList.map { entityToUiModel(it, allGenres) }
        }
    }

    override suspend fun fetchAndCacheGenres() {
        return withContext(Dispatchers.IO) {
            try {
                val response = tmdbService.getGenres()
                val genres = response.genres.map { GenreEntity(it.id, it.name) }
                genreDao.upsertAll(genres)
            } catch (e: Exception) {
                // You may want to log this error for debugging.
            }
        }
    }

    override fun getGenresFlow(): Flow<List<GenreEntity>> {
        return genreDao.getAll()
    }
    
    override fun observeUserFlags(): Flow<List<MovieUserFlags>> {
        return movieDao.observeUserFlags()
    }
    
    private fun entityToUiModel(entity: MovieEntity, allGenres: List<GenreEntity> = emptyList()): MovieUiModel {
        // Build poster URL
        val posterUrl = entity.posterPath?.let { path ->
            "https://image.tmdb.org/t/p/w500$path"
        }
        
        // Get genre names from genre IDs
        val genreNames = entity.genreIds.mapNotNull { genreId ->
            allGenres.firstOrNull { it.id == genreId }?.name
        }
        
        return MovieUiModel(
            id = entity.id,
            title = entity.title,
            posterUrl = posterUrl,
            director = entity.directorName,
            releaseDate = entity.releaseDate,
            overview = entity.overview,
            genres = genreNames,
            isFavorite = entity.isFavorite,
            userRating = if (entity.userRating > 0f) entity.userRating else 0f
        )
    }
}
