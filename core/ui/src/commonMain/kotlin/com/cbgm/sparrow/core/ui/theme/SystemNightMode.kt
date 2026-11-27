package com.cbgm.sparrow.core.ui.theme

import androidx.compose.runtime.Composable

/** Hands the chosen scheme to the system, so the splash screen at the next start already matches it. */
@Composable
expect fun SystemNightMode(mode: AppThemeMode)
