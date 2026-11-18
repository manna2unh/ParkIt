package com.unh.parkit_android26_myles_syed.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.unh.parkit_android26_myles_syed.ui.parking.ParkingRepository

private val DetailBlue = Color(0xFF0B5CA8)
private val DetailBg = Color(0xFFF1F6FA)
private val DetailGreen = Color(0xFF2E7D5B)
private val DetailGreenBg = Color(0xFFE3F3EA)
private val TextGray = Color(0xFF6B7C8C)


@Composable
fun LotDetailScreen(lot: Lot, onBack: () -> Unit) {
    val open = ParkingRepository.openCount(lot)
    val context = LocalContext.current

    val hasPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }
    val userLocation by rememberUserLocation(hasPermission)
    val meters = userLocation?.let { distanceMeters(it, lot.position) }
    val distanceText = meters?.let { "%.1f mi".format(it / 1609.34f) } ?: "--"
    val walkText = meters?.let { "${(it / 80).toInt().coerceAtLeast(1)} min" } ?: "--"

    val (statusText, statusColor, statusBg) = when {
        open == 0 -> Triple("Full", Color(0xFFB3261E), Color(0xFFFCE4E2))
        open.toFloat() / lot.spots < 0.25f -> Triple("Filling up", Color(0xFF9A6A00), Color(0xFFFFF1D0))
        else -> Triple("Plenty available", DetailGreen, DetailGreenBg)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(DetailBg)
            .statusBarsPadding()
    ) {
        // Top bar
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.Black,
                modifier = Modifier.size(28.dp).clickable { onBack() }
            )
            Spacer(Modifier.width(12.dp))
            Text(lot.name, fontSize = 22.sp, fontWeight = FontWeight.Medium, color = Color.Black)
        }

        Column(Modifier.weight(1f)) {
            // Static-ish map preview
            val cameraState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(lot.position, 18f)
            }
            GoogleMap(
                modifier = Modifier.fillMaxWidth().height(150.dp),
                cameraPositionState = cameraState,
                googleMapOptionsFactory = { GoogleMapOptions().liteMode(true) },
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    scrollGesturesEnabled = false,
                    zoomGesturesEnabled = false,
                    rotationGesturesEnabled = false,
                    tiltGesturesEnabled = false,
                    myLocationButtonEnabled = false
                )
            ) {
                Marker(state = rememberMarkerState(position = lot.position))
            }

            Column(Modifier.weight(1f).padding(16.dp)) {
                // Open count + status pill
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text("$open", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = DetailGreen)
                        Text("open of ${lot.spots} spaces", color = TextGray, fontSize = 15.sp)
                    }
                    Surface(shape = RoundedCornerShape(50), color = statusBg) {
                        Text(
                            statusText,
                            color = statusColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Distance / walk cards
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(Modifier.weight(1f), Icons.Default.NorthEast, distanceText, "distance")
                    StatCard(Modifier.weight(1f), Icons.Default.Person, walkText, "walk")
                }

                // Buttons centered in the remaining space
                Column(
                    Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.Center
                ) {
                    // Directions
                    Button(
                        onClick = { openDirections(context, lot) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DetailBlue, contentColor = Color.White)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Directions", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Spacer(Modifier.height(12.dp))

                    // Notify (placeholder)
                    Button(
                        onClick = { /* TODO: notification settings */ },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDDEBF7),
                            contentColor = Color(0xFF0B2A5B)
                        )
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Notify me when a spot opens", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(20.dp), color = Color.White) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = DetailBlue)
            Spacer(Modifier.height(12.dp))
            Text(value, fontSize = 18.sp, color = Color.Black)
            Text(label, fontSize = 13.sp, color = TextGray)
        }
    }
}

internal fun openDirections(context: android.content.Context, lot: Lot, mode: String = "w") {
    val lat = lot.position.latitude
    val lng = lot.position.longitude
    val navIntent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$lat,$lng&mode=$mode"))
        .setPackage("com.google.android.apps.maps")
    try {
        context.startActivity(navIntent)
    } catch (e: Exception) {
        // Fallback: any maps app
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(lot.name)})")))
        } catch (_: Exception) { }
    }
}