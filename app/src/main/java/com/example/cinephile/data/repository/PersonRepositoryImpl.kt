package com.example.cinephile.data.repository

import com.example.cinephile.data.remote.TmdbMovieCastCredit
import com.example.cinephile.data.remote.TmdbMovieCrewCredit
import com.example.cinephile.data.remote.TmdbPersonDetails
import com.example.cinephile.data.remote.TmdbService
import com.example.cinephile.domain.repository.PersonCreditType
import com.example.cinephile.domain.repository.PersonMovieCredit
import com.example.cinephile.domain.repository.PersonProfile
import com.example.cinephile.domain.repository.PersonRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersonRepositoryImpl @Inject constructor(
    private val tmdbService: TmdbService
) : PersonRepository {

    override suspend fun getPersonProfile(personId: Long): PersonProfile? {
        val details = runCatching { tmdbService.getPersonDetails(personId) }.getOrNull()
        return details?.toDomain()
    }

    override suspend fun getPersonMovieCredits(personId: Long): List<PersonMovieCredit> {
        val response = runCatching { tmdbService.getPersonMovieCredits(personId) }.getOrNull()
            ?: return emptyList()
        val creditsMap = linkedMapOf<Long, PersonMovieCredit>()

        response.cast
            .sortedByDescending { it.releaseDate.orEmpty() }
            .forEach { credit ->
                creditsMap[credit.id] = credit.toDomain()
            }

        response.crew
            .sortedByDescending { it.releaseDate.orEmpty() }
            .forEach { credit ->
                creditsMap.putIfAbsent(credit.id, credit.toDomain())
            }

        return creditsMap.values.toList()
    }

    private fun TmdbPersonDetails.toDomain(): PersonProfile =
        PersonProfile(
            id = id,
            name = name,
            biography = biography?.takeIf { it.isNotBlank() },
            birthday = birthday,
            deathday = deathday,
            placeOfBirth = placeOfBirth,
            knownForDepartment = knownForDepartment,
            alsoKnownAs = alsoKnownAs ?: emptyList(),
            profileImageUrl = profilePath?.let { "https://image.tmdb.org/t/p/w780$it" },
            popularity = popularity,
            gender = gender
        )

    private fun TmdbMovieCastCredit.toDomain(): PersonMovieCredit =
        PersonMovieCredit(
            id = id,
            title = title,
            posterUrl = posterPath?.let { "https://image.tmdb.org/t/p/w342$it" },
            releaseDate = releaseDate,
            role = character?.takeIf { it.isNotBlank() },
            voteAverage = voteAverage,
            type = PersonCreditType.CAST
        )

    private fun TmdbMovieCrewCredit.toDomain(): PersonMovieCredit =
        PersonMovieCredit(
            id = id,
            title = title,
            posterUrl = posterPath?.let { "https://image.tmdb.org/t/p/w342$it" },
            releaseDate = releaseDate,
            role = job?.takeIf { it.isNotBlank() },
            voteAverage = voteAverage,
            type = PersonCreditType.CREW
        )
}

