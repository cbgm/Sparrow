package com.cbgm.sparrow.feature.applock.device

import androidx.compose.runtime.Composable

/** Back on the lock screen sends the app to the background, like Home, instead of revealing the screen below. */
@Composable
expect fun AppLockBackHandler()
