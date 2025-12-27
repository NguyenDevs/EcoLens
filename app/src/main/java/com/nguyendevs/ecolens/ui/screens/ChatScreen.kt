package com.nguyendevs.ecolens.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.Html
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.model.ChatMessage
import com.nguyendevs.ecolens.ui.theme.*
import com.nguyendevs.ecolens.view.EcoLensViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: EcoLensViewModel,
    onBackClick: () -> Unit,
    sessionId: Long? = null
) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isStreaming by viewModel.isStreamingActive.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(sessionId) {
        if (sessionId != null) {
            viewModel.loadChatSession(sessionId)
        } else {
            viewModel.initNewChatSession(
                context.getString(R.string.chat_welcome),
                context.getString(R.string.new_chat)
            )
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EcoLens AI", color = OnPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = OnPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = OnPrimary)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Xóa đoạn chat") },
                            onClick = {
                                sessionId?.let { viewModel.deleteChatSession(it) }
                                onBackClick()
                                showMenu = false
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary)
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .background(Color.Transparent),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Hỏi về thiên nhiên...") },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Color.Gray
                    ),
                    enabled = !isStreaming
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendChatMessage(inputText, "New Chat")
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank() && !isStreaming,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Primary, contentColor = Color.White)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { message ->
                ChatItem(
                    message = message,
                    onCopy = { copyToClipboard(context, it) },
                    onShare = { shareText(context, it) },
                    onRenew = { viewModel.renewAiResponse(message) }
                )
            }
        }
    }
}

@Composable
fun ChatItem(
    message: ChatMessage,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onRenew: () -> Unit
) {
    val isUser = message.isUser
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (isUser) Primary else Surface
    val textColor = if (isUser) Color.White else TextPrimary
    val shape = if (isUser) {
        RoundedCornerShape(16.dp).copy(bottomEnd = CornerSize(0.dp))
    } else {
        RoundedCornerShape(16.dp).copy(bottomStart = CornerSize(0.dp))
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
            Card(
                colors = CardDefaults.cardColors(containerColor = bgColor),
                shape = shape,
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Hiển thị text HTML (Strip tags đơn giản hoặc dùng thư viện render)
                    // Ở đây dùng text trơn để demo migrate, thực tế nên dùng AndroidView với TextView để render HTML
                    Text(
                        text = stripHtml(message.content),
                        color = textColor,
                        fontSize = 16.sp,
                        lineHeight = 22.sp
                    )

                    if (!isUser) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.align(Alignment.End)) {
                            IconButton(onClick = { onCopy(message.content) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { onShare(message.content) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { onRenew() }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Refresh, contentDescription = "Renew", tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper functions (Utilities from ChatFragment)
private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val cleanText = stripHtml(text)
    val clip = ClipData.newPlainText("EcoLens", cleanText)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Đã sao chép", Toast.LENGTH_SHORT).show()
}

private fun shareText(context: Context, text: String) {
    val cleanText = stripHtml(text)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, cleanText)
    }
    context.startActivity(Intent.createChooser(intent, "Chia sẻ tin nhắn"))
}

private fun stripHtml(html: String): String {
    // Simplified version of the one in ChatFragment
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString()
    } else {
        @Suppress("DEPRECATION")
        Html.fromHtml(html).toString()
    }.trim()
}