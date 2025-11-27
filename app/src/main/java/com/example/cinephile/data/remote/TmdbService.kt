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

    @GET("search/tv")
    suspend fun searchTvSeries(
        @Query("query") query: String,
        @Query("page") page: Int = 1
    ): TmdbTvSearchResponse

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

    @GET("person/{person_id}")
    suspend fun getPersonDetails(
        @Path("person_id") personId: Long
    ): TmdbPersonDetails

    @GET("person/{person_id}/movie_credits")
    suspend fun getPersonMovieCredits(
        @Path("person_id") personId: Long
    ): TmdbPersonCredits

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

    @GET("tv/{tv_id}")
    suspend fun getTvSeries(
        @Path("tv_id") seriesId: Long
    ): TmdbTvSeriesDetails

    @GET("tv/{tv_id}/credits")
    suspend fun getTvCredits(
        @Path("tv_id") seriesId: Long
    ): TmdbCredits
}


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

data class TmdbTvSearchResponse(
    val page: Int,
    val results: List<TmdbTvSeries>,
    @Json(name = "total_pages") val totalPages: Int,
    @Json(name = "total_results") val totalResults: Int
)

data class TmdbTvSeries(
    val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "poster_path") val posterPath: String?,
    val overview: String?,
    @Json(name = "first_air_date") val firstAirDate: String?,
    @Json(name = "vote_average") val voteAverage: Double,
    @Json(name = "genre_ids") val genreIds: List<Int>
)

data class TmdbTvSeriesDetails(
    val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "poster_path") val posterPath: String?,
    val overview: String?,
    @Json(name = "first_air_date") val firstAirDate: String?,
    @Json(name = "last_air_date") val lastAirDate: String?,
    @Json(name = "vote_average") val voteAverage: Double,
    val genres: List<TmdbGenre>,
    @Json(name = "number_of_seasons") val numberOfSeasons: Int?,
    @Json(name = "number_of_episodes") val numberOfEpisodes: Int?,
    @Json(name = "episode_run_time") val episodeRunTime: List<Int>?
)

data class TmdbCredits(
    val id: Long,
    val cast: List<TmdbCast>,
    val crew: List<TmdbCrew>
)

data class TmdbPersonDetails(
    val id: Long,
    val name: String,
    @Json(name = "known_for_department") val knownForDepartment: String?,
    val biography: String?,
    val birthday: String?,
    val deathday: String?,
    @Json(name = "place_of_birth") val placeOfBirth: String?,
    @Json(name = "profile_path") val profilePath: String?,
    @Json(name = "also_known_as") val alsoKnownAs: List<String>?,
    val gender: Int?,
    val popularity: Double?
)

data class TmdbPersonCredits(
    val id: Long,
    val cast: List<TmdbMovieCastCredit>,
    val crew: List<TmdbMovieCrewCredit>
)

data class TmdbMovieCastCredit(
    val id: Long,
    val title: String,
    @Json(name = "poster_path") val posterPath: String?,
    @Json(name = "release_date") val releaseDate: String?,
    val character: String?,
    @Json(name = "vote_average") val voteAverage: Double
)

data class TmdbMovieCrewCredit(
    val id: Long,
    val title: String,
    @Json(name = "poster_path") val posterPath: String?,
    @Json(name = "release_date") val releaseDate: String?,
    val job: String?,
    @Json(name = "vote_average") val voteAverage: Double
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
