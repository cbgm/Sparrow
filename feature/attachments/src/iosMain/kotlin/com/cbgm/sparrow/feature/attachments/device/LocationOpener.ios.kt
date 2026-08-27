package com.cbgm.sparrow.feature.attachments.device

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.cbgm.sparrow.feature.attachments.domain.model.CurrentLocation
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

@Composable
actual fun rememberLocationOpener(): LocationOpener = remember { IosLocationOpener() }

private class IosLocationOpener : LocationOpener {
    @Suppress("DEPRECATION")
    override fun open(location: CurrentLocation): Result<Unit> =
        runCatching {
            val coordinates = "${location.latitude},${location.longitude}"
            val url = requireNotNull(NSURL(string = "http://maps.apple.com/?ll=$coordinates"))
            val application = UIApplication.sharedApplication
            check(application.canOpenURL(url)) { "No map application is available" }
            check(application.openURL(url)) { "Location could not be opened" }
        }
}
