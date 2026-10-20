package com.cbgm.sparrow.feature.ocr.domain.repository

import com.cbgm.sparrow.feature.ocr.domain.model.ReceiptScan

interface ReceiptScanRepository {
    suspend fun scan(localFilePath: String): Result<ReceiptScan>
}
