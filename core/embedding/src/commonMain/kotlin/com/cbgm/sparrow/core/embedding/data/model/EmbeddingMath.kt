package com.cbgm.sparrow.core.embedding.data.model

import kotlin.math.sqrt

fun FloatArray.normalizedPrefix(size: Int): FloatArray {
    require(size > 0 && size <= this.size)
    val result = copyOf(size)
    var squaredNorm = 0.0
    result.forEach { value -> squaredNorm += value * value }
    val norm = sqrt(squaredNorm).toFloat()
    if (norm == 0f) return result
    for (index in result.indices) {
        result[index] /= norm
    }
    return result
}

fun cosineSimilarity(
    first: FloatArray,
    second: FloatArray
): Float {
    val size = minOf(first.size, second.size)
    if (size == 0) return 0f
    var dot = 0f
    var firstNorm = 0f
    var secondNorm = 0f
    for (index in 0 until size) {
        val a = first[index]
        val b = second[index]
        dot += a * b
        firstNorm += a * a
        secondNorm += b * b
    }
    if (firstNorm == 0f || secondNorm == 0f) return 0f
    return dot / (sqrt(firstNorm) * sqrt(secondNorm))
}
