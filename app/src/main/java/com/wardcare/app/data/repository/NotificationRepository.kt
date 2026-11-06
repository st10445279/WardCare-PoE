package com.wardcare.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.wardcare.app.data.local.NotificationDao
import com.wardcare.app.data.model.AppNotification
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

/**
 * Notification list: Room is the local copy (works offline),
 * Firestore at users/{uid}/notifications is the cloud copy.
 */
class NotificationRepository(
    private val notificationDao: NotificationDao
) {
    private val firestore = FirebaseFirestore.getInstance()

    private fun cloudCollection(): CollectionReference? {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        return firestore.collection("users").document(uid).collection("notifications")
    }

    val allNotifications: Flow<List<AppNotification>> = notificationDao.getAllNotifications()
    val notificationCountFlow: Flow<Int> = notificationDao.getNotificationCountFlow()

    /** Saves locally first, then uploads to Firestore. */
    suspend fun addNotification(notification: AppNotification) {
        notificationDao.insertNotification(notification)
        try {
            cloudCollection()?.document(notification.id)?.set(notification)?.await()
        } catch (e: Exception) {
            // Offline or failed: the local copy is kept
        }
    }

    /** Downloads the Firestore copy into Room. */
    suspend fun refreshNotifications() {
        try {
            val snapshot = cloudCollection()?.get()?.await() ?: return
            notificationDao.insertAll(snapshot.toObjects(AppNotification::class.java))
        } catch (e: Exception) {
            // Keep showing cached notifications if the network fails
        }
    }

    suspend fun clearNotifications() {
        notificationDao.clearAllNotifications()
        try {
            val snapshot = cloudCollection()?.get()?.await()
            snapshot?.documents?.forEach { it.reference.delete() }
        } catch (e: Exception) {
            // Ignore cloud failure
        }
    }

    suspend fun getLatestNotification(): AppNotification? = notificationDao.getLatestNotification()
}