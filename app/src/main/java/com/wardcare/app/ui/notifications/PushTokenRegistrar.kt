package com.wardcare.app.notifications

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.wardcare.app.data.remote.ApiClient
import com.wardcare.app.data.remote.FirestoreMapper
import com.wardcare.app.data.remote.FirestorePaths
import kotlinx.coroutines.CancellationException

/** Saves this device's FCM token to Firestore (deviceTokens/{uid}) so it can be targeted with push messages. */
object PushTokenRegistrar {
    private const val TAG = "PushTokenRegistrar"

    suspend fun register(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Log.d(TAG, "register: nobody signed in yet, token will be saved after login")
            return
        }
        try {
            val path = FirestorePaths.document(ApiClient.projectId, "deviceTokens", uid)
            val body = FirestoreMapper.deviceTokenDocument(token, System.currentTimeMillis())
            val response = ApiClient.apiService.upsertDocument(path, body)
            Log.i(TAG, "register: token saved, HTTP ${response.code()}")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "register: could not save token (${e.javaClass.simpleName})")
        }
    }
}