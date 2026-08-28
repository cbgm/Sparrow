package com.cbgm.sparrow.feature.media.device

import androidx.compose.runtime.Composable
import com.cbgm.sparrow.feature.media.domain.model.MediaExportItem

interface MediaExporter {
    suspend fun saveToCameraRoll(media: List<MediaExportItem>): Result<Int>
}

@Composable
expect fun rememberMediaExporter(): MediaExporter
