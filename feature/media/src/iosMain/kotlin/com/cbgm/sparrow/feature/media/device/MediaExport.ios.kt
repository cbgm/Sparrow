package com.cbgm.sparrow.feature.media.device

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.cbgm.sparrow.feature.media.domain.model.MediaExportItem

@Composable
actual fun rememberMediaExporter(): MediaExporter =
    remember {
        object : MediaExporter {
            override suspend fun saveToCameraRoll(media: List<MediaExportItem>): Result<Int> =
                Result.failure(UnsupportedOperationException("Camera-roll export is not implemented on iOS yet"))
        }
    }
