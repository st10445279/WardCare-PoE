package com.wardcare.app.data.local

import android.content.Context
import android.content.SharedPreferences

/** Small session/settings store (SharedPreferences). Never stores passwords. */
class UserManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("wardcare_user_prefs", Context.MODE_PRIVATE)

    var userName: String
        get() = prefs.getString("user_name", "Staff Member") ?: "Staff Member"
        set(value) = prefs.edit().putString("user_name", value).apply()

    var staffName: String
        get() = userName
        set(value) { userName = value }

    var userEmail: String
        get() = prefs.getString("user_email", "") ?: ""
        set(value) = prefs.edit().putString("user_email", value).apply()

    var userWard: String
        get() = prefs.getString("user_ward", "General Ward") ?: "General Ward"
        set(value) = prefs.edit().putString("user_ward", value).apply()

    var userRole: String
        get() = prefs.getString("user_role", "Registered Nurse") ?: "Registered Nurse"
        set(value) = prefs.edit().putString("user_role", value).apply()

    var staffId: String
        get() = prefs.getString("staff_id", "") ?: ""
        set(value) = prefs.edit().putString("staff_id", value).apply()

    var userPhone: String
        get() = prefs.getString("user_phone", "") ?: ""
        set(value) = prefs.edit().putString("user_phone", value).apply()

    var selectedLanguage: String
        get() = prefs.getString("selected_language", "EN") ?: "EN"
        set(value) = prefs.edit().putString("selected_language", value).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean("notifications_enabled", true)
        set(value) = prefs.edit().putBoolean("notifications_enabled", value).apply()

    var isLoggedIn: Boolean
        get() = prefs.getBoolean("is_logged_in", false)
        set(value) = prefs.edit().putBoolean("is_logged_in", value).apply()

    fun logout() {
        prefs.edit().putBoolean("is_logged_in", false).apply()
    }
}