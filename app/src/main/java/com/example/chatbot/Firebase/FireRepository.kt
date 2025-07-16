package com.example.chatbot.Firebase

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FireRepository{
    private val database = Firebase.database("https://chat-bot-47952-default-rtdb.asia-southeast1.firebasedatabase.app/")
    private val  auth = Firebase.auth
    private fun currentUserId(): String {
        return auth.currentUser?.uid ?: throw Exception("User not authenticated")
    }

    fun ObservewChatSession(): Flow<List<FireChatSession>> = callbackFlow{
        val userId = currentUserId()
        val chatsRef = database.getReference("users/$userId/chats")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val chats = snapshot.children.mapNotNull {
                    it.getValue(FireChatSession::class.java)?.let { firebaseChat ->
                        FireChatSession(
                            id = firebaseChat.id,
                            title = firebaseChat.title,
                            messages = hashMapOf()
                        )
                    }
                }
                trySend(chats)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }

        }

        chatsRef.addValueEventListener(listener)
        awaitClose { chatsRef.removeEventListener(listener) }
    }

    fun ObservewMessages(chatId:String): Flow<List<FireMessageModel>> = callbackFlow{
        val userId = currentUserId()
        val messageRef = database.getReference("users/$userId/chats/$chatId/messages")
            .orderByChild("timeStamp")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull{ messageSnapshot ->
                try {
                    messageSnapshot.getValue(FireMessageModel::class.java)
                } catch (e: Exception) {
                    Log.e("FireRepository", "Error parsing message: ${e.message}")
                    null
                }
            }.sortedBy { it.timeStamp }
                trySend(messages)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }

        }
        messageRef.addValueEventListener(listener)
        awaitClose { messageRef.removeEventListener(listener) }
    }

    suspend fun createChatSession(title: String): String {
        val userId = currentUserId()
        val chatId = UUID.randomUUID().toString()
        val chatsRef = database.getReference("users/$userId/chats/$chatId")

        val chat = FireChatSession(
            id = chatId,
            title = title,
            messages = hashMapOf()
        )
        chatsRef.setValue(chat)
        return chatId
    }
    suspend fun addMessage(chatId: String, message: FireMessageModel) {
        val userId = currentUserId()
        val messagesRef = database.getReference("users/$userId/chats/$chatId/messages").push()
        messagesRef.setValue(message).await()
    }
    suspend fun updateChatTitle(chatId: String, newTitle: String) {
        val userId = currentUserId()
        database.getReference("users/$userId/chats/$chatId/title")
            .setValue(newTitle).await()

    }

    // Delete a chat session
    suspend fun deleteChatSession(chatId: String) {
        val userId = currentUserId()
        database.getReference("users/$userId/chats/$chatId").removeValue().await()
    }
}