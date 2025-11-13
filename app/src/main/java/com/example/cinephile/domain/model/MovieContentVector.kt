package com.example.cinephile.domain.model

/**
 * Represents a multi-hot content vector for a movie. The [vector] uses the same ordering
 * as [featureKeys], where a value of `true` indicates that the movie expresses the
 * corresponding feature (genres, cast members, director, keywords, etc).
 */
data class MovieContentVector(
    val movieId: Long,
    val vector: BooleanArray,
    val featureKeys: List<String>
)

