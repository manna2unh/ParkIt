package com.unh.parkit_android26_myles_syed

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.unh.parkit_android26_myles_syed.ui.navigation.AppNavHost
import com.unh.parkit_android26_myles_syed.ui.navigation.DRIVING_ROUTE
import com.unh.parkit_android26_myles_syed.ui.navigation.LOGIN_ROUTE
import com.unh.parkit_android26_myles_syed.ui.navigation.SCAN_ROUTE
import com.unh.parkit_android26_myles_syed.ui.navigation.WELCOME_ROUTE
import com.unh.parkit_android26_myles_syed.ui.parking.ParkingNotifier
import com.unh.parkit_android26_myles_syed.ui.parking.ParkingRepository
import com.unh.parkit_android26_myles_syed.ui.screens.rememberDrivingState
import com.unh.parkit_android26_myles_syed.ui.theme.ParkItAndroid26MylesSyedTheme
import com.unh.parkit_android26_myles_syed.ui.widgets.BottomBar

private const val REAPPEAR_DELAY_MS = 15_000L

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ParkingNotifier.install(this)
        ParkingRepository.init()
        enableEdgeToEdge()
        setContent {
            ParkItAndroid26MylesSyedTheme {
                val context = LocalContext.current
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val route = backStackEntry?.destination?.route
                val hideBottomBar = route == SCAN_ROUTE || route == WELCOME_ROUTE ||
                        route == LOGIN_ROUTE || route == DRIVING_ROUTE

                val fine = Manifest.permission.ACCESS_FINE_LOCATION
                val coarse = Manifest.permission.ACCESS_COARSE_LOCATION
                var hasFineLocation by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(context, fine) ==
                                PackageManager.PERMISSION_GRANTED
                    )
                }
                val locationLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { result -> hasFineLocation = result[fine] == true }

                val notifLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { }

                val inApp = route != null && route != WELCOME_ROUTE && route != LOGIN_ROUTE

                LaunchedEffect(inApp) {
                    if (inApp && !hasFineLocation) locationLauncher.launch(arrayOf(fine, coarse))
                }

                LaunchedEffect(inApp) {
                    val notif = Manifest.permission.POST_NOTIFICATIONS
                    if (inApp &&
                        ContextCompat.checkSelfPermission(context, notif) != PackageManager.PERMISSION_GRANTED
                    ) notifLauncher.launch(notif)
                }

                var drivingDismissed by remember { mutableStateOf(false) }
                val driving = rememberDrivingState(enabled = inApp && hasFineLocation)
                val isDriving = driving.isDriving.value

                LaunchedEffect(isDriving, drivingDismissed, route) {
                    if (!isDriving) {
                        drivingDismissed = false
                        return@LaunchedEffect
                    }
                    if (!inApp || route == DRIVING_ROUTE || route == SCAN_ROUTE) {
                        return@LaunchedEffect
                    }
                    if (!drivingDismissed) {
                        navController.navigate(DRIVING_ROUTE) { launchSingleTop = true }
                        return@LaunchedEffect
                    }
                    while (true) {
                        delay(REAPPEAR_DELAY_MS)
                        if (driving.movingNow()) {
                            drivingDismissed = false
                            break
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = { if (!hideBottomBar) BottomBar(navController) }
                ) { innerPadding ->
                    AppNavHost(
                        navController,
                        Modifier.padding(innerPadding),
                        onDrivingDismissed = { drivingDismissed = true }
                    )
                }
            }
        }
    }
}