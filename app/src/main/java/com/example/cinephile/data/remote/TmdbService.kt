package com.example.cinephile.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbService {

    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    @GET("discover/movie")
    suspend fun discoverMovies(
        @Query("primary_release_year") year: Int? = null,
        @Query("with_genres") genreIds: String? = null,
        @Query("with_cast") castIds: String? = null,
        @Query("with_crew") crewIds: String? = null,
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    @GET("movie/{movie_id}")
    suspend fun getMovie(
        @Path("movie_id") movieId: Long
    ): TmdbMovieDetails

    @GET("movie/{movie_id}/credits")
    suspend fun getCredits(
        @Path("movie_id") movieId: Long
    ): TmdbCredits

    @GET("movie/{movie_id}/keywords")
    suspend fun getKeywords(
        @Path("movie_id") movieId: Long
    ): TmdbKeywords

    @GET("search/person")
    suspend fun searchPerson(
        @Query("query") query: String,
        @Query("page") page: Int = 1
    ): TmdbPersonSearchResponse

    @GET("genre/movie/list")
    suspend fun getGenres(): TmdbGenresResponse

    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    @GET("trending/movie/week")
    suspend fun getTrendingMovies(
        @Query("page") page: Int = 1
    ): TmdbSearchResponse
}

// DTOs - These will be implemented in the next steps
data class TmdbSearchResponse(
    val page: Int,
    val results: List<TmdbMovie>,
    val total_pages: Int,
    val total_results: Int
)

data class TmdbMovie(
    val id: Long,
    val title: String,
    val poster_path: String?,
    val overview: String,
    val release_date: String?,
    val vote_average: Double,
    val genre_ids: List<Int>
)

data class TmdbMovieDetails(
    val id: Long,
    val title: String,
    val poster_path: String?,
    val overview: String,
    val release_date: String?,
    val runtime: Int?,
    val vote_average: Double,
    val genres: List<TmdbGenre>
)

data class TmdbCredits(
    val id: Long,
    val cast: List<TmdbCast>,
    val crew: List<TmdbCrew>
)

data class TmdbCast(
    val id: Long,
    val name: String,
    val character: String,
    val order: Int
)

data class TmdbCrew(
    val id: Long,
    val name: String,
    val job: String
)

data class TmdbKeywords(
    val id: Long,
    val keywords: List<TmdbKeyword>
)

data class TmdbKeyword(
    val id: Int,
    val name: String
)

data class TmdbPersonSearchResponse(
    val page: Int,
    val results: List<TmdbPerson>,
    val total_pages: Int,
    val total_results: Int
)

data class TmdbPerson(
    val id: Long,
    val name: String,
    val known_for_department: String
)

data class TmdbGenresResponse(
    val genres: List<TmdbGenre>
)

data class TmdbGenre(
    val id: Int,
    val name: String
)
