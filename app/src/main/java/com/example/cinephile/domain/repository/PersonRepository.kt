package com.example.cinephile.domain.repository

data class PersonProfile(
    val id: Long,
    val name: String,
    val biography: String?,
    val birthday: String?,
    val deathday: String?,
    val placeOfBirth: String?,
    val knownForDepartment: String?,
    val alsoKnownAs: List<String>,
    val profileImageUrl: String?,
    val popularity: Double?,
    val gender: Int?
)

data class PersonMovieCredit(
    val id: Long,
    val title: String,
    val posterUrl: String?,
    val releaseDate: String?,
    val role: String?,
    val voteAverage: Double,
    val type: PersonCreditType
)

enum class PersonCreditType { CAST, CREW }

interface PersonRepository {
    suspend fun getPersonProfile(personId: Long): PersonProfile?
    suspend fun getPersonMovieCredits(personId: Long): List<PersonMovieCredit>
}

