package com.cbgm.sparrow.feature.search.data.embedding

import kotlin.math.sqrt

internal fun FloatArray.normalizedPrefix(size: Int): FloatArray {
    val result = copyOf(minOf(size, this.size))
    var sumSquares = 0.0
    result.forEach { value -> sumSquares += value * value }
    val norm = sqrt(sumSquares).toFloat()
    if (norm > 0f) {
        result.indices.forEach { index -> result[index] /= norm }
    }
    return result
}

internal fun cosineSimilarity(
    first: FloatArray,
    second: FloatArray
): Float {
    if (first.size != second.size || first.isEmpty()) return 0f
    var dot = 0f
    var firstNorm = 0f
    var secondNorm = 0f
    for (index in first.indices) {
        dot += first[index] * second[index]
        firstNorm += first[index] * first[index]
        secondNorm += second[index] * second[index]
    }
    if (firstNorm == 0f || secondNorm == 0f) return 0f
    return dot / sqrt(firstNorm * secondNorm)
}
