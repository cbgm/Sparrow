package com.cbgm.sparrow.feature.onboarding.device

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
actual fun OnboardingPermissionRequester(
    requestId: Int,
    onResult: (PermissionRequestResult) -> Unit
) {
    val context = LocalContext.current
    val permissions = remember { onboardingPermissions() }

    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            onResult(
                PermissionRequestResult(
                    contactsGranted =
                        context.isGranted(result, Manifest.permission.READ_CONTACTS) &&
                            context.isGranted(result, Manifest.permission.WRITE_CONTACTS),
                    cameraGranted =
                        context.isGranted(result, Manifest.permission.CAMERA),
                    audioGranted =
                        context.isGranted(result, Manifest.permission.RECORD_AUDIO),
                    notificationsGranted =
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                            context.isGranted(result, Manifest.permission.POST_NOTIFICATIONS),
                    phoneNumberGranted =
                        context.isGranted(result, Manifest.permission.READ_PHONE_NUMBERS) &&
                            context.isGranted(result, Manifest.permission.READ_PHONE_STATE)
                )
            )
        }

    LaunchedEffect(requestId) {
        if (requestId > 0) {
            launcher.launch(permissions)
        }
    }
}

@SuppressLint("MissingPermission", "HardwareIds")
@Composable
actual fun AutomaticPhoneNumberReader(
    requestId: Int,
    enabled: Boolean,
    onResult: (AutomaticPhoneNumberResult) -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(requestId, enabled) {
        if (!enabled || requestId <= 0) {
            return@LaunchedEffect
        }

        if (!context.hasPhoneNumberPermission()) {
            onResult(AutomaticPhoneNumberResult.Unavailable)
            return@LaunchedEffect
        }

        val number =
            runCatching {
                context.readPhoneNumber()
            }.getOrElse { error ->
                onResult(
                    AutomaticPhoneNumberResult.Failed(
                        error.message ?: "SIM phone number could not be read"
                    )
                )
                return@LaunchedEffect
            }

        onResult(
            number
                ?.let(AutomaticPhoneNumberResult::Found)
                ?: AutomaticPhoneNumberResult.Unavailable
        )
    }
}

private fun onboardingPermissions(): Array<String> =
    buildList {
        add(Manifest.permission.READ_CONTACTS)
        add(Manifest.permission.WRITE_CONTACTS)
        add(Manifest.permission.CAMERA)
        add(Manifest.permission.RECORD_AUDIO)
        add(Manifest.permission.READ_PHONE_NUMBERS)
        add(Manifest.permission.READ_PHONE_STATE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

@SuppressLint("MissingPermission", "HardwareIds")
private fun Context.readPhoneNumber(): String? {
    val subscriptionManager =
        getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager

    return subscriptionManager.activeSubscriptionInfoList
        .orEmpty()
        .firstNotNullOfOrNull { subscription ->
            val number =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    subscriptionManager.getPhoneNumber(subscription.subscriptionId)
                } else {
                    @Suppress("DEPRECATION")
                    (getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager)
                        .createForSubscriptionId(subscription.subscriptionId)
                        .line1Number
                }

            number?.trim()?.takeIf(String::isNotBlank)
        }
}

private fun Context.hasPhoneNumberPermission(): Boolean =
    isGranted(Manifest.permission.READ_PHONE_NUMBERS) &&
        isGranted(Manifest.permission.READ_PHONE_STATE)

private fun Context.isGranted(
    result: Map<String, Boolean>,
    permission: String
): Boolean =
    result[permission] == true || isGranted(permission)

private fun Context.isGranted(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) ==
        PackageManager.PERMISSION_GRANTED
