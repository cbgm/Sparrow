package com.cbgm.sparrow.feature.attachments.device

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.cbgm.sparrow.feature.attachments.domain.model.CurrentLocation
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.Foundation.NSError
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberCurrentLocationLauncher(
    onLocation: (CurrentLocation) -> Unit,
    onError: (String) -> Unit
): CurrentLocationLauncher {
    val currentOnLocation = rememberUpdatedState(onLocation)
    val currentOnError = rememberUpdatedState(onError)
    val locationManager = remember { CLLocationManager() }
    val delegate =
        remember(locationManager) {
            CurrentLocationDelegate(
                locationManager = locationManager,
                onLocation = { currentOnLocation.value(it) },
                onError = { currentOnError.value(it) }
            )
        }

    DisposableEffect(locationManager, delegate) {
        locationManager.delegate = delegate
        onDispose { locationManager.delegate = null }
    }

    return remember(delegate) {
        CurrentLocationLauncher { delegate.requestCurrentLocation() }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class CurrentLocationDelegate(
    private val locationManager: CLLocationManager,
    private val onLocation: (CurrentLocation) -> Unit,
    private val onError: (String) -> Unit
) : NSObject(), CLLocationManagerDelegateProtocol {
    fun requestCurrentLocation() {
        when (CLLocationManager.authorizationStatus()) {
            kCLAuthorizationStatusAuthorizedAlways,
            kCLAuthorizationStatusAuthorizedWhenInUse -> locationManager.requestLocation()

            kCLAuthorizationStatusNotDetermined -> locationManager.requestWhenInUseAuthorization()
            kCLAuthorizationStatusDenied,
            kCLAuthorizationStatusRestricted ->
                onError("Location permission is required to share the current location")

            else -> onError("Current location is unavailable")
        }
    }

    override fun locationManager(
        manager: CLLocationManager,
        didChangeAuthorizationStatus: CLAuthorizationStatus
    ) {
        when (didChangeAuthorizationStatus) {
            kCLAuthorizationStatusAuthorizedAlways,
            kCLAuthorizationStatusAuthorizedWhenInUse -> locationManager.requestLocation()

            kCLAuthorizationStatusDenied,
            kCLAuthorizationStatusRestricted ->
                onError("Location permission is required to share the current location")

            else -> Unit
        }
    }

    override fun locationManager(
        manager: CLLocationManager,
        didUpdateLocations: List<*>
    ) {
        val location = didUpdateLocations.lastOrNull() as? CLLocation
        if (location == null) {
            onError("Current location is unavailable")
            return
        }
        onLocation(
            CurrentLocation(
                latitude = location.coordinate.latitude,
                longitude = location.coordinate.longitude
            )
        )
    }

    override fun locationManager(
        manager: CLLocationManager,
        didFailWithError: NSError
    ) {
        onError(didFailWithError.localizedDescription)
    }
}
