package com.unh.parkit_android26_myles_syed.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unh.parkit_android26_myles_syed.ui.parking.ParkingRepository
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import androidx.compose.ui.platform.LocalLocale

private val SessNavy = Color(0xFF0B2A5B)
private val SessGold = Color(0xFFF2B33D)
private val SessBg = Color(0xFFF1F6FA)
private val SessBlue = Color(0xFF0B5CA8)
private val SessGreen = Color(0xFF2E7D5B)
private val SessGreenBg = Color(0xFFE3F3EA)
private val SessRed = Color(0xFFC0392B)
private val SessGray = Color(0xFF6B7C8C)

//IF ANOTHER USER IS LOOKING AT THE SCREEN THE SPOT YOU CHOSE
// SHOULD BE RED ON THEIR SCREEN FIREbase

@Composable
fun SessionScreen(onFindParking: () -> Unit, onSessionEnded: () -> Unit) {
    val session = ParkingRepository.session
    val context = LocalContext.current

    if (session == null) {
        Column(
            Modifier.fillMaxSize().background(SessBg).statusBarsPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("No active session", fontSize = 22.sp, fontWeight = FontWeight.Medium, color = Color.Black)
            Spacer(Modifier.height(8.dp))
            Text("Scan a lot barcode to start parking.", color = SessGray)
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onFindParking,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SessBlue, contentColor = Color.White)
            ) { Text("Find parking") }
        }
        return
    }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(session) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val elapsed = ((now - session.startMillis) / 1000).coerceAtLeast(0)
    val timeText = "%02d:%02d".format(elapsed / 3600, (elapsed % 3600) / 60)
    val progress = (elapsed % 3600) / 3600f
    val startedText = SimpleDateFormat("h:mm a", LocalLocale.current.platformLocale).format(Date(session.startMillis))
    val spotLabel = ParkingRepository.spotLabel(session.spot)

    Column(Modifier.fillMaxSize().background(SessBg).statusBarsPadding().padding(16.dp)) {
        Text(
            "Parking session", fontSize = 22.sp, fontWeight = FontWeight.Medium,
            color = Color.Black, modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // Timer card
        Column(
            Modifier.fillMaxWidth().background(SessNavy, RoundedCornerShape(24.dp)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(shape = RoundedCornerShape(50), color = SessGreenBg) {
                Text(
                    "PARKING ACTIVE", color = SessGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 14.dp.toPx()
                    val topLeft = Offset(stroke / 2, stroke / 2)
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(
                        SessGold.copy(alpha = 0.25f), 0f, 360f, false,
                        topLeft = topLeft, size = arcSize, style = Stroke(stroke)
                    )
                    drawArc(
                        SessGold, -90f, 360f * progress, false,
                        topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(timeText, color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                    Text("elapsed time", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(session.lot.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text("Spot $spotLabel · Started $startedText", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
        }

        Spacer(Modifier.height(16.dp))

        // Car saved card
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).background(Color(0xFFDDEBF7), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = SessBlue)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Your car is saved", fontWeight = FontWeight.Bold, color = SessNavy, fontSize = 16.sp)
                    Text("Row ${ParkingRepository.rowLabel(session.spot)}, ${session.lot.name}", color = SessGray, fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { openDirections(context, session.lot) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDDEBF7), contentColor = SessNavy)
        ) {
            Icon(Icons.Default.Person, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Directions back to my car", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                ParkingRepository.endSession() // spot turns green again
                onSessionEnded()
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SessRed, contentColor = Color.White)
        ) {
            Icon(Icons.Default.StopCircle, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("End parking", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}