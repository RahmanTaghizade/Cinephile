package com.example.cinephile.data.repository

import com.example.cinephile.data.local.entities.MovieEntity
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MovieContentVectorBuilderTest {

    @Test
    fun `compute creates multi-hot vector with expected features`() {
        val builder = MovieContentVectorBuilder()
        val entity = movie(
            id = 1L,
            genreIds = listOf(12, 18),
            castIds = listOf(1001L, 1002L),
            directorId = 900L,
            keywordIds = listOf(700, 701, 702)
        )

        val vector = builder.compute(entity)

        assertThat(vector.movieId).isEqualTo(1L)
        assertThat(vector.featureKeys).containsAtLeastElementsIn(
            listOf(
                "genre:12",
                "genre:18",
                "cast:1001",
                "cast:1002",
                "director:900",
                "keyword:700",
                "keyword:701",
                "keyword:702"
            )
        )
        val activeCount = vector.vector.count { it }
        assertThat(activeCount).isEqualTo(8)
    }

    @Test
    fun `compute caches results and expands for new features`() {
        val builder = MovieContentVectorBuilder()
        val first = movie(
            id = 1L,
            genreIds = listOf(12),
            castIds = listOf(1001L),
            directorId = 900L,
            keywordIds = listOf(700)
        )
        val second = movie(
            id = 2L,
            genreIds = listOf(12, 35),
            castIds = listOf(1001L, 1003L),
            directorId = 901L,
            keywordIds = listOf(700, 705, 706)
        )

        val initialVector = builder.compute(first)
        val secondVector = builder.compute(second)
        val recomputedFirst = builder.compute(first)

        assertThat(initialVector.vector.size).isLessThan(secondVector.vector.size)
        assertThat(recomputedFirst.vector.size).isEqualTo(secondVector.vector.size)
        val index = secondVector.featureKeys.indexOf("cast:1003")
        assertThat(index).isAtLeast(0)
        assertThat(recomputedFirst.vector[index]).isFalse()
    }

    @Test
    fun `invalidate drops cached vector for movie`() {
        val builder = MovieContentVectorBuilder()
        val initial = movie(
            id = 5L,
            genreIds = listOf(10),
            castIds = listOf(2000L),
            directorId = 3000L,
            keywordIds = listOf(4000)
        )
        val updated = movie(
            id = 5L,
            genreIds = listOf(10, 11),
            castIds = listOf(2001L),
            directorId = 3001L,
            keywordIds = listOf(4001, 4002)
        )

        val originalVector = builder.compute(initial)
        builder.invalidate(initial.id)
        val updatedVector = builder.compute(updated)

        val oldKeywordIndex = updatedVector.featureKeys.indexOf("keyword:4000")
        if (oldKeywordIndex >= 0) {
            assertThat(updatedVector.vector[oldKeywordIndex]).isFalse()
        }
        val newKeywordIndex = updatedVector.featureKeys.indexOf("keyword:4002")
        assertThat(newKeywordIndex).isAtLeast(0)
        assertThat(updatedVector.vector[newKeywordIndex]).isTrue()
    }

    private fun movie(
        id: Long,
        genreIds: List<Int>,
        castIds: List<Long>,
        directorId: Long?,
        keywordIds: List<Int>
    ): MovieEntity {
        return MovieEntity(
            id = id,
            title = "Movie $id",
            posterPath = null,
            overview = "Overview",
            releaseDate = "2024-01-01",
            directorId = directorId,
            directorName = directorId?.let { "Director $it" },
            castIds = castIds,
            castNames = castIds.map { "Cast $it" },
            genreIds = genreIds,
            keywordIds = keywordIds,
            runtime = 120,
            lastUpdated = System.currentTimeMillis(),
            isFavorite = false,
            userRating = 0f
        )
    }
}

