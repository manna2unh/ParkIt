package com.unh.parkit_android26_myles_syed.ui.navigation

import android.net.Uri
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.unh.parkit_android26_myles_syed.data.AccountRepository
import com.unh.parkit_android26_myles_syed.ui.parking.ParkingRepository
import com.unh.parkit_android26_myles_syed.ui.screens.DrivingModeScreen
import com.unh.parkit_android26_myles_syed.ui.screens.LoginScreen
import com.unh.parkit_android26_myles_syed.ui.screens.LotDetailScreen
import com.unh.parkit_android26_myles_syed.ui.screens.MapScreen
import com.unh.parkit_android26_myles_syed.ui.screens.ProfileScreen
import com.unh.parkit_android26_myles_syed.ui.screens.ScanScreen
import com.unh.parkit_android26_myles_syed.ui.screens.SessionScreen
import com.unh.parkit_android26_myles_syed.ui.screens.SpotPickerScreen
import com.unh.parkit_android26_myles_syed.ui.screens.WelcomeScreen
import com.unh.parkit_android26_myles_syed.ui.screens.lots
import com.unh.parkit_android26_myles_syed.ui.widgets.Destination

//MAKE NOTIFICATIONS LIKE WHEN PRESS DIRECTIONS IT GIVES YOU A
//NOTICACTION THAT YOUVE ARRIVED AND CLICK IT

const val WELCOME_ROUTE = "welcome"
const val LOGIN_ROUTE = "login"
const val SCAN_ROUTE = "scan"
const val LOT_DETAIL_ROUTE = "lot"
const val SPOTS_ROUTE = "spots"
const val DRIVING_ROUTE = "driving"

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onDrivingDismissed: () -> Unit = {}
) {
    val goToMap = {
        navController.navigate(Destination.MAP.route) {
            popUpTo(Destination.MAP.route)
            launchSingleTop = true
        }
        Unit
    }

    NavHost(
        navController = navController,
        // Returning users who are still signed in skip straight to the map.
        startDestination = if (AccountRepository.isSignedIn) Destination.MAP.route else WELCOME_ROUTE,
        modifier = modifier
    ) {
        composable(WELCOME_ROUTE) {
            WelcomeScreen(onContinue = {
                navController.navigate(LOGIN_ROUTE) { popUpTo(WELCOME_ROUTE) { inclusive = true } }
            })
        }

        composable(LOGIN_ROUTE) {
            LoginScreen(onAuthenticated = {
                // Now that there's a real account, start the live Firestore parking sync.
                ParkingRepository.init()
                navController.navigate(Destination.MAP.route) { popUpTo(LOGIN_ROUTE) { inclusive = true } }
            })
        }

        composable(Destination.MAP.route) {
            MapScreen(
                onScanClick = {
                    // Already parked? Go to the session instead of scanning again.
                    val target = if (ParkingRepository.session != null) Destination.SESSION.route else SCAN_ROUTE
                    navController.navigate(target)
                },
                onLotClick = { lot -> navController.navigate("$LOT_DETAIL_ROUTE/${Uri.encode(lot.name)}") },
                onDrivingClick = { navController.navigate(DRIVING_ROUTE) { launchSingleTop = true } }
            )
        }

        composable(Destination.SESSION.route) {
            SessionScreen(onFindParking = goToMap, onSessionEnded = goToMap)
        }
        composable(Destination.ALERTS.route) { Text("Alerts") }
        composable(Destination.PROFILE.route) {
            ProfileScreen(onSignOut = {
                ParkingRepository.reset()
                AccountRepository.signOut()
                navController.navigate(LOGIN_ROUTE) { popUpTo(0) { inclusive = true } }
            })
        }

        composable(SCAN_ROUTE) {
            ScanScreen(
                onClose = { navController.popBackStack() },
                onCodeScanned = { code ->
                    val lot = ParkingRepository.lotForToken(code)
                    if (lot == null) {
                        false // ScanScreen shows an "unknown code" error
                    } else {
                        navController.navigate("$SPOTS_ROUTE/${Uri.encode(lot.name)}") {
                            popUpTo(SCAN_ROUTE) { inclusive = true }
                        }
                        true
                    }
                }
            )
        }

        composable(
            route = "$SPOTS_ROUTE/{name}",
            arguments = listOf(navArgument("name") { type = NavType.StringType })
        ) { entry ->
            val lot = lots.firstOrNull { it.name == entry.arguments?.getString("name") }
            if (lot != null) {
                SpotPickerScreen(
                    lot = lot,
                    onBack = { navController.popBackStack() },
                    onSpotChosen = { spot ->
                        if (ParkingRepository.startSession(lot, spot)) {
                            navController.navigate(Destination.SESSION.route) {
                                popUpTo(Destination.MAP.route)
                            }
                        }
                    }
                )
            }
        }

        composable(
            route = "$LOT_DETAIL_ROUTE/{name}",
            arguments = listOf(navArgument("name") { type = NavType.StringType })
        ) { entry ->
            val lot = lots.firstOrNull { it.name == entry.arguments?.getString("name") }
            if (lot != null) {
                LotDetailScreen(lot = lot, onBack = { navController.popBackStack() })
            }
        }

        composable(DRIVING_ROUTE) {
            DrivingModeScreen(
                onExit = {
                    onDrivingDismissed()
                    navController.popBackStack()
                }
            )
        }
    }
}