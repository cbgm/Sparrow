package com.cbgm.sparrow.core.ui.device.background

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.cbgm.sparrow.core.ui.component.SparrowOutlinedButton
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.core_background_help_open
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import java.io.File

@Composable
actual fun BackgroundRestrictionHelp(modifier: Modifier) {
    val context = LocalContext.current
    val maker by produceState(PhoneMaker.of(Build.MANUFACTURER)) {
        value = withContext(Dispatchers.IO) { context.debugPhoneMaker() } ?: value
    }
    val shownMaker = maker ?: return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
        Text(
            text = stringResource(shownMaker.instruction()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SparrowOutlinedButton(
            onClick = { context.openBackgroundSettings(shownMaker) },
            text = stringResource(Res.string.core_background_help_open),
            fillMaxWidth = false
        )
    }
}

/**
 * Debug builds only, for trying the help on an emulator: the maker named in the file [DEBUG_PHONE_MAKER_FILE] in the
 * app's files, written with adb (commands in docs/push-notifications.md; delete the file to go back). `run-as` works only for debuggable apps, and release builds never read the file.
 */
private fun Context.debugPhoneMaker(): PhoneMaker? {
    val debuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    val file = File(filesDir, DEBUG_PHONE_MAKER_FILE)
    return if (debuggable && file.isFile) PhoneMaker.of(file.readText()) else null
}

private const val DEBUG_PHONE_MAKER_FILE = "debug_phone_maker"

/** The maker's screen, the first that opens; Sparrow's app settings when none does. */
private fun Context.openBackgroundSettings(maker: PhoneMaker) {
    val screens =
        maker.settingsScreens.map { (packageName, activity) -> Intent().setComponent(ComponentName(packageName, activity)) } +
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri())
    screens.any { screen -> opens(screen) }
}

/** Some of these screens are missing on a given system version, others are not exported to other apps. */
private fun Context.opens(screen: Intent): Boolean =
    try {
        startActivity(screen.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
