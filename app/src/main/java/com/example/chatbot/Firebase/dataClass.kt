package com.example.chatbot.Firebase

import com.google.firebase.database.IgnoreExtraProperties


@IgnoreExtraProperties
data class FireMessageModel(
    val content: FireMessageContent? = FireMessageContent(),
    val role: String? = null,
    val timeStamp: Long = System.currentTimeMillis()
)
@IgnoreExtraProperties
data class FireChatSession(
    val id: String= "",
    val title: String= "",
    val messages: HashMap<String, FireMessageModel> = hashMapOf()

)
enum class MessageContentType {
    TEXT, IMAGE, FILE
}
@IgnoreExtraProperties
open class FireMessageContent(
    open val type: MessageContentType = MessageContentType.TEXT
) {
    @IgnoreExtraProperties
    data class Text(
        val text: String = ""
    ) : FireMessageContent(MessageContentType.TEXT)

    @IgnoreExtraProperties
    data class Image(
        val uri: String = ""
    ) : FireMessageContent(MessageContentType.IMAGE)

    @IgnoreExtraProperties
    data class File(
        val uri: String = "",
        val name: String = ""
    ) : FireMessageContent(MessageContentType.FILE)
}