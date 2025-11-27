package com.example.cinephile.domain.model






data class MovieContentVector(
    val movieId: Long,
    val vector: BooleanArray,
    val featureKeys: List<String>
)

