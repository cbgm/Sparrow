package com.cbgm.sparrow.feature.applock.device

import androidx.compose.runtime.Composable

/** iOS has no system back action. */
@Composable
actual fun AppLockBackHandler() = Unit
