package com.example.chatbot.sign_in

import android.annotation.SuppressLint
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.chatbot.AuthRespone
import com.example.chatbot.AuthenticationManager
import com.example.chatbot.ChatViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class AuthViewModel(private val authenticationManager: AuthenticationManager) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        // Add Firebase Auth state listener
        Firebase.auth.addAuthStateListener { auth ->
            val currentUser = auth.currentUser
            Log.d("AuthViewModel", "Auth state changed: ${currentUser?.displayName ?: "null"}")
            _authState.value = if (currentUser != null) {
                AuthState.Authenticated(currentUser)
            } else {
                AuthState.Unauthenticated
            }
        }

        // Initial check
        checkAuthState()
    }

    private fun checkAuthState() {
        val currentUser = Firebase.auth.currentUser
        Log.d("AuthViewModel", "Checking auth state: ${currentUser?.displayName ?: "null"}")
        _authState.value = if (currentUser != null) {
            AuthState.Authenticated(currentUser)
        } else {
            AuthState.Unauthenticated
        }
    }

    fun signInWithGoogle() {
        _authState.value = AuthState.Loading

        authenticationManager.signinwithGoogle()
            .onEach { response ->
                when (response) {
                    is AuthRespone.Sucess -> {
                        val user = Firebase.auth.currentUser
                        _authState.value = if (user != null) {
                            AuthState.Authenticated(user)
                        } else {
                            AuthState.Unauthenticated
                        }
                    }
                    is AuthRespone.Error -> {
                        _authState.value = AuthState.Unauthenticated
                        _errorMessage.value = response.message
                    }
                }
            }
            .catch { exception ->
                _authState.value = AuthState.Unauthenticated
                _errorMessage.value = exception.message
            }
            .launchIn(viewModelScope)
    }

    fun signOut() {
        viewModelScope.launch {
            authenticationManager.signOut().collect { response ->
                when (response) {
                    is AuthRespone.Sucess -> {
                        _authState.value = AuthState.Unauthenticated
                    }
                    is AuthRespone.Error -> {
                        _errorMessage.value = response.message
                    }
                }
            }
        }
    }


    fun clearError() {
        _errorMessage.value = null
    }
}

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
}


