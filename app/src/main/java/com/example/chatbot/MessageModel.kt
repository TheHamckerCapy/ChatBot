package com.example.chatbot

data class MessageModel(
    val content: MessageContent,
    val role: String
)

data class ChatSession(
    val id: String,
    val title: String,
    val messages: MutableList<MessageModel>

)

sealed class MessageContent {
    data class Text(val text: String) : MessageContent()
    data class Image(val uri: String) : MessageContent() // Store URI or URL
    data class File(val uri: String, val name: String) : MessageContent()
}
