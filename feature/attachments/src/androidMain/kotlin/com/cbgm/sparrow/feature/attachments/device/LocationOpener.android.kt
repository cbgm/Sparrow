package com.cbgm.sparrow.feature.attachments.device

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.cbgm.sparrow.feature.attachments.domain.model.CurrentLocation

@Composable
actual fun rememberLocationOpener(): LocationOpener {
    val context = LocalContext.current.applicationContext
    return remember(context) { AndroidLocationOpener(context) }
}

private class AndroidLocationOpener(
    private val context: Context
) : LocationOpener {
    override fun open(location: CurrentLocation): Result<Unit> =
        runCatching {
            val coordinates = "${location.latitude},${location.longitude}"
            val intent = Intent(
                Intent.ACTION_VIEW,
                "geo:$coordinates?q=$coordinates(Standort)".toUri()
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
}
