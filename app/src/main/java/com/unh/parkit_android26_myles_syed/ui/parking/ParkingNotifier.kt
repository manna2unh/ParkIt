package com.unh.parkit_android26_myles_syed.ui.parking

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.unh.parkit_android26_myles_syed.MainActivity
import com.unh.parkit_android26_myles_syed.R
import com.unh.parkit_android26_myles_syed.ui.screens.Lot

/**
 * Posts a local notification whenever another driver's parking session ends
 * (their spot is freed), no matter how far away the lot is.
 */
object ParkingNotifier {
    private const val CHANNEL_ID = "spot_freed"

    private var appContext: Context? = null

    /** Call once at app start (MainActivity.onCreate). Creates the notification channel. */
    fun install(context: Context) {
        val app = context.applicationContext
        appContext = app
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Open parking spots",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Tells you when a parking spot opens up" }
        app.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Called by ParkingRepository when someone else's session in [lot] has ended. */
    @SuppressLint("MissingPermission")
    fun onSpotFreed(lot: Lot, spot: Int) {
        val ctx = appContext ?: return
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return
        val nm = NotificationManagerCompat.from(ctx)
        if (!nm.areNotificationsEnabled()) return

        val open = PendingIntent.getActivity(
            ctx, 0,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Parking spot open")
            .setContentText("A spot in \"${lot.name}\" with the parking spot #$spot")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        // One notification per spot, so several freed spots each show up
        nm.notify("${lot.name}_$spot".hashCode(), n)
    }
}
