package com.example.cinephile.data.repository

import com.example.cinephile.data.local.entities.MovieEntity
import com.example.cinephile.domain.model.MovieContentVector
import java.util.concurrent.ConcurrentHashMap

internal class MovieContentVectorBuilder {

    private val featureIndex = LinkedHashMap<String, Int>()
    private val vectorCache = ConcurrentHashMap<Long, BooleanArray>()
    private val lock = Any()

    fun compute(entity: MovieEntity): MovieContentVector {
        val features = collectFeatures(entity)
        synchronized(lock) {
            val cached = vectorCache[entity.id]?.let { ensureCapacity(it, featureIndex.size) }
            if (cached != null) {
                val updated = applyFeatures(cached, features)
                vectorCache[entity.id] = updated
                return MovieContentVector(
                    movieId = entity.id,
                    vector = updated.copyOf(),
                    featureKeys = snapshotFeatureKeys()
                )
            }

            var vector = BooleanArray(featureIndex.size)
            for (feature in features) {
                val index = featureIndex[feature] ?: registerFeature(feature)
                if (index >= vector.size) {
                    vector = vector.copyOf(featureIndex.size)
                }
            }
            for (feature in features) {
                val index = featureIndex.getValue(feature)
                vector[index] = true
            }
            vectorCache[entity.id] = vector
            return MovieContentVector(
                movieId = entity.id,
                vector = vector.copyOf(),
                featureKeys = snapshotFeatureKeys()
            )
        }
    }

    fun invalidate(movieId: Long) {
        vectorCache.remove(movieId)
    }

    fun clear() {
        synchronized(lock) {
            featureIndex.clear()
            vectorCache.clear()
        }
    }

    private fun applyFeatures(
        original: BooleanArray,
        features: Set<String>
    ): BooleanArray {
        var vector = original
        var mutated = false
        for (feature in features) {
            val index = featureIndex[feature] ?: registerFeature(feature)
            if (index >= vector.size) {
                vector = vector.copyOf(featureIndex.size)
                mutated = true
            }
            if (!vector[index]) {
                vector[index] = true
                mutated = true
            }
        }
        return if (mutated) vector else original
    }

    private fun registerFeature(feature: String): Int {
        val newIndex = featureIndex.size
        featureIndex[feature] = newIndex
        if (vectorCache.isNotEmpty()) {
            vectorCache.replaceAll { _, vector ->
                if (vector.size > newIndex) {
                    vector
                } else {
                    vector.copyOf(newIndex + 1)
                }
            }
        }
        return newIndex
    }

    private fun ensureCapacity(vector: BooleanArray, size: Int): BooleanArray {
        return if (vector.size >= size) vector else vector.copyOf(size)
    }

    private fun snapshotFeatureKeys(): List<String> {
        return featureIndex.entries
            .sortedBy { it.value }
            .map { it.key }
    }

    private fun collectFeatures(entity: MovieEntity): Set<String> {
        val features = LinkedHashSet<String>()
        entity.genreIds.forEach { features.add("genre:$it") }
        entity.castIds.forEach { features.add("cast:$it") }
        entity.directorId?.let { features.add("director:$it") }
        entity.keywordIds.forEach { features.add("keyword:$it") }
        return features
    }
}

