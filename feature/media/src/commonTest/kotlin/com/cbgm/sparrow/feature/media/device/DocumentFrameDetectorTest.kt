package com.cbgm.sparrow.feature.media.device

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DocumentFrameDetectorTest {
    @Test
    fun steadyReceiptBecomesReadyAfterTheRequiredTime() {
        val detector = DocumentFrameDetector(requiredSteadyMilliseconds = 600L)
        val frame = receiptFrame()

        val first = detector.analyze(frame, WIDTH, HEIGHT, timestampMilliseconds = 0L)
        val middle = detector.analyze(frame, WIDTH, HEIGHT, timestampMilliseconds = 300L)
        val last = detector.analyze(frame, WIDTH, HEIGHT, timestampMilliseconds = 900L)

        assertTrue(first.isDocumentDetected)
        assertEquals(0f, first.steadyProgress)
        assertTrue(middle.steadyProgress in 0.4f..0.6f)
        assertTrue(last.isReadyForCapture)
    }

    @Test
    fun blankPaperWithoutTextIsNotADocument() {
        val detector = DocumentFrameDetector()
        val frame = receiptFrame(withText = false)

        detector.analyze(frame, WIDTH, HEIGHT, timestampMilliseconds = 0L)
        val state = detector.analyze(frame, WIDTH, HEIGHT, timestampMilliseconds = 2_000L)

        assertFalse(state.isDocumentDetected)
        assertFalse(state.isReadyForCapture)
    }

    @Test
    fun darkSceneIsNotADocument() {
        val detector = DocumentFrameDetector()
        val frame = ByteArray(WIDTH * HEIGHT) { 40 }

        val state = detector.analyze(frame, WIDTH, HEIGHT, timestampMilliseconds = 0L)

        assertFalse(state.isDocumentDetected)
    }

    @Test
    fun movementRestartsTheSteadyTimer() {
        val detector = DocumentFrameDetector(requiredSteadyMilliseconds = 600L)

        detector.analyze(receiptFrame(), WIDTH, HEIGHT, timestampMilliseconds = 0L)
        detector.analyze(receiptFrame(), WIDTH, HEIGHT, timestampMilliseconds = 500L)
        val shifted = receiptFrame(offset = 40)
        val moved = detector.analyze(shifted, WIDTH, HEIGHT, timestampMilliseconds = 700L)
        val afterMove = detector.analyze(shifted, WIDTH, HEIGHT, timestampMilliseconds = 900L)

        assertEquals(0f, moved.steadyProgress)
        assertFalse(afterMove.isReadyForCapture)
    }

    private fun receiptFrame(withText: Boolean = true, offset: Int = 0): ByteArray =
        ByteArray(WIDTH * HEIGHT) { index ->
            val x = index % WIDTH
            val y = index / WIDTH
            val onPaper =
                x in (WIDTH / 6 + offset) until (WIDTH * 5 / 6 + offset) &&
                    y in HEIGHT / 10 until HEIGHT * 9 / 10
            val onText = withText && onPaper && y % 12 < 3 && (x / 4) % 3 != 0
            when {
                onText -> 30
                onPaper -> 220
                else -> 60
            }.toByte()
        }

    private companion object {
        const val WIDTH = 240
        const val HEIGHT = 320
    }
}
