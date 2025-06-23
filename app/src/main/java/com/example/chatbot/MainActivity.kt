package com.example.chatbot

import android.accounts.AccountManager
import android.content.ContentValues.TAG
import android.content.Context
import android.os.Build
import android.os.Bundle

import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getString
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.ClearCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chatbot.sign_in.AuthState
import com.example.chatbot.sign_in.AuthViewModel

import com.example.chatbot.ui.theme.ChatBotTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

import java.security.MessageDigest
import java.util.UUID



// Updated MainActivity using AuthViewModel
class MainActivity : ComponentActivity() {
    private val chatViewModel: ChatViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(AuthenticationManager(this@MainActivity)) as T
            }
        }
    }

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { chatViewModel.sendImage(it) }
    }
    private val legacyPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { chatViewModel.sendImage(it) }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ChatBotTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val authState by authViewModel.authState.collectAsState()
                    val errorMessage by authViewModel.errorMessage.collectAsState()

                    // Show error message if exists
                    errorMessage?.let { message ->
                        LaunchedEffect(message) {
                            Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                            authViewModel.clearError()
                        }
                    }

                    when (authState) {
                        is AuthState.Loading -> {
                            LoadingScreen()
                        }
                        is AuthState.Unauthenticated -> {
                            LoginScreen(
                                onSignInClick = { authViewModel.signInWithGoogle() }
                            )
                        }
                        is AuthState.Authenticated -> {
                            ChatApp(
                                viewModel= chatViewModel,
                                onPickImage = ::launchImagePicker,
                                authViewModel = authViewModel

                            )
                        }
                    }
                }
            }
        }
    }

    private fun launchImagePicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } else {
            legacyPickerLauncher.launch("image/*")
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
@Composable
fun LoginScreen(
    onSignInClick: () -> Unit
) {
    val context = LocalContext.current
    val authenticationManager = remember{
        AuthenticationManager(context)
    }
    val coroutineScope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to ChatBot",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(
            onClick = onSignInClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Sign in with Google")
        }
    }
}

class AuthenticationManager(private val context: Context){
    private val serverClientId = context.getString(R.string.default_client_id)
    private val auth = Firebase.auth

    fun createNonce(): String{
        val rawNonce = UUID.randomUUID().toString()
        val bytes = rawNonce.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)

        return digest.fold(""){ str,it->
            str + "%02x".format(it)

        }
    }
    fun signinwithGoogle(): Flow<AuthRespone> = callbackFlow{
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(false)
            .setNonce(createNonce())
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            val credentialManager = CredentialManager.create(context)
            val accountManager = AccountManager.get(context)
            val accounts = accountManager.getAccountsByType("com.google")
            Log.d("DEBUG", "Google accounts on device: ${accounts.size}")

            val result = credentialManager.getCredential(context=context, request = request)
            val credential = result.credential
            if(credential is CustomCredential){
                if(credential.type==GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL){
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        val firebaseCredential= GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken,null)
                        auth.signInWithCredential(firebaseCredential)
                            .addOnCompleteListener{
                                if(it.isSuccessful){
                                    trySend(AuthRespone.Sucess)
                                }else{
                                    trySend(AuthRespone.Error(message = it.exception?.message?:" "))
                                }
                            }
                    }catch (e:GoogleIdTokenParsingException){
                        trySend(AuthRespone.Error(message = e.message?:" "))

                    }
                }
            }
        }catch (e:Exception){
            trySend(AuthRespone.Error(message = e.message?:" "))
        }
    }
    suspend fun signOut(): Flow<AuthRespone> = callbackFlow {

            auth.signOut()
            try{
                val credentialManager = CredentialManager.create(context)
                val clearRequest = ClearCredentialStateRequest()
                credentialManager.clearCredentialState(clearRequest)

                trySend(AuthRespone.Sucess)
            }catch (e: ClearCredentialException){
                Log.e(TAG, "Couldn't clear user credentials: ${e.localizedMessage}")


            }

    }
}

interface AuthRespone{
    data object Sucess: AuthRespone
    data class Error(val message: String): AuthRespone

}

/*
https://dribbble.com/shots/26150299-AI-Chatbot-Mobile-App ---> one option
https://dribbble.com/shots/26067877-Aclik-AI-Chatbot-Mobile-App  ---->second option
 */

