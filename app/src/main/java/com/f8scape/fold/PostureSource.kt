package com.f8scape.fold

import kotlinx.coroutines.flow.Flow

/**
 * Abstraction for observing fold posture changes from various system sources.
 */
interface PostureSource {
    val name: String
    val isAvailable: Boolean
    fun observePosture(): Flow<PostureData>
}
