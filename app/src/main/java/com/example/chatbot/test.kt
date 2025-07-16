package com.example.chatbot

import android.text.Layout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar() {
    TopAppBar(
        title = { Text("Create an image", color = Color.White) },
        navigationIcon = {
            IconButton(onClick = { /* Handle back */ }) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
            }
        },
        actions = {
            IconButton(onClick = { /* Menu */ }) {
                Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.White)
            }
        },
        colors = TopAppBarColors(
            // This 'colors' parameter is incomplete in the snippet
            containerColor = Color(0xFF0F172A), // Assuming this was the intent for background
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            scrolledContainerColor = Color.White,
            actionIconContentColor = Color.White,
        ),

    )
}
@Composable
fun MessageBubble(text: String, isUser: Boolean) {
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleColor = if (isUser) Color(0xFF1E293B) else Color(0xFF334155)

    Row (
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .background(bubbleColor, RoundedCornerShape(12.dp))
                .padding(12.dp)
                .widthIn(max = 280.dp)
        ) {
            Text(text = text, color = Color.White)
        }
    }
}
@Composable
fun ChatInput(onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Message...", color = Color.Gray) },
            modifier = Modifier.weight(1f),
            colors = TextFieldDefaults.colors(
               Color(0xFF1E293B),
                Color.White,
                Color.LightGray
            ),
            shape = RoundedCornerShape(20.dp)
        )
        IconButton(
            onClick = {
                if (text.isNotBlank()) {
                    onSend(text)
                    text = ""
                }
            }
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color(0xFF3B82F6))
        }
    }
}
@Composable
fun ChatBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A), // Top (Dark Blue)
                        Color(0xFF1E293B)  // Bottom (Darker Gray-Blue)
                    )
                )
            )
    ) {
        content()
    }
}

@Composable
fun ChatScreen(
    messages: List<Pair<String, Boolean>>, // Pair<message, isUser>
    onSend: (String) -> Unit
) {
    ChatBackground{
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
        ) {
            TopBar()
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp),
                reverseLayout = true
            ) {
                items(messages.reversed()) { (msg, isUser) ->
                    MessageBubble(text = msg, isUser = isUser)
                }
            }
            ChatInput(onSend = onSend)
        }
    }
}
@Preview(showBackground = true)
@Composable
fun PreviewChatScreen() {
    val sampleMessages = listOf(
        "Create an image of a tiny dragon..." to true,
        "A tiny dragon hatches in a sunlit meadow..." to false
    )
    ChatScreen(messages = sampleMessages, onSend = {})
}



