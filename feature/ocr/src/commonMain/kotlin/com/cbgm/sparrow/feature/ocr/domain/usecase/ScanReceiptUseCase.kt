package com.cbgm.sparrow.feature.ocr.domain.usecase

import com.cbgm.sparrow.feature.ocr.domain.model.ReceiptScan
import com.cbgm.sparrow.feature.ocr.domain.repository.ReceiptScanRepository

class ScanReceiptUseCase(
    private val repository: ReceiptScanRepository
) {
    suspend operator fun invoke(localFilePath: String): Result<ReceiptScan> =
        repository.scan(localFilePath)
}
