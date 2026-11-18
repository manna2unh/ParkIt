package com.unh.parkit_android26_myles_syed.ui.screens

import android.annotation.SuppressLint
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

private const val DRIVE_SPEED_MPS = 4.4f
private const val STOPPED_SPEED_MPS = 1.5f
private const val FAST_READINGS_TO_START = 3
private const val SLOW_READINGS_TO_STOP = 60
private const val MOVING_WINDOW_MS = 10_000L

class DrivingState(
    val isDriving: State<Boolean>,
    private val lastFastMillis: State<Long>
) {
    fun movingNow(): Boolean {
        val last = lastFastMillis.value
        return last > 0L && SystemClock.elapsedRealtime() - last < MOVING_WINDOW_MS
    }
}

@SuppressLint("MissingPermission")
@Composable
fun rememberDrivingState(enabled: Boolean): DrivingState {
    val context = LocalContext.current
    val driving = remember { mutableStateOf(false) }
    val lastFast = remember { mutableStateOf(0L) }

    DisposableEffect(enabled) {
        if (!enabled) {
            driving.value = false
            lastFast.value = 0L
            return@DisposableEffect onDispose { }
        }
        val client = LocationServices.getFusedLocationProviderClient(context)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L).build()
        var fast = 0
        var slow = 0
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                if (!loc.hasSpeed()) return
                if (loc.speed >= DRIVE_SPEED_MPS) lastFast.value = SystemClock.elapsedRealtime()
                when {
                    loc.speed >= DRIVE_SPEED_MPS -> { fast++; slow = 0 }
                    loc.speed < STOPPED_SPEED_MPS -> { slow++; fast = 0 }
                    else -> { fast = 0; slow = 0 }
                }
                if (fast >= FAST_READINGS_TO_START) driving.value = true
                if (slow >= SLOW_READINGS_TO_STOP) driving.value = false
            }
        }
        try {
            client.requestLocationUpdates(request, callback, android.os.Looper.getMainLooper())
        } catch (_: SecurityException) {
        }
        onDispose { client.removeLocationUpdates(callback) }
    }
    return remember { DrivingState(driving, lastFast) }
}