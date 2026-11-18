package com.unh.parkit_android26_myles_syed.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class PermissionItem(val title: String, val description: String)

private val items = listOf(
    PermissionItem("Notifications", "Know when a spot opens nearby."),
    PermissionItem("Camera", "Scan a lot barcode to secure a spot."),
    PermissionItem("Motion & activity", "Detect driving and departure automatically for safety.")
)

@Composable
fun WelcomeScreen(onContinue: () -> Unit) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { onContinue() }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Spacer(Modifier.height(48.dp))
            Text("ParkIt · UNH", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            Text("Park smarter on campus.", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Live lot availability & easier parking, and a safer drive across the University of New Haven.")
            Spacer(Modifier.height(32.dp))
            items.forEach {
                Column(Modifier.padding(vertical = 8.dp)) {
                    Text(it.title, fontWeight = FontWeight.SemiBold)
                    Text(it.description, fontSize = 13.sp)
                }
            }
        }
        Button(
            onClick = {
                launcher.launch(
                    arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.POST_NOTIFICATIONS,
                        Manifest.permission.ACTIVITY_RECOGNITION
                    )
                )
            },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text("Allow & continue") }
    }
}