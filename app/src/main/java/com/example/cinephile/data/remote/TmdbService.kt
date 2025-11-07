package com.example.cinephile.data.remote

import com.squareup.moshi.Json
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

// DTOs
data class TmdbSearchResponse(
    val page: Int,
    val results: List<TmdbMovie>,
    @Json(name = "total_pages") val totalPages: Int,
    @Json(name = "total_results") val totalResults: Int
)

data class TmdbMovie(
    val id: Long,
    val title: String,
    @Json(name = "poster_path") val posterPath: String?,
    val overview: String,
    @Json(name = "release_date") val releaseDate: String?,
    @Json(name = "vote_average") val voteAverage: Double,
    @Json(name = "genre_ids") val genreIds: List<Int>
)

data class TmdbMovieDetails(
    val id: Long,
    val title: String,
    @Json(name = "poster_path") val posterPath: String?,
    val overview: String,
    @Json(name = "release_date") val releaseDate: String?,
    val runtime: Int?,
    @Json(name = "vote_average") val voteAverage: Double,
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
    @Json(name = "profile_path") val profilePath: String?,
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
    @Json(name = "total_pages") val totalPages: Int,
    @Json(name = "total_results") val totalResults: Int
)

data class TmdbPerson(
    val id: Long,
    val name: String,
    @Json(name = "known_for_department") val knownForDepartment: String,
    @Json(name = "profile_path") val profilePath: String?
)

data class TmdbGenresResponse(
    val genres: List<TmdbGenre>
)

data class TmdbGenre(
    val id: Int,
    val name: String
)
