package com.unh.parkit_android26_myles_syed.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unh.parkit_android26_myles_syed.data.AccountRepository
import com.unh.parkit_android26_myles_syed.data.Vehicle

private val ButtonBlue = Color(0xFF255898)
private val CheckGreen = Color(0xFF3A8A5C)

@Composable
fun ProfileScreen(onSignOut: () -> Unit) {
    var vehicle by remember { mutableStateOf(Vehicle()) }
    var showDialog by remember { mutableStateOf(false) }
    val email = remember { AccountRepository.email }

    // Live-updates whenever the signed-in account's vehicle changes in Firestore.
    DisposableEffect(Unit) {
        val stopListening = AccountRepository.listenVehicle { vehicle = it }
        onDispose { stopListening() }
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Icon(
            Icons.Outlined.AccountCircle,
            contentDescription = "Profile",
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(32.dp))
        Text("Email:", fontSize = 28.sp)
        Text(email, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(40.dp))

        Button(
            onClick = { showDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = ButtonBlue, contentColor = Color.White),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text("Add New Vehicle", fontWeight = FontWeight.Bold) }
        Text(
            "You can change access anytime in Settings.",
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        Spacer(Modifier.height(32.dp))

        VehicleRow("Make", vehicle.make, "Insert Make of vehicle used to park.")
        VehicleRow("Model", vehicle.model, "Insert Model of vehicle used to park.")
        VehicleRow("License Plate", vehicle.plate, "Insert License Plate #.")

        Spacer(Modifier.weight(1f))
        Button(
            onClick = onSignOut,
            colors = ButtonDefaults.buttonColors(containerColor = ButtonBlue, contentColor = Color.White),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text("Exit", fontWeight = FontWeight.Bold) }
    }

    if (showDialog) {
        AddVehicleDialog(replacing = !vehicle.isEmpty, onDismiss = { showDialog = false })
    }
}

@Composable
private fun VehicleRow(label: String, value: String, hint: String) {
    val filled = value.isNotBlank()
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.Bold)
            if (filled) Text(value, fontSize = 15.sp)
            else Text(hint, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = if (filled) "$label saved" else "$label missing",
            tint = if (filled) CheckGreen else MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
private fun AddVehicleDialog(replacing: Boolean, onDismiss: () -> Unit) {
    var make by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var plate by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Add New Vehicle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (replacing) {
                    Text("This replaces the vehicle currently saved to your account.", fontSize = 13.sp)
                }
                OutlinedTextField(
                    make, { make = it }, label = { Text("Make") },
                    singleLine = true, enabled = !saving, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    model, { model = it }, label = { Text("Model") },
                    singleLine = true, enabled = !saving, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    plate, { plate = it }, label = { Text("License Plate") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    singleLine = true, enabled = !saving, modifier = Modifier.fillMaxWidth()
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving && make.isNotBlank() && model.isNotBlank() && plate.isNotBlank(),
                onClick = {
                    saving = true
                    error = null
                    AccountRepository.saveVehicle(
                        Vehicle(make.trim(), model.trim(), plate.trim().uppercase())
                    ) { result ->
                        saving = false
                        if (result == null) onDismiss() else error = result
                    }
                }
            ) { Text(if (saving) "Saving…" else "Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") }
        }
    )
}
