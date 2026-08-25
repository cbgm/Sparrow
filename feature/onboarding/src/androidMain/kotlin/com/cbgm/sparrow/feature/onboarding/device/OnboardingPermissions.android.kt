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

    val permissions =
        remember {
            buildList {
                /*
                 * READ_CONTACTS:
                 * Required for loading contacts and duplicate detection.
                 *
                 * WRITE_CONTACTS:
                 * Required for directly inserting a contact without
                 * opening the system contact editor.
                 */
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
        }

    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->

            val contactsReadGranted =
                result[Manifest.permission.READ_CONTACTS] == true ||
                    context.isGranted(Manifest.permission.READ_CONTACTS)

            val contactsWriteGranted =
                result[Manifest.permission.WRITE_CONTACTS] == true ||
                    context.isGranted(Manifest.permission.WRITE_CONTACTS)

            onResult(
                PermissionRequestResult(
                    /*
                     * Treat contacts as granted only when the app can
                     * both check for duplicates and insert contacts.
                     */
                    contactsGranted =
                        contactsReadGranted &&
                            contactsWriteGranted,
                    cameraGranted =
                        result[Manifest.permission.CAMERA] == true ||
                            context.isGranted(
                                Manifest.permission.CAMERA
                            ),
                    audioGranted =
                        result[Manifest.permission.RECORD_AUDIO] == true ||
                            context.isGranted(
                                Manifest.permission.RECORD_AUDIO
                            ),
                    notificationsGranted =
                        Build.VERSION.SDK_INT <
                            Build.VERSION_CODES.TIRAMISU ||
                            result[
                                Manifest.permission.POST_NOTIFICATIONS
                            ] == true ||
                            context.isGranted(
                                Manifest.permission.POST_NOTIFICATIONS
                            ),
                    phoneNumberGranted =
                        (
                            result[
                                Manifest.permission.READ_PHONE_NUMBERS
                            ] == true ||
                                context.isGranted(
                                    Manifest.permission.READ_PHONE_NUMBERS
                                )
                        ) && (
                            result[
                                Manifest.permission.READ_PHONE_STATE
                            ] == true ||
                                context.isGranted(
                                    Manifest.permission.READ_PHONE_STATE
                                )
                        )
                )
            )
        }

    LaunchedEffect(requestId) {
        if (requestId > 0) {
            launcher.launch(permissions)
        }
    }
}

@SuppressLint(
    "MissingPermission",
    "HardwareIds"
)
@Composable
actual fun AutomaticPhoneNumberReader(
    requestId: Int,
    enabled: Boolean,
    onResult: (AutomaticPhoneNumberResult) -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(
        requestId,
        enabled
    ) {
        if (!enabled || requestId <= 0) {
            return@LaunchedEffect
        }

        if (
            !context.isGranted(
                Manifest.permission.READ_PHONE_NUMBERS
            ) || !context.isGranted(
                Manifest.permission.READ_PHONE_STATE
            )
        ) {
            onResult(
                AutomaticPhoneNumberResult.Unavailable
            )
            return@LaunchedEffect
        }

        val number =
            runCatching {
                val manager =
                    context.getSystemService(
                        Context.TELEPHONY_SUBSCRIPTION_SERVICE
                    ) as SubscriptionManager

                val subscriptions =
                    manager.activeSubscriptionInfoList.orEmpty()

                subscriptions.firstNotNullOfOrNull { info ->
                    val value =
                        if (
                            Build.VERSION.SDK_INT >=
                            Build.VERSION_CODES.TIRAMISU
                        ) {
                            manager.getPhoneNumber(
                                info.subscriptionId
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            (
                                context.getSystemService(
                                    Context.TELEPHONY_SERVICE
                                ) as TelephonyManager
                            ).createForSubscriptionId(
                                info.subscriptionId
                            ).line1Number
                        }

                    value
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                }
            }.getOrElse { throwable ->
                onResult(
                    AutomaticPhoneNumberResult.Failed(
                        throwable.message
                            ?: "SIM phone number could not be read"
                    )
                )
                return@LaunchedEffect
            }

        if (number == null) {
            onResult(
                AutomaticPhoneNumberResult.Unavailable
            )
        } else {
            onResult(
                AutomaticPhoneNumberResult.Found(
                    number
                )
            )
        }
    }
}

private fun Context.isGranted(permission: String): Boolean =
    ContextCompat.checkSelfPermission(
        this,
        permission
    ) == PackageManager.PERMISSION_GRANTED
