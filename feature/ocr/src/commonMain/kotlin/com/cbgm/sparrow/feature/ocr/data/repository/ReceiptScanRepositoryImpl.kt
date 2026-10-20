package com.cbgm.sparrow.feature.ocr.data.repository

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.ocr.data.parser.ReceiptTextParser
import com.cbgm.sparrow.feature.ocr.device.TextRecognizer
import com.cbgm.sparrow.feature.ocr.domain.model.ReceiptScan
import com.cbgm.sparrow.feature.ocr.domain.repository.ReceiptScanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class ReceiptScanRepositoryImpl(
    private val textRecognizer: TextRecognizer
) : ReceiptScanRepository {
    private val logger = SparrowLog.withTag("ReceiptScan")

    override suspend fun scan(localFilePath: String): Result<ReceiptScan> =
        safeSuspendCall {
            val lines = textRecognizer.recognize(localFilePath).getOrThrow()
            logger.debug { "Recognized receipt lines:\n${lines.joinToString("\n")}" }
            withContext(Dispatchers.Default) { ReceiptTextParser.parse(lines) }
                .also { scan -> logger.debug { "Receipt scan result: $scan" } }
        }
}
