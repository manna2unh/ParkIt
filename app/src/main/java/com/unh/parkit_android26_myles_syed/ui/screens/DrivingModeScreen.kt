package com.unh.parkit_android26_myles_syed.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unh.parkit_android26_myles_syed.ui.parking.ParkingRepository

private val DriveBg = Color(0xFF062B5B)
private val DriveNavy = Color(0xFF0B2A5B)
private val DriveGold = Color(0xFFF2B33D)
private val DriveBlue = Color(0xFF0B5CA8)
private val DriveGreen = Color(0xFF2E7D5B)
private val DriveGreenBg = Color(0xFFE3F3EA)
private val DriveGray = Color(0xFF6B7C8C)
private val DrivePanel = Color.White.copy(alpha = 0.08f)

private const val AVG_DRIVE_MPS = 8f // ~18 mph campus driving

@Composable
fun DrivingModeScreen(onExit: () -> Unit) {
    BackHandler(onBack = onExit)

    val context = LocalContext.current
    val userLocation by rememberUserLocation(true)

    val sorted = remember(userLocation) {
        userLocation?.let { u -> lots.sortedBy { distanceMeters(u, it.position) } } ?: lots
    }
    var index by remember { mutableIntStateOf(0) }
    val lot = sorted[index % sorted.size]
    val open = ParkingRepository.openCount(lot)

    val meters = userLocation?.let { distanceMeters(it, lot.position) }
    val distText = meters?.let { "%.1f MI".format(it / 1609.34f) } ?: "-- MI"
    val driveText = meters
        ?.let { "${(it / AVG_DRIVE_MPS / 60).toInt().coerceAtLeast(1)} min drive" } ?: "-- min drive"
    val label = if (index % sorted.size == 0) "NEAREST LOT" else "NEXT NEAREST"

    Column(
        Modifier
            .fillMaxSize()
            .background(DriveBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        // Header
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(DriveGold, CircleShape))
            Spacer(Modifier.width(10.dp))
            Text(
                "Driving Mode", color = Color.White, fontSize = 26.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.White.copy(alpha = 0.12f),
                modifier = Modifier.clickable { onExit() }
            ) {
                Text(
                    "Exit", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Outlined.Shield, null, tint = DriveGold, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                "Simplified controls while you drive. Keep your eyes on the road.",
                color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp
            )
        }

        Spacer(Modifier.weight(1f))

        // Nearest lot card
        Surface(shape = RoundedCornerShape(28.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "$label · $distText", color = DriveBlue,
                        fontWeight = FontWeight.Bold, fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(shape = RoundedCornerShape(50), color = DriveGreenBg) {
                        Text(
                            "$open OPEN", color = DriveGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(lot.name, color = DriveNavy, fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp)
                Spacer(Modifier.height(8.dp))
                Text(driveText, color = DriveGray, fontSize = 16.sp)
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { openDirections(context, lot, mode = "d") },
                    modifier = Modifier.fillMaxWidth().height(72.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DriveGold, contentColor = DriveNavy)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                    Spacer(Modifier.width(12.dp))
                    Text("Navigate", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Next lot
        Column(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(DrivePanel, RoundedCornerShape(24.dp))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                .clickable { index++ },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White)
            Spacer(Modifier.height(8.dp))
            Text("Next lot", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(Modifier.weight(1f))

        // Departure banner
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF1B4A7A), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.DirectionsCar, null, tint = DriveGold)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Automatic departure detection is on", color = Color.White,
                    fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Your session ends when you leave the lot.",
                    color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }
    }
}