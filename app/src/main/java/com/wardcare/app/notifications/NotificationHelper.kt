package com.wardcare.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.wardcare.app.R

object NotificationHelper {

    private const val CHANNEL_ID = "wardcare_alerts"

    /** Called once from MainActivity.onCreate. */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID, "WardCare alerts", NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }
    }

    /** Push alerts coming from Firebase Cloud Messaging. */
    fun showPushNotification(context: Context, title: String, body: String, type: String) {
        show(context, title, body)
    }

    /** Local notification shown after a medication is logged. */
    fun sendMedicationLoggedNotification(
        context: Context,
        patientName: String,
        medicationName: String,
        status: String
    ) {
        show(context, "Medication logged", "$patientName: $medicationName ($status)")
    }

    /** Reminder shown from the patient detail screen. */
    fun sendMedicationDueReminder(
        context: Context,
        patientName: String,
        bedNumber: String,
        medSchedule: String
    ) {
        show(context, "Medication due: $bedNumber", "$patientName: $medSchedule")
    }

    private fun show(context: Context, title: String, body: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        createNotificationChannel(context)
        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}