package com.cbgm.sparrow.core.ui.device.background

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.cbgm.sparrow.core.ui.component.SparrowOutlinedButton
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.core_background_help_open
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun BackgroundRestrictionHelp(modifier: Modifier) {
    val maker = PhoneMaker.of(Build.MANUFACTURER) ?: return
    val context = LocalContext.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
        Text(
            text = stringResource(maker.instruction()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SparrowOutlinedButton(
            onClick = { context.openBackgroundSettings(maker) },
            text = stringResource(Res.string.core_background_help_open),
            fillMaxWidth = false
        )
    }
}

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
