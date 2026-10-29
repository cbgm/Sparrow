package com.cbgm.sparrow.feature.applock.device

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun AppLockBackHandler() {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    BackHandler { activity?.moveTaskToBack(true) }
}
