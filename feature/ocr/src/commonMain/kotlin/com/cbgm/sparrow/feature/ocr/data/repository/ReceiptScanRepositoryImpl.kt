package com.cbgm.sparrow.feature.ocr.data.repository

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.ocr.data.parser.ReceiptTextParser
import com.cbgm.sparrow.feature.ocr.device.TextRecognizer
import com.cbgm.sparrow.feature.ocr.domain.model.ReceiptScan
import com.cbgm.sparrow.feature.ocr.domain.repository.ReceiptScanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class ReceiptScanRepositoryImpl(
    private val textRecognizer: TextRecognizer
) : ReceiptScanRepository {
    override suspend fun scan(localFilePath: String): Result<ReceiptScan> =
        safeSuspendCall {
            val lines = textRecognizer.recognize(localFilePath).getOrThrow()
            withContext(Dispatchers.Default) { ReceiptTextParser.parse(lines) }
        }
}
