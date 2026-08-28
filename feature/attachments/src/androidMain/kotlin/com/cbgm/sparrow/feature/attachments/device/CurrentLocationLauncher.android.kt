package com.cbgm.sparrow.feature.attachments.device

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.cbgm.sparrow.feature.attachments.domain.model.CurrentLocation
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

@Composable
actual fun rememberCurrentLocationLauncher(
    onLocation: (CurrentLocation) -> Unit,
    onError: (String) -> Unit
): CurrentLocationLauncher {
    val context = LocalContext.current
    val currentOnLocation = rememberUpdatedState(onLocation)
    val currentOnError = rememberUpdatedState(onError)

    val requestCurrentLocation =
        remember(context) {
            {
                requestCurrentLocation(
                    context = context,
                    onLocation = { currentOnLocation.value(it) },
                    onError = { currentOnError.value(it) }
                )
            }
        }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            val granted =
                result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    result[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
                    context.hasLocationPermission()
            if (granted) {
                requestCurrentLocation()
            } else {
                currentOnError.value("Location permission is required to share the current location")
            }
        }

    return remember(context, permissionLauncher, requestCurrentLocation) {
        CurrentLocationLauncher {
            if (context.hasLocationPermission()) {
                requestCurrentLocation()
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun requestCurrentLocation(
    context: Context,
    onLocation: (CurrentLocation) -> Unit,
    onError: (String) -> Unit
) {
    if (!context.hasLocationPermission()) {
        onError("Location permission is required to share the current location")
        return
    }

    val priority =
        if (context.hasFineLocationPermission()) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }
    val request =
        CurrentLocationRequest.Builder()
            .setPriority(priority)
            .setMaxUpdateAgeMillis(MAX_LOCATION_AGE_MILLISECONDS)
            .setDurationMillis(MAX_LOCATION_WAIT_MILLISECONDS)
            .build()
    val cancellationTokenSource = CancellationTokenSource()

    LocationServices
        .getFusedLocationProviderClient(context)
        .getCurrentLocation(request, cancellationTokenSource.token)
        .addOnSuccessListener { location ->
            if (location == null) {
                onError("Current location is unavailable")
            } else {
                onLocation(
                    CurrentLocation(
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                )
            }
        }
        .addOnFailureListener { error ->
            onError(error.message ?: "Current location is unavailable")
        }
}

private fun Context.hasFineLocationPermission(): Boolean =
    checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

private fun Context.hasLocationPermission(): Boolean =
    hasFineLocationPermission() ||
        checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

private const val MAX_LOCATION_AGE_MILLISECONDS = 30_000L
private const val MAX_LOCATION_WAIT_MILLISECONDS = 10_000L
