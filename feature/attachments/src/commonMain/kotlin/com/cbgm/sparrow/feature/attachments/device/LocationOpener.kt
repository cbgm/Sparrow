package com.cbgm.sparrow.feature.attachments.device

import androidx.compose.runtime.Composable
import com.cbgm.sparrow.feature.attachments.domain.model.CurrentLocation

interface LocationOpener {
    fun open(location: CurrentLocation): Result<Unit>
}

@Composable
expect fun rememberLocationOpener(): LocationOpener
