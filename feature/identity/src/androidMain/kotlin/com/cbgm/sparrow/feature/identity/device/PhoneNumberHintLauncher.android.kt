package com.cbgm.sparrow.feature.identity.device

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.identity.GetPhoneNumberHintIntentRequest
import com.google.android.gms.auth.api.identity.Identity

@Composable
actual fun PhoneNumberHintLauncher(
    requestId: Int,
    enabled: Boolean,
    onResult: (PhoneNumberHintResult) -> Unit
) {
    val context = LocalContext.current

    val currentOnResult = rememberUpdatedState(newValue = onResult)

    val activity = remember(context) { context.findActivity() }

    val signInClient = remember(activity) { activity?.let { Identity.getSignInClient(it) } }

    val resultLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->

            if (result.resultCode != Activity.RESULT_OK) {
                currentOnResult.value(PhoneNumberHintResult.Cancelled)

                return@rememberLauncherForActivityResult
            }

            val selectedPhoneNumber =
                runCatching {
                    signInClient?.getPhoneNumberFromIntent(result.data)
                }.getOrNull()
                    ?.trim()
                    .orEmpty()

            if (selectedPhoneNumber.isBlank()) {
                currentOnResult.value(PhoneNumberHintResult.Unavailable)
            } else {
                currentOnResult.value(PhoneNumberHintResult.Selected(phoneNumber = selectedPhoneNumber))
            }
        }

    LaunchedEffect(
        requestId,
        enabled,
        signInClient
    ) {
        if (!enabled || requestId <= 0) return@LaunchedEffect

        val client =
            signInClient ?: run {
                currentOnResult.value(
                    PhoneNumberHintResult.Failed(message = "Phone number picker requires an Android activity")
                )

                return@LaunchedEffect
            }

        val request = GetPhoneNumberHintIntentRequest.builder().build()

        client
            .getPhoneNumberHintIntent(request)
            .addOnSuccessListener { pendingIntent ->
                resultLauncher.launch(
                    IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                )
            }.addOnFailureListener { error ->
                currentOnResult.value(
                    PhoneNumberHintResult.Failed(
                        message = error.message ?: "Phone number picker is unavailable"
                    )
                )
            }
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> {
            this
        }

        is ContextWrapper -> {
            baseContext.findActivity()
        }

        else -> {
            null
        }
    }
