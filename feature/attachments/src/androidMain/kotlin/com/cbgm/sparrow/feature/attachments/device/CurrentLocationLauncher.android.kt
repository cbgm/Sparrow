package com.cbgm.sparrow.feature.attachments.device

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.cbgm.sparrow.feature.attachments.domain.model.CurrentLocation

@Composable
actual fun rememberCurrentLocationLauncher(
    onLocation: (CurrentLocation) -> Unit,
    onError: (String) -> Unit
): CurrentLocationLauncher {
    val context = LocalContext.current
    val currentOnLocation = rememberUpdatedState(onLocation)
    val currentOnError = rememberUpdatedState(onError)

    val requestCurrentLocation = remember(context) {
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

    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val provider = locationManager.currentLocationProvider(context)
    if (provider == null) {
        onError("Current location is unavailable")
        return
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        locationManager.getCurrentLocation(
            provider,
            CancellationSignal(),
            context.mainExecutor
        ) { location ->
            if (location == null) {
                onError("Current location is unavailable")
            } else {
                onLocation(CurrentLocation(latitude = location.latitude, longitude = location.longitude))
            }
        }
    } else {
        @Suppress("DEPRECATION")
        locationManager.requestSingleUpdate(
            provider,
            object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    onLocation(CurrentLocation(latitude = location.latitude, longitude = location.longitude))
                }

                @Deprecated("Deprecated in Android")
                override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit

                override fun onProviderEnabled(provider: String) = Unit

                override fun onProviderDisabled(provider: String) {
                    onError("Current location is unavailable")
                }
            },
            Looper.getMainLooper()
        )
    }
}

private fun LocationManager.currentLocationProvider(context: Context): String? {
    val fineGranted =
        context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    return when {
        fineGranted && isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        fineGranted -> getProviders(true).firstOrNull()
        else -> getProviders(true).firstOrNull { provider -> provider != LocationManager.GPS_PROVIDER }
    }
}

private fun Context.hasLocationPermission(): Boolean =
    checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
