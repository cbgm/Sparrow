package com.cbgm.sparrow.feature.media.device

import kotlin.math.abs

internal data class DocumentFrameState(
    val isDocumentDetected: Boolean,
    val steadyProgress: Float
) {
    val isReadyForCapture: Boolean get() = steadyProgress >= 1f

    companion object {
        val None = DocumentFrameState(isDocumentDetected = false, steadyProgress = 0f)
    }
}

internal class DocumentFrameDetector(
    private val requiredSteadyMilliseconds: Long = DEFAULT_REQUIRED_STEADY_MILLISECONDS
) {
    private var previousGrid: IntArray? = null
    private var steadySinceMilliseconds: Long? = null

    fun reset() {
        previousGrid = null
        steadySinceMilliseconds = null
    }

    fun analyze(
        luminance: ByteArray,
        width: Int,
        height: Int,
        timestampMilliseconds: Long
    ): DocumentFrameState {
        require(width > 0 && height > 0 && luminance.size >= width * height) {
            "Invalid luminance frame"
        }
        val frame = Frame(luminance, width, height)
        val grid = frame.innerGrid()
        val previous = previousGrid
        previousGrid = grid
        val detected = frame.hasPaper() && frame.hasPrintedText()
        val steady = previous != null && grid.meanDifference(previous) <= MAX_STEADY_DIFFERENCE
        if (!detected || !steady) {
            steadySinceMilliseconds = if (detected) timestampMilliseconds else null
            return DocumentFrameState(isDocumentDetected = detected, steadyProgress = 0f)
        }
        val since =
            steadySinceMilliseconds ?: timestampMilliseconds.also { steadySinceMilliseconds = it }
        val progress = (timestampMilliseconds - since).toFloat() / requiredSteadyMilliseconds
        return DocumentFrameState(
            isDocumentDetected = true,
            steadyProgress = progress.coerceIn(0f, 1f)
        )
    }

    private class Frame(
        private val luminance: ByteArray,
        private val width: Int,
        private val height: Int
    ) {
        private val innerLeft = (width * INNER_START).toInt()
        private val innerRight = (width * INNER_END).toInt()
        private val innerTop = (height * INNER_START).toInt()
        private val innerBottom = (height * INNER_END).toInt()
        private val step = maxOf(1, minOf(width, height) / SAMPLES_PER_SIDE)

        fun at(x: Int, y: Int): Int = luminance[y * width + x].toInt() and 0xFF

        fun isInner(x: Int, y: Int): Boolean =
            x in innerLeft until innerRight && y in innerTop until innerBottom

        fun hasPaper(): Boolean {
            var outerSum = 0L
            var outerCount = 0
            forEachSample { x, y ->
                if (!isInner(x, y)) {
                    outerSum += at(x, y)
                    outerCount++
                }
            }
            val outerMean = if (outerCount == 0) 0 else (outerSum / outerCount).toInt()
            val threshold = maxOf(MIN_PAPER_LUMINANCE, outerMean + MIN_PAPER_CONTRAST)
            var paper = 0
            var inner = 0
            forEachSample { x, y ->
                if (isInner(x, y)) {
                    inner++
                    if (at(x, y) >= threshold) paper++
                }
            }
            return inner > 0 && paper.toFloat() / inner >= MIN_PAPER_RATIO
        }

        fun hasPrintedText(): Boolean {
            var edges = 0
            var pairs = 0
            var y = innerTop
            while (y < innerBottom) {
                for (x in innerLeft until innerRight - 1) {
                    pairs++
                    if (abs(at(x, y) - at(x + 1, y)) >= TEXT_EDGE_DIFFERENCE) edges++
                }
                y += TEXT_ROW_STEP
            }
            return pairs > 0 && edges.toFloat() / pairs >= MIN_TEXT_EDGE_RATIO
        }

        fun innerGrid(): IntArray {
            val cellWidth = maxOf(1, (innerRight - innerLeft) / GRID_SIZE)
            val cellHeight = maxOf(1, (innerBottom - innerTop) / GRID_SIZE)
            return IntArray(GRID_SIZE * GRID_SIZE) { index ->
                val left = innerLeft + (index % GRID_SIZE) * cellWidth
                val top = innerTop + (index / GRID_SIZE) * cellHeight
                var sum = 0
                var count = 0
                var y = top
                while (y < minOf(top + cellHeight, height)) {
                    var x = left
                    while (x < minOf(left + cellWidth, width)) {
                        sum += at(x, y)
                        count++
                        x += step
                    }
                    y += step
                }
                if (count == 0) 0 else sum / count
            }
        }

        private inline fun forEachSample(action: (Int, Int) -> Unit) {
            var y = 0
            while (y < height) {
                var x = 0
                while (x < width) {
                    action(x, y)
                    x += step
                }
                y += step
            }
        }
    }

    private companion object {
        const val DEFAULT_REQUIRED_STEADY_MILLISECONDS = 700L
        const val INNER_START = 0.25f
        const val INNER_END = 0.75f
        const val SAMPLES_PER_SIDE = 48
        const val GRID_SIZE = 12
        const val MIN_PAPER_LUMINANCE = 110
        const val MIN_PAPER_CONTRAST = 20
        const val MIN_PAPER_RATIO = 0.55f
        const val TEXT_ROW_STEP = 3
        const val TEXT_EDGE_DIFFERENCE = 40
        const val MIN_TEXT_EDGE_RATIO = 0.01f
        const val MAX_STEADY_DIFFERENCE = 8
    }
}

private fun IntArray.meanDifference(other: IntArray): Int {
    if (size != other.size || isEmpty()) return Int.MAX_VALUE
    var sum = 0
    for (index in indices) sum += abs(this[index] - other[index])
    return sum / size
}
