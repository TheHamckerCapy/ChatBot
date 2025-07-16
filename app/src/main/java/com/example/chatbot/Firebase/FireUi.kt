package com.example.chatbot.Firebase



import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Size
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.example.chatbot.sign_in.AuthViewModel
import com.example.chatbot.ui.theme.ColorModelMessage
import com.example.chatbot.ui.theme.ColorUserMessage
import com.halilibo.richtext.markdown.Markdown
import com.halilibo.richtext.ui.material3.RichText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatPage(modifier: Modifier = Modifier, viewModel: NChatViewModel, onOpenDrawer: () -> Unit, onPickImage: () -> Unit) {
    val currentChatMessages by viewModel.currentChatMessages.collectAsState(initial = emptyList())
    val chats by viewModel.chats.collectAsState()
    val currentId by viewModel.currentId.collectAsState()
    Column(
        modifier = modifier
    ) {
        TopAppBar(
            title = {
                Text(text = viewModel.chats.value.find { it.id == currentId }?.title ?: "New Chat")
            },
            navigationIcon = {
                IconButton(onClick = onOpenDrawer) {
                    Icon(imageVector = Icons.Default.Menu, contentDescription = null)
                }
            }
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (currentId.isNotEmpty()) {
                MessageList(
                    modifier = Modifier.fillMaxSize(),
                    messageList = currentChatMessages
                )
            } else {
                // Show empty state when no chat is selected
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Start a conversation",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Type a message below to begin",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
        MessageInput(
            onMessageSend = { message ->
                if (currentId.isEmpty()) {
                    viewModel.createNewChat()
                }

                viewModel.sendMessage(message)

            },
            enabled = true,
            onAttach = {attachmentType->
                if(attachmentType=="image"){
                    onPickImage()
                }else if(attachmentType=="file"){
                    onPickImage()
                }

            }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun MessageList(modifier: Modifier = Modifier, messageList: List<FireMessageModel>) {
    LazyColumn(
        modifier = modifier,
        reverseLayout = true
    ) {
        items(messageList.reversed()) {
            MessageCompose(messageModel = it)
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun MessageCompose(messageModel: FireMessageModel) {
    val isModel = messageModel.role == "model"

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .align(
                        if (isModel) Alignment.BottomStart
                        else Alignment.BottomEnd
                    )
                    .padding(
                        start = if (isModel) 8.dp else 70.dp,
                        end = if (isModel) 70.dp else 8.dp,
                        top = 8.dp,
                        bottom = 8.dp
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isModel) ColorModelMessage else ColorUserMessage)
                    .padding(16.dp)
            ) {
                SelectionContainer {
                    when (messageModel.content) {
                        is FireMessageContent.Text -> {
                            if (isModel) {
                                RichText {
                                    messageModel.content.text.let { Markdown(content = it) }
                                }
                            } else {
                                messageModel.content.text.let {
                                    Text(
                                        text = it,
                                        fontWeight = FontWeight.W500
                                    )
                                }
                            }
                        }

                        is FireMessageContent.Image -> {
                            Column {
                                messageModel.content.uri.let { ImageMessage(uri = it) }
                                if (isModel) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    RichText {
                                        Markdown(content = "Here's what I see in this image:")
                                    }
                                }
                            }
                        }

                        is FireMessageContent.File -> {
                            messageModel.content.uri?.let {
                                messageModel.content.name.let { it1 ->
                                    FileMessage(
                                        uri = it,
                                        fileName = it1
                                    )
                                }
                            }
                        }

                        null -> TODO()
                    }

                }

            }

        }

    }
}

@Composable
fun FileMessage(uri: String, fileName: String) {
    val icon = when (fileName.substringAfterLast(".")) {
        "pdf" -> Icons.Default.PictureAsPdf
        "doc", "docx" -> Icons.Default.Description
        "xls", "xlsx" -> Icons.Default.TableChart
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable { /* Handle file open */ }
            .padding(8.dp)
    ) {
        Icon(icon, contentDescription = "File type")
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = fileName,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun ImageMessage(uri: String) {
    val context = LocalContext.current
    val bitmap = remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uri) {
        bitmap.value = withContext(Dispatchers.IO) {
            if (uri.startsWith("content://")) {
                context.contentResolver.loadThumbnail(uri.toUri(), Size(512, 512), null)
            } else {
                BitmapFactory.decodeFile(uri)
            }
        }
    }

    bitmap.value?.let { bmp ->
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "Attached image",
            modifier = Modifier
                .sizeIn(maxHeight = 300.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun AppHeader(modifier: Modifier = Modifier) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
    ) {
        Text(
            text = "Chat Bot",
            fontSize = 16.sp,
            color = Color.White,
            modifier = Modifier.padding(16.dp)
        )

    }
}

@Composable
fun MessageInput(
    onMessageSend: (String) -> Unit,
    enabled: Boolean = true,
    onAttach: (type: String) -> Unit
) {

    var message by remember { mutableStateOf("") }
    Row(
        modifier = Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically

    ) {
        AttachmentButton(onAttachmentClick = onAttach)
        OutlinedTextField(
            modifier = Modifier.weight(1f),
            value = message,
            onValueChange = {
                message = it
            },
            enabled = enabled,
        )
        IconButton(
            onClick = {
                if (message.isNotEmpty()) {
                    onMessageSend(message)
                    message = ""
                }

            },
            enabled = enabled && message.isNotEmpty()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = null
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun ChatApp(viewModel: NChatViewModel, modifier: Modifier = Modifier, onPickImage: () -> Unit, authViewModel: AuthViewModel) {

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    ModalNavigationDrawer(

        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxHeight()
            ) {
                ChatSideBar(
                    viewModel = viewModel,
                    onCloseDrawer = {
                        scope.launch {
                            drawerState.close()
                        }
                    },
                    authViewModel=authViewModel
                )
            }
        },
        drawerState = drawerState,
        modifier = Modifier.fillMaxSize()


    ) {
        ChatPage(
            viewModel = viewModel,
            onOpenDrawer = {
                scope.launch {
                    drawerState.open()
                }
            },
            onPickImage = onPickImage
        )
    }
}


@Composable
fun ChatListItem(
    chat: FireChatSession,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit

) {
    val bgColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(bgColor)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically

    ) {
        Text(
            text = chat.title,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        IconButton(onClick = onDelete) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = null)
        }
    }
}

@Composable
fun ChatSideBar(
    viewModel: NChatViewModel,
    onCloseDrawer: () -> Unit,
    authViewModel: AuthViewModel
) {
    val chats by viewModel.chats.collectAsState()
    val currentId by viewModel.currentId.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)

            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Chats",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onCloseDrawer
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null
                )
            }


        }
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        Button(onClick ={authViewModel.signOut()} ) {
            Text(text = "Sign Out")
        }

        Button(
            onClick = { viewModel.createNewChat() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "New Chat"
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn {
            items(chats) { chat ->
                ChatListItem(
                    chat = chat,
                    isSelected = chat.id == currentId,
                    onClick = {
                        viewModel.setCurrentChat(chat.id)
                        onCloseDrawer()
                    },
                    onDelete = {
                        chat.id?.let { viewModel.deleteChat(it) }
                    }
                )

            }
        }
    }
}

@Composable
fun AttachmentButton(onAttachmentClick: (type: String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(

    ) {
        IconButton(
            onClick = { expanded = true }
        ) {
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = null
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Image") },
                onClick = {
                    onAttachmentClick("image")
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("File") },
                onClick = {
                    onAttachmentClick("file")
                    expanded = false
                }
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun ImageMessage(uri: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uri) {
        withContext(Dispatchers.IO) {
            bitmap.value = if (uri.startsWith("content://")) {
                context.contentResolver.loadThumbnail(uri.toUri(), Size(512, 512), null)
            } else {
                BitmapFactory.decodeFile(uri)
            }
        }
    }

    bitmap.value?.let { bmp ->
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "Attached image",
            modifier = modifier
                .sizeIn(maxHeight = 300.dp)
                .clip(RoundedCornerShape(8.dp))
        )
    }
}

class FilePickerActivity : AppCompatActivity() {
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            setResult(RESULT_OK, Intent().apply { data = uri })
            finish()
        }
    }

    private val pickFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            setResult(RESULT_OK, Intent().apply { data = uri })
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when (intent.getStringExtra("type")) {
            "image" -> pickImage.launch("image/*")
            "file" -> pickFile.launch("*/*")
        }
    }
}

