package com.cbgm.sparrow.core.ui.device.background

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** iOS keeps no connection while Sparrow is closed, so there is nothing to set. */
@Composable
actual fun BackgroundRestrictionHelp(modifier: Modifier) = Unit
