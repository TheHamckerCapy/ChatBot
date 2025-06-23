package com.example.chatbot

import android.app.Application
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID


class ChatViewModel(application: Application) : AndroidViewModel(application) {
    val chats = mutableStateListOf<ChatSession>()
    var currentId by mutableStateOf("")

    private val model = Firebase.ai(backend = GenerativeBackend.googleAI())
        .generativeModel("gemini-2.0-flash-001")

    val currentChat: ChatSession?
        get() = chats.find { it.id == currentId }

    fun sendMessage(question: String) {
        currentChat?.let { chat ->
            viewModelScope.launch {
                try {
                    // Add user message
                    chat.messages.add(MessageModel(MessageContent.Text(question), "user"))

                    // Add temporary typing indicator
                    val typingIndex = chat.messages.size
                    chat.messages.add(MessageModel(MessageContent.Text("typing..."), "model"))

                    // Prepare chat history
                    val history = chat.messages.dropLast(1).map { msg ->
                        content(role = msg.role) {
                            when (msg.content) {
                                is MessageContent.Text -> text(msg.content.text)
                                is MessageContent.Image -> image(BitmapFactory.decodeFile(msg.content.uri))
                                is MessageContent.File -> text("[File: ${msg.content.name}]")
                            }
                        }
                    }

                    // Get response
                    val chatSession = model.startChat(history = history)
                    val response = chatSession.sendMessage(question)

                    // Update with actual response
                    chat.messages[typingIndex] = MessageModel(
                        MessageContent.Text(response.text ?: "No response"),
                        "model"
                    )
                } catch (e: Exception) {
                    // Remove typing indicator and show error
                    chat.messages.removeAt(chat.messages.lastIndex)
                    chat.messages.add(
                        MessageModel(
                            MessageContent.Text("Error: ${e.message ?: "Unknown error"}"),
                            "model"
                        )
                    )
                }
            }
        }
    }

    fun createNewChat() {
        val newChat = ChatSession(
            id = UUID.randomUUID().toString(),
            title = "Chat ${chats.size + 1}",
            messages = mutableStateListOf()
        )
        chats.add(newChat)
        currentId = newChat.id
    }

    fun deleteChat(chatId: String) {
        chats.removeIf { it.id == chatId }
        if (currentId == chatId) {
            currentId = chats.firstOrNull()?.id ?: ""
        }
    }

    fun sendImage(uri: Uri) {
        currentChat?.let { chat ->
            viewModelScope.launch {
                try {
                    // Add image message
                    chat.messages.add(MessageModel(MessageContent.Image(uri.toString()), "user"))

                    // Add typing indicator
                    val typingIndex = chat.messages.size
                    chat.messages.add(
                        MessageModel(
                            MessageContent.Text("analyzing image..."),
                            "model"
                        )
                    )

                    // Analyze image
                    val response = analyzeImage(uri)

                    // Update with analysis
                    chat.messages[typingIndex] = MessageModel(
                        MessageContent.Text(response),
                        "model"
                    )
                } catch (e: Exception) {
                    chat.messages.add(
                        MessageModel(
                            MessageContent.Text("Error analyzing image: ${e.message}"),
                            "model"
                        )
                    )
                }
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
/*
class ChatViewModel(application: Application) : AndroidViewModel(application){

    /*
    val messageList by lazy {
        mutableStateListOf<MessageModel>()
    }
     */


    val chats = mutableStateListOf<ChatSession>()
    var currentId by mutableStateOf("")

    val model = Firebase.ai(backend = GenerativeBackend.googleAI())
        .generativeModel("gemini-2.0-flash-001")
    val currentChat = chats.find { it.id==currentId }
    fun sendMessage(question: String){

        if(currentChat==null)return
        viewModelScope.launch {
            try {
                currentChat.messages.add(MessageModel(question,"user"))
                currentChat.messages.add(MessageModel("typing.....","model"))
                val chat = model.startChat(
                    history = currentChat.messages.dropLast(1).map { msg->
                        content(role = msg.role){
                            text(msg.message)
                        }

                    }
                )

                val response: GenerateContentResponse = chat.sendMessage(question)
                currentChat.messages.removeAt(currentChat.messages.lastIndex)
                currentChat.messages.add(MessageModel(response.text.toString(),"model"))
            }catch (e:Exception){
                currentChat.messages.removeAt(currentChat.messages.lastIndex)
                currentChat.messages.add(MessageModel("Error: ${e.message.toString()}","model"))
            }


        }
    }

    fun createNewChat(){
        val newChat = ChatSession(
            id = UUID.randomUUID().toString(),
            title = "Chat ${chats.size+1}",
            messages = mutableStateListOf()
        )
        chats.add(newChat)
        currentId = newChat.id
    }

    fun deleteChat(chatId: String){
       chats.removeIf { it.id==chatId }
        if(currentId==chatId){
            currentId = chats.firstOrNull()?.id ?: ""
        }
    }
    fun sendImage(uri: Uri){
        viewModelScope.launch {
            currentChat?.messages?.add(MessageModel(MessageContent.Image(uri.toString()).toString(),"user"))
            val response = analyzeImage(uri)
            currentChat?.messages?.add(MessageModel(MessageContent.Text(response.toString()).toString(),"model"))
        }
    }
    private suspend fun analyzeImage(uri: Uri): String{
        val contentResolver = getApplication<Application>().contentResolver
        val bitmap: Bitmap? = withContext(Dispatchers.IO) {
            try {
                // For modern Android (API 29+), loadThumbnail is preferred and safer
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    contentResolver.loadThumbnail(uri, Size(1024, 1024), null)
                } else {
                    // Fallback for older Android versions (requires READ_EXTERNAL_STORAGE permission)
                    // This is a simplified fallback; robust handling might involve querying MediaStore directly.
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    // You might need to scale this bitmap manually if it's too large for older versions
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error loading thumbnail: ${e.message}", e)
                null // Return null if loading fails
            }
        }
        if (bitmap == null) {
            return "Could not load image to analyze."
        }
        val inputContent = content {
            image(bitmap)
            text("Describe this image")
        }
        return try {
            val response = model.generateContent(inputContent)
            response.text ?: "Couldn't analyze image"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
    fun getCurrentChat(): ChatSession? {
        return chats.find { it.id == currentId }
    }
}
*/
/*
sealed class ChatUiState {
    data object Idle : ChatUiState()
    data object Loading : ChatUiState()
    data class Success(val message: String) : ChatUiState()
    data class Error(val message: String) : ChatUiState()
}*/