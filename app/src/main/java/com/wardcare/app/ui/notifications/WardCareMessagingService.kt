package com.wardcare.app.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.wardcare.app.data.local.UserManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receives real-time push messages from Firebase Cloud Messaging.
 * Reference: https://firebase.google.com/docs/cloud-messaging/android/receive
 */
class WardCareMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "WardCareMessaging"
    }

    // Called while the app is open (background messages are shown by the system tray)
    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: "WardCare alert"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        Log.i(TAG, "onMessageReceived: title=$title")

        if (UserManager(applicationContext).notificationsEnabled) {
            NotificationHelper.showPushNotification(
                applicationContext, title, body, message.data["type"] ?: "info"
            )
        } else {
            Log.d(TAG, "notifications disabled in settings, ignoring push")
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "onNewToken: FCM token refreshed")
        CoroutineScope(Dispatchers.IO).launch { PushTokenRegistrar.register(token) }
    }
}