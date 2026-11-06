package com.wardcare.app.data.repository

import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.wardcare.app.data.local.UserDao
import com.wardcare.app.data.model.UserAccount
import com.wardcare.app.data.remote.ApiClient
import com.wardcare.app.data.remote.ApiService
import com.wardcare.app.data.remote.FirestoreMapper
import com.wardcare.app.data.remote.FirestorePaths
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

/**
 * Accounts live in Firebase Authentication (email/password + Google SSO). Firebase stores only a
 * salted hash of the password, and the app never keeps the password. The staff profile is stored in
 * Firestore (users/{uid}) and cached in Room so it is available offline.
 * Reference: https://firebase.google.com/docs/auth/android/start
 */
class UserRepository(
    private val userDao: UserDao,
    private val apiService: ApiService = ApiClient.apiService,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    companion object {
        private const val TAG = "UserRepository"
        private const val USERS = "users"
    }

    suspend fun registerUser(
        name: String,
        email: String,
        ward: String,
        password: String,
        phoneNumber: String
    ): Result<UserAccount> {
        val cleanEmail = email.trim().lowercase()
        return try {
            val authResult = auth.createUserWithEmailAndPassword(cleanEmail, password).await()
            val uid = authResult.user?.uid
                ?: return Result.failure(Exception("Account could not be created. Please try again."))

            val user = UserAccount(
                email = cleanEmail,
                name = name.trim(),
                ward = ward.trim().ifBlank { "General Ward" },
                passwordHash = "", // never stored
                role = "Registered Nurse",
                staffId = "NURSE-${Random.nextInt(1000, 9999)}",
                phoneNumber = phoneNumber.trim()
            )
            userDao.insertUser(user)
            pushProfile(uid, user)
            Log.i(TAG, "registerUser: account created uid=$uid")
            auth.signOut() // the user logs in on the next screen
            Result.success(user)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "registerUser failed: ${e.javaClass.simpleName}")
            Result.failure(Exception(friendlyMessage(e)))
        }
    }

    suspend fun loginUser(email: String, password: String): Result<UserAccount> {
        val cleanEmail = email.trim().lowercase()
        return try {
            val authResult = auth.signInWithEmailAndPassword(cleanEmail, password).await()
            val fbUser = authResult.user
                ?: return Result.failure(Exception("Sign-in failed. Please try again."))
            Log.i(TAG, "loginUser: signed in uid=${fbUser.uid}")
            Result.success(loadProfile(fbUser.uid, fbUser.email ?: cleanEmail, fbUser.displayName))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "loginUser failed: ${e.javaClass.simpleName}")
            Result.failure(Exception(friendlyMessage(e)))
        }
    }

    /** Single sign-on: exchanges the Google ID token for a Firebase session. */
    suspend fun loginWithGoogle(idToken: String): Result<UserAccount> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val fbUser = authResult.user
                ?: return Result.failure(Exception("Google sign-in failed. Please try again."))
            Log.i(TAG, "loginWithGoogle: signed in uid=${fbUser.uid}")
            Result.success(loadProfile(fbUser.uid, fbUser.email.orEmpty(), fbUser.displayName))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "loginWithGoogle failed: ${e.javaClass.simpleName}")
            Result.failure(Exception(friendlyMessage(e)))
        }
    }

    /** Firebase emails the user a secure password-reset link. */
    suspend fun sendPasswordResetEmail(email: String): Result<String> {
        val cleanEmail = email.trim().lowercase()
        return try {
            auth.sendPasswordResetEmail(cleanEmail).await()
            Log.i(TAG, "sendPasswordResetEmail: reset email requested")
            Result.success(cleanEmail)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "sendPasswordResetEmail failed: ${e.javaClass.simpleName}")
            Result.failure(Exception(friendlyMessage(e)))
        }
    }

    suspend fun updateProfile(
        name: String,
        email: String,
        ward: String,
        role: String,
        staffId: String,
        phone: String
    ): Result<Boolean> {
        val cleanEmail = email.trim().lowercase()
        val existing = userDao.getUserByEmail(cleanEmail)
        val updated = existing?.copy(
            name = name.trim(),
            ward = ward.trim(),
            role = role.trim(),
            staffId = staffId.trim(),
            phoneNumber = phone.trim()
        ) ?: UserAccount(
            email = cleanEmail,
            name = name.trim(),
            ward = ward.trim(),
            passwordHash = "",
            role = role.trim(),
            staffId = staffId.trim(),
            phoneNumber = phone.trim()
        )
        userDao.insertUser(updated)
        auth.currentUser?.let { pushProfile(it.uid, updated) }
        return Result.success(true)
    }

    /** Profile from Firestore, else the cached Room copy, else a new profile (first Google login). */
    private suspend fun loadProfile(uid: String, email: String, displayName: String?): UserAccount {
        val cleanEmail = email.trim().lowercase()
        try {
            val path = FirestorePaths.document(ApiClient.projectId, USERS, uid)
            val response = apiService.getDocument(path)
            if (response.isSuccessful) {
                val fields = response.body()?.getAsJsonObject("fields")
                val remote = fields?.let { FirestoreMapper.userFromFields(it) }
                if (remote != null) {
                    userDao.insertUser(remote)
                    Log.d(TAG, "loadProfile: profile loaded from Firestore")
                    return remote
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "loadProfile: Firestore unavailable (${e.javaClass.simpleName})")
        }

        userDao.getUserByEmail(cleanEmail)?.let { return it }

        val created = UserAccount(
            email = cleanEmail,
            name = displayName.takeUnless { it.isNullOrBlank() } ?: cleanEmail.substringBefore("@"),
            ward = "General Ward",
            passwordHash = "",
            role = "Registered Nurse",
            staffId = "NURSE-${Random.nextInt(1000, 9999)}",
            phoneNumber = ""
        )
        userDao.insertUser(created)
        pushProfile(uid, created)
        Log.d(TAG, "loadProfile: created a new profile")
        return created
    }

    private suspend fun pushProfile(uid: String, user: UserAccount) {
        try {
            val path = FirestorePaths.document(ApiClient.projectId, USERS, uid)
            val response = apiService.upsertDocument(path, FirestoreMapper.userToDocument(user))
            if (response.isSuccessful) {
                Log.i(TAG, "pushProfile: profile saved to Firestore for uid=$uid")
            } else {
                Log.w(TAG, "pushProfile: HTTP ${response.code()}")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "pushProfile: offline, profile kept locally (${e.javaClass.simpleName})")
        }
    }

    // FirebaseAuthWeakPasswordException is a subclass of InvalidCredentials, so it must come first
    private fun friendlyMessage(e: Exception): String = when (e) {
        is FirebaseAuthUserCollisionException -> "An account with this email already exists."
        is FirebaseAuthWeakPasswordException -> "Password is too weak. Use 8+ characters with a letter and a number."
        is FirebaseAuthInvalidUserException -> "No account found with that email. Please register first."
        is FirebaseAuthInvalidCredentialsException -> "Email or password is incorrect."
        is FirebaseNetworkException -> "No internet connection. Please connect and try again."
        else -> e.localizedMessage ?: "Something went wrong. Please try again."
    }
}