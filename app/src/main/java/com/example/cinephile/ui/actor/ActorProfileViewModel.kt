package com.example.cinephile.ui.actor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cinephile.domain.repository.PersonCreditType
import com.example.cinephile.domain.repository.PersonMovieCredit
import com.example.cinephile.domain.repository.PersonProfile
import com.example.cinephile.domain.repository.PersonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

data class ActorInfoRow(
    val label: String,
    val value: String
)

data class ActorProfileUiModel(
    val id: Long,
    val name: String,
    val profileImageUrl: String?,
    val biography: String?,
    val roles: List<String>,
    val birthDate: String?,
    val birthDateDisplay: String?,
    val deathDateDisplay: String?,
    val ageYears: Int?,
    val placeOfBirth: String?,
    val alsoKnownAs: List<String>
)

data class ActorProfileUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val toolbarTitle: String? = null,
    val profile: ActorProfileUiModel? = null,
    val movies: List<ActorMovieUiModel> = emptyList()
)

@HiltViewModel
class ActorProfileViewModel @Inject constructor(
    private val personRepository: PersonRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val actorId: Long = checkNotNull(savedStateHandle["actorId"]) { "actorId is required" }
    private val actorName: String? = savedStateHandle["actorName"]

    private val _uiState = MutableStateFlow(
        ActorProfileUiState(
            isLoading = true,
            toolbarTitle = actorName
        )
    )
    val uiState: StateFlow<ActorProfileUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun reload() {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                val profile = personRepository.getPersonProfile(actorId)
                val credits = personRepository.getPersonMovieCredits(actorId)
                Pair(profile, credits)
            }.onSuccess { (profile, credits) ->
                val uiProfile = profile?.toUiModel(credits)
                val movies = credits.map { it.toUiModel() }
                _uiState.value = ActorProfileUiState(
                    isLoading = false,
                    error = null,
                    toolbarTitle = uiProfile?.name ?: actorName,
                    profile = uiProfile,
                    movies = movies
                )
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = throwable.message ?: "Failed to load actor"
                    )
                }
            }
        }
    }

    private fun PersonProfile.toUiModel(credits: List<PersonMovieCredit>): ActorProfileUiModel {
        val roles = linkedSetOf<String>()
        knownForDepartment?.takeIf { it.isNotBlank() }?.let { roles.add(it) }
        if (credits.any { it.type == PersonCreditType.CAST }) {
            roles.add("Actor")
        }
        credits.filter { it.type == PersonCreditType.CREW }
            .mapNotNull { it.role }
            .map { capitalizeWords(it) }
            .forEach { roles.add(it) }

        val birthData = parseDate(birthday)
        val deathData = parseDate(deathday)

        val ageYears = birthData?.let { birth ->
            val endDate = deathData ?: LocalDate.now()
            val period = Period.between(birth, endDate)
            period.years.takeIf { it > 0 }
        }

        val birthDateDisplay = birthData?.let { formatDate(it) }
        val deathDateDisplay = deathData?.let { formatDate(it) }

        return ActorProfileUiModel(
            id = id,
            name = name,
            profileImageUrl = profileImageUrl,
            biography = biography?.takeIf { it.isNotBlank() },
            roles = roles.toList(),
            birthDate = birthday,
            birthDateDisplay = birthDateDisplay,
            deathDateDisplay = deathDateDisplay,
            ageYears = ageYears,
            placeOfBirth = placeOfBirth?.takeIf { it.isNotBlank() },
            alsoKnownAs = alsoKnownAs.filter { it.isNotBlank() }
        )
    }

    private fun PersonMovieCredit.toUiModel(): ActorMovieUiModel {
        val releaseYear = releaseDate?.takeIf { it.isNotBlank() }?.let { it.take(4) }
        return ActorMovieUiModel(
            id = id,
            title = title,
            posterUrl = posterUrl,
            releaseYear = releaseYear,
            role = role,
            type = type,
            voteAverage = voteAverage
        )
    }

    private fun parseDate(date: String?): LocalDate? {
        if (date.isNullOrBlank()) return null
        return try {
            LocalDate.parse(date)
        } catch (ex: DateTimeParseException) {
            null
        }
    }

    private fun formatDate(date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault())
        return date.format(formatter)
    }

    private fun capitalizeWords(text: String): String =
        text.split(" ").joinToString(" ") { word ->
            word.lowercase(Locale.getDefault()).replaceFirstChar { ch ->
                if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
            }
        }
}

