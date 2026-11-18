package com.unh.parkit_android26_myles_syed.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.unh.parkit_android26_myles_syed.ui.parking.ParkingRepository

//CURRENTLY HAVE PLACEHOLDERS FOR LOT SPACES

private val ParkBlue = Color(0xFF0B5CA8)
private val ParkGold = Color(0xFFF2B33D)
private val OpenGreen = Color(0xFF2E7D5B)
private val OpenGreenBg = Color(0xFFE3F3EA)

@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    onScanClick: () -> Unit = {},
    onLotClick: (Lot) -> Unit = {},
    onDrivingClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val fine = Manifest.permission.ACCESS_FINE_LOCATION
    val coarse = Manifest.permission.ACCESS_COARSE_LOCATION


    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, fine) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, coarse) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result -> hasPermission = result.values.any { it } }

    LaunchedEffect(Unit) { if (!hasPermission) launcher.launch(arrayOf(fine, coarse)) }

    val userLocation by rememberUserLocation(hasPermission)
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Lot?>(null) }

    val nearest = userLocation?.let { u -> lots.minByOrNull { distanceMeters(u, it.position) } }
    val shown = selected ?: nearest ?: lots.first()
    val results = if (query.isBlank()) emptyList()
    else lots.filter { it.name.contains(query, ignoreCase = true) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(lots.first().position, 16f)
    }

    var lastCentered by remember { mutableStateOf<LatLng?>(null) }
    LaunchedEffect(userLocation) {
        val u = userLocation ?: return@LaunchedEffect
        val prev = lastCentered
        if (prev == null || distanceMeters(prev, u) > 500f) {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(u, 16f))
            lastCentered = u
        }
    }
    // Move to a lot when the user picks one from search
    LaunchedEffect(selected) {
        selected?.let {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it.position, 17f))
        }
    }

    val meters = userLocation?.let { distanceMeters(it, shown.position) }
    val detail = meters?.let { "%.1f mi · %d min walk".format(it / 1609.34f, (it / 80).toInt().coerceAtLeast(1)) }
        ?: "Locating you…"

    Column(modifier.fillMaxSize()) {
        Header(onDrivingClick = onDrivingClick)

        Box(Modifier.weight(1f).fillMaxWidth()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = hasPermission),
                uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = hasPermission)
            ) {
                lots.forEach { lot ->
                    MarkerComposable(
                        lot.name,
                        state = rememberMarkerState(position = lot.position),
                        onClick = { onLotClick(lot); true }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                lot.name,
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                modifier = Modifier
                                    .background(Color.White, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = ParkBlue,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }

            Column(Modifier.align(Alignment.TopCenter).padding(16.dp)) {
                SearchBar(query = query, onQueryChange = { query = it })
                results.forEach { lot ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable {
                            selected = lot; query = ""
                        },
                        color = Color.White,
                        contentColor = Color.Black,
                        shadowElevation = 2.dp
                    ) {
                        Text(
                            lot.name,
                            modifier = Modifier.padding(16.dp),
                            color = Color.Black
                        )
                    }
                }
            }
        }

        LotCard(name = shown.name, detail = detail, open = ParkingRepository.openCount(shown), onScan = onScanClick) }
}

@Composable
private fun Header(title: String = "Find Parking", onDrivingClick: () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(title, fontSize = 26.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Button(
            onClick = onDrivingClick,
            colors = ButtonDefaults.buttonColors(containerColor = ParkBlue, contentColor = Color.White)
        ) { Text("Driving mode") }
    }
}
@SuppressLint("MissingPermission")
@Composable
internal fun rememberUserLocation(enabled: Boolean): State<LatLng?> {
    val context = LocalContext.current
    val location = remember { mutableStateOf<LatLng?>(null) }

    DisposableEffect(enabled) @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION]) {
        if (!enabled) return@DisposableEffect onDispose { }
        val client = LocationServices.getFusedLocationProviderClient(context)
        client.lastLocation.addOnSuccessListener { loc ->
            loc?.let { location.value = LatLng(it.latitude, it.longitude) }
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateDistanceMeters(10f)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location.value = LatLng(it.latitude, it.longitude) }
            }
        }
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        onDispose { client.removeLocationUpdates(callback) }
    }
    return location
}
@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 4.dp,
        contentColor = Color.Black,
        color = Color.White
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search campus lots") },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = ParkBlue) },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedPlaceholderColor = Color.Gray,
                unfocusedPlaceholderColor = Color.Gray,
                cursorColor = ParkBlue
            )
        )
    }
}

@Composable
private fun LotCard(name: String, detail: String, open: Int, onScan: () -> Unit) {    Column(Modifier.fillMaxWidth().padding(16.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            Text(detail, color = Color.Gray, fontSize = 14.sp)
        }
        Surface(shape = RoundedCornerShape(50), color = OpenGreenBg) {
            Text(
                "$open open",
                color = OpenGreen,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = onScan,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ParkGold, contentColor = Color.Black)
    ) {
        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Scan to park", fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}
}

@Preview(showBackground = true)
@Composable
private fun MapScreenPreview() {
    MapScreen()
}

data class Lot(val name: String, val position: LatLng, val spots: Int)

val lots = listOf(
    Lot("Admissions Lot", LatLng(41.291747, -72.962459), 25),
    Lot("Library Lot", LatLng(41.290369, -72.960808), 77),
    Lot("Harugari Lot (Commuter Portion)", LatLng(41.288424, -72.959067), 21),
    Lot("Celentano Lots", LatLng(41.289070, -72.963093), 180),
    Lot("German Club Lot", LatLng(41.290322, -72.963679), 29),
    Lot("Gerber Hall Lot", LatLng(41.290798, -72.962958), 37),
    Lot("Beckerman Recreation Center Lot", LatLng(41.291103, -72.963399), 50),
    Lot("Bergami North Lot", LatLng(41.290957, -72.964770), 52),
    Lot("Bergami East Lot", LatLng(41.291010, -72.963858), 30),
    Lot("Bergami West Lot", LatLng(41.290569, -72.965379), 84),
    Lot("Echlin Hall Lot", LatLng(41.293087, -72.961955), 20),
    Lot("Subway Lot", LatLng(41.292368, -72.962494), 22),
    Lot("Wade Street Lot", LatLng(41.292140, -72.963634), 42),
    Lot("North Campus West Lot", LatLng(41.295601, -72.963608), 132),
    Lot("Ruden Street West Lot", LatLng(41.289995, -72.960735), 13),
    Lot("Ruden Street Lot", LatLng(41.290068, -72.960163), 22),
    Lot("Ruden Street Office Lot", LatLng(41.290267, -72.959721), 18),
    Lot("Charger Plaza Lot", LatLng(41.293359, -72.958275), 195),
    Lot("University Student Hall Lot", LatLng(41.287380, -72.956930), 91),
    Lot("Hoffman Street Lot", LatLng(41.291446, -72.966014), 195),
    
    Lot("Elevation Center Lot", LatLng(41.290053, -72.957794), 370),
)
internal fun distanceMeters(a: LatLng, b: LatLng): Float {
    val r = FloatArray(1)
    android.location.Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, r)
    return r[0]
}