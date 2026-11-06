package com.wardcare.app.ui.auth

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wardcare.app.data.local.AppDatabase
import com.wardcare.app.data.model.UserAccount
import com.wardcare.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "AuthViewModel"
    }

    private val userRepository = UserRepository(
        userDao = AppDatabase.getDatabase(application).userDao()
    )

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _resetTokenState = MutableStateFlow<ResetTokenState>(ResetTokenState.Idle)
    val resetTokenState: StateFlow<ResetTokenState> = _resetTokenState

    fun register(name: String, email: String, ward: String, password: String, phoneNumber: String) {
        viewModelScope.launch {
            Log.d(TAG, "register: attempt, ward=$ward")
            _authState.value = AuthState.Loading
            userRepository.registerUser(name, email, ward, password, phoneNumber).fold(
                onSuccess = { user ->
                    Log.i(TAG, "register: success")
                    _authState.value = AuthState.Success("Account created. Please log in.", user)
                },
                onFailure = { error ->
                    Log.w(TAG, "register: failed - ${error.message}")
                    _authState.value = AuthState.Error(error.message ?: "Registration failed")
                }
            )
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            Log.d(TAG, "login: attempt")
            _authState.value = AuthState.Loading
            userRepository.loginUser(email, password).fold(
                onSuccess = { user ->
                    Log.i(TAG, "login: success")
                    _authState.value = AuthState.Success("Logged in successfully", user)
                },
                onFailure = { error ->
                    Log.w(TAG, "login: failed - ${error.message}")
                    _authState.value = AuthState.Error(error.message ?: "Login failed")
                }
            )
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            Log.d(TAG, "loginWithGoogle: exchanging Google ID token with Firebase")
            _authState.value = AuthState.Loading
            userRepository.loginWithGoogle(idToken).fold(
                onSuccess = { user ->
                    Log.i(TAG, "loginWithGoogle: success")
                    _authState.value = AuthState.Success("Signed in with Google", user)
                },
                onFailure = { error ->
                    Log.w(TAG, "loginWithGoogle: failed - ${error.message}")
                    _authState.value = AuthState.Error(error.message ?: "Google sign-in failed")
                }
            )
        }
    }

    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            Log.d(TAG, "sendPasswordResetEmail: requested")
            _resetTokenState.value = ResetTokenState.Loading
            userRepository.sendPasswordResetEmail(email).fold(
                onSuccess = { sentTo -> _resetTokenState.value = ResetTokenState.Sent(sentTo) },
                onFailure = { error ->
                    _resetTokenState.value = ResetTokenState.Error(error.message ?: "Could not send reset email")
                }
            )
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
        _resetTokenState.value = ResetTokenState.Idle
    }

    sealed class AuthState {
        object Idle : AuthState()
        object Loading : AuthState()
        data class Success(val message: String, val user: UserAccount) : AuthState()
        data class Error(val message: String) : AuthState()
    }

    sealed class ResetTokenState {
        object Idle : ResetTokenState()
        object Loading : ResetTokenState()
        data class Sent(val email: String) : ResetTokenState()
        data class Error(val message: String) : ResetTokenState()
    }
}