package com.cbgm.sparrow.feature.ocr.device

import com.cbgm.sparrow.feature.ocr.data.model.RecognizedTextLine

interface TextRecognizer {
    suspend fun recognize(localFilePath: String): Result<List<RecognizedTextLine>>
}
