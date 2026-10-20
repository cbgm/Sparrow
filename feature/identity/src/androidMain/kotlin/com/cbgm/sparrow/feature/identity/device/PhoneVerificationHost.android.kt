package com.cbgm.sparrow.feature.identity.device

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.cbgm.sparrow.feature.identity.data.repository.FirebasePhoneVerificationRepository
import org.koin.compose.koinInject

@Composable
actual fun PhoneVerificationHost() {
    val repository = koinInject<FirebasePhoneVerificationRepository>()
    val activity = LocalContext.current.findActivity() ?: return
    DisposableEffect(repository, activity) {
        repository.attach(activity)
        onDispose { repository.detach(activity) }
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
