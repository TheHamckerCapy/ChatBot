package com.example.chatbot.Firebase

import android.app.Application
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.time.withTimeoutOrNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

class NChatViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FireRepository()
    private val model = Firebase.ai(backend = GenerativeBackend.googleAI())
        .generativeModel("gemini-2.0-flash-001")

    val chats = repository.ObservewChatSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    private val _currentId = MutableStateFlow("")
    val currentId: StateFlow<String> = _currentId.asStateFlow()
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentChatMessages: Flow<List<FireMessageModel>> =_currentId
            .flatMapLatest { id->
                if (id.isNotEmpty()) {
                    repository.ObservewMessages(id)
                } else {
                    flowOf(emptyList())
                }
            }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val currentIdValue: String get() = _currentId.value
    fun setCurrentChat(chatId: String) {
        Log.d("ChatViewModel", "Setting current chat to: $chatId")
        _currentId.value = chatId
    }
    fun createNewChat() {
        viewModelScope.launch {
            try {
                val newChatId = repository.createChatSession("Chat ${chats.value.size + 1}")
                Log.d("ChatViewModel", "Created new chat: $newChatId")
                _currentId.value = newChatId
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error creating chat", e)
            }
        }
    }

    fun sendMessage(question: String) {
        if (_currentId.value.isEmpty()|| question.isBlank()) {
            Log.w("ChatViewModel", "Cannot send message: currentId is empty or question is blank")
            return
        }
        viewModelScope.launch {
            try {
                val userMessage = FireMessageModel(FireMessageContent.Text(question), "user")
                repository.addMessage(_currentId.value, userMessage)


                val currentMessage = try {
                    // Use firstOrNull with timeout to avoid indefinite suspension
                    withTimeoutOrNull(5000) {
                        currentChatMessages.firstOrNull() ?: emptyList()
                    } ?: emptyList()
                } catch (e: Exception) {
                    Log.w("ChatViewModel", "Error getting current messages, using empty list", e)
                    emptyList()
                }
                val history = currentMessage.dropLast(1).mapNotNull { msg ->
                    try {
                        content(role = msg.role ?: "user") {
                            when (val content = msg.content) {
                                is FireMessageContent.Text -> {
                                    if (content.text.isNotEmpty()) {
                                        text(content.text)
                                    }
                                }
                                is FireMessageContent.Image -> {
                                    if (content.uri.isNotEmpty()) {
                                        // Handle image loading more safely
                                        try {
                                            val bitmap = BitmapFactory.decodeFile(content.uri)
                                            if (bitmap != null) {
                                                image(bitmap)
                                            } else {
                                                text("[Image could not be loaded]")
                                            }
                                        } catch (e: Exception) {
                                            text("[Image: ${content.uri}]")
                                        }
                                    }
                                }
                                is FireMessageContent.File -> {
                                    text("[File: ${content.name}]")
                                }
                                null -> text("[Empty message]")
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("ChatViewModel", "Error processing message for history", e)
                        null
                    }
                }
                val chatSession = model.startChat(history = history)
                val response = chatSession.sendMessage(question)
                val aiMessage = FireMessageModel(
                    FireMessageContent.Text(response.text ?: "No response"),
                    "model"
                )
                repository.addMessage(_currentId.value, aiMessage)
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error sending message", e)
                if (_currentId.value.isNotEmpty()) {
                    try {
                        repository.addMessage(
                            _currentId.value,
                            FireMessageModel(
                                FireMessageContent.Text("Error: ${e.message ?: "Unknown error occurred"}"),
                                "model"
                            )
                        )
                    } catch (addError: Exception) {
                        Log.e("ChatViewModel", "Error adding error message", addError)
                    }
                }
            }
        }
    }

    fun sendImage(uri: Uri) {
        if (_currentId.value.isEmpty()) return

        viewModelScope.launch {
            try {
                // First upload image to Firebase Storage if needed
                val storageRef = Firebase.storage.reference
                val imageRef = storageRef.child("chat_images/${UUID.randomUUID()}")
                val uploadTask = imageRef.putFile(uri).await()
                val downloadUrl = uploadTask.storage.downloadUrl.await().toString()

                // Add image message to Firebase
                val imageMessage = FireMessageModel(FireMessageContent.Image(downloadUrl), "user")
                repository.addMessage(_currentId.value, imageMessage)

                // Analyze image and get response
                val response = analyzeImage(uri)

                // Add AI response to Firebase
                val aiMessage = FireMessageModel(
                    FireMessageContent.Text(response ?: "No response"),
                    "model"
                )
                repository.addMessage(_currentId.value, aiMessage)
            } catch  (e: Exception) {
            Log.e("ChatViewModel", "Error sending image", e)
            if (_currentId.value.isNotEmpty()) {
                try {
                    repository.addMessage(
                        _currentId.value,
                        FireMessageModel(
                            FireMessageContent.Text("Error sending image: ${e.message ?: "Unknown error"}"),
                            "model"
                        )
                    )
                } catch (addError: Exception) {
                    Log.e("ChatViewModel", "Error adding error message", addError)
                }
            }
        }
        }
    }
    fun deleteChat(chatId:String){
        viewModelScope.launch {

                repository.deleteChatSession(chatId)
                if (_currentId.value == chatId) {
                    _currentId.value = chats.value.firstOrNull()?.id ?: ""
                }

        }
    }

    private suspend fun analyzeImage(uri: Uri): String {
        return try {
            val bitmap = withContext(Dispatchers.IO) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    getApplication<Application>().contentResolver.loadThumbnail(
                        uri,
                        Size(1024, 1024),
                        null
                    )
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(
                        getApplication<Application>().contentResolver,
                        uri
                    )
                }
            } ?: return "Couldn't load image"

            val inputContent = content {
                image(bitmap)
                text("Describe this image")
            }

            val response = model.generateContent(inputContent)
            response.text ?: "No description available"
        } catch (e: Exception) {
            "Error analyzing image: ${e.message}"
        }
    }

}