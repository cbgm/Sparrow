package com.cbgm.sparrow.feature.identity.device

import androidx.compose.runtime.Composable

/** Gives the platform SMS verification the screen it may need (for example a reCAPTCHA check). */
@Composable
expect fun PhoneVerificationHost()
