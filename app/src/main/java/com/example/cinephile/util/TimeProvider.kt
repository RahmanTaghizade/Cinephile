package com.example.cinephile.util

import kotlinx.coroutines.flow.Flow

interface TimeProvider {
    fun tickEverySecond(): Flow<Long>
}

