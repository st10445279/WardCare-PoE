package com.wardcare.app.data.remote

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val TAG = "ApiClient"
    private const val BASE_URL = "https://firestore.googleapis.com/"

    /** Firebase project id, read from google-services.json at runtime. */
    val projectId: String
        get() = FirebaseApp.getInstance().options.projectId.orEmpty()

    /** Adds "Authorization: Bearer <Firebase ID token>" so Firestore rules see who is calling. */
    private class FirebaseAuthInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val user = FirebaseAuth.getInstance().currentUser
            val token = if (user != null) {
                try {
                    // Runs on an OkHttp background thread, so blocking here is safe.
                    Tasks.await(user.getIdToken(false), 10, TimeUnit.SECONDS).token
                } catch (e: Exception) {
                    Log.w(TAG, "Could not get Firebase ID token (offline?): ${e.javaClass.simpleName}")
                    null
                }
            } else null

            val request = if (token != null) {
                chain.request().newBuilder().header("Authorization", "Bearer $token").build()
            } else {
                chain.request()
            }
            return chain.proceed(request)
        }
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(FirebaseAuthInterceptor())
            // BASIC logs method, URL and status only (no headers/bodies), so no tokens or patient data
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()
    }

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}