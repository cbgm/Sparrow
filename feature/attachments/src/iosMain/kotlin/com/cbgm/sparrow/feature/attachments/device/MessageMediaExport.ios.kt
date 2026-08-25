package com.cbgm.sparrow.feature.attachments.device

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberMessageMediaExporter(): MessageMediaExporter =
    remember {
        object : MessageMediaExporter {
            override suspend fun saveToCameraRoll(media: List<MessageMediaExportItem>): Result<Int> =
                Result.failure(UnsupportedOperationException("Camera-roll export is not implemented on iOS yet"))
        }
    }
