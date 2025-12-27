// ui/screens/chat/ChatScreen.kt
package com.example.ecolens.ui.screens.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ecolens.R
import com.example.ecolens.model.ChatMessage
import com.example.ecolens.model.ChatSession
import com.example.ecolens.ui.theme.*
import com.example.ecolens.view.EcoLensViewModel
import java.text.SimpleDateFormat
import java.util.*

// ==================== Chat History Screen ====================
@Composable
fun ChatHistoryScreen(
    viewModel: EcoLensViewModel,
    modifier: Modifier = Modifier
) {
    val chatSessions by viewModel.allChatSessions.collectAsState(initial = emptyList())

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .padding(top = 10.dp, bottom = 10.dp)
        ) {
            Text(
                text = stringResource(R.string.my_garden_title),
                style = MaterialTheme.typography.displayLarge,
                color = Primary,
                modifier = Modifier.padding(bottom = 15.dp)
            )

            HorizontalDivider(
                thickness = 1.5.dp,
                color = BorderNormal,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = (-20).dp, vertical = 10.dp)
            )
        }

        // Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (chatSessions.isEmpty()) {
                EmptyChatState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing.xl)
                )
            } else {
                ChatSessionList(
                    sessions = chatSessions,
                    onSessionClick = { /* Navigate to chat */ },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Spacing.lg)
                )
            }
        }

        // New Chat FAB
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.BottomEnd
        ) {
            ExtendedFloatingActionButton(
                onClick = { viewModel.startNewChatSession() },
                modifier = Modifier.padding(Spacing.lg),
                containerColor = Primary,
                contentColor = Color.White,
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_chat_add),
                        contentDescription = null,
                        modifier = Modifier.size(IconSize.lg)
                    )
                },
                text = { Text(stringResource(R.string.new_chat)) }
            )
        }
    }
}

@Composable
private fun ChatSessionList(
    sessions: List<ChatSession>,
    onSessionClick: (ChatSession) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        items(
            items = sessions,
            key = { it.id }
        ) { session ->
            val index = sessions.indexOf(session)
            val showDateHeader = index == 0 ||
                    !isSameDay(session.timestamp, sessions[index - 1].timestamp)

            ChatSessionItem(
                session = session,
                showDateHeader = showDateHeader,
                onClick = { onSessionClick(session) }
            )
        }
    }
}

@Composable
private fun ChatSessionItem(
    session: ChatSession,
    showDateHeader: Boolean,
    onClick: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (showDateHeader) {
            Text(
                text = dateFormatter.format(Date(session.timestamp)),
                style = MaterialTheme.typography.bodyLarge,
                color = Primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(
                    start = 4.dp,
                    bottom = Spacing.xs
                )
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = Elevation.none),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderNormal)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon with gradient ring
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_gemini_sparkle),
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(IconSize.lg)
                    )
                }

                // Info
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = Spacing.md)
                ) {
                    Text(
                        text = session.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDark,
                        maxLines = 1
                    )

                    Text(
                        text = session.lastMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        modifier = Modifier.padding(top = Spacing.xxxs)
                    )
                }

                // Time
                Text(
                    text = timeFormatter.format(Date(session.timestamp)),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextTertiary
                )
            }
        }
    }
}

@Composable
private fun EmptyChatState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_chat_bot),
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier
                .size(80.dp)
                .graphicsLayer { alpha = 0.3f }
        )

        Text(
            text = stringResource(R.string.history_empty),
            style = MaterialTheme.typography.headlineSmall,
            color = TextSecondary,
            modifier = Modifier.padding(top = Spacing.md)
        )

        Text(
            text = stringResource(R.string.garden_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(top = Spacing.xs)
        )
    }
}

// ==================== Chat Detail Screen ====================
@Composable
fun ChatScreen(
    sessionId: Long?,
    viewModel: EcoLensViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.chatMessages.collectAsState()
    val isStreaming by viewModel.isStreamingActive.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(sessionId) {
        if (sessionId != null) {
            viewModel.loadChatSession(sessionId)
        } else {
            viewModel.initNewChatSession(
                welcomeMessage = "Welcome",
                defaultTitle = "New Chat"
            )
        }
    }

    // Auto scroll to bottom
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Header
        TopAppBar(
            title = { Text(stringResource(R.string.my_garden_title)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.ic_back),
                        contentDescription = stringResource(R.string.back)
                    )
                }
            },
            actions = {
                IconButton(onClick = { /* Show menu */ }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_vert),
                        contentDescription = stringResource(R.string.settings)
                    )
                }
            }
        )

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = Spacing.sm),
            contentPadding = PaddingValues(vertical = Spacing.xs)
        ) {
            items(
                items = messages,
                key = { it.id }
            ) { message ->
                ChatMessageItem(
                    message = message,
                    onCopy = { /* Handle copy */ },
                    onShare = { /* Handle share */ },
                    onRenew = { viewModel.renewAiResponse(message) }
                )
            }
        }

        // Input
        ChatInput(
            value = messageText,
            onValueChange = { messageText = it },
            onSend = {
                if (messageText.isNotBlank()) {
                    viewModel.sendChatMessage(messageText, "New Chat")
                    messageText = ""
                }
            },
            enabled = !isStreaming,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onRenew: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.xxs),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            shape = CardShape,
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser) Primary else Surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = Elevation.none),
            border = if (!message.isUser)
                androidx.compose.foundation.BorderStroke(1.dp, BorderNormal)
            else null
        ) {
            Column(
                modifier = Modifier.padding(Spacing.sm)
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isUser) Color.White else TextPrimary,
                    modifier = Modifier.widthIn(max = 230.dp)
                )

                if (message.isStreaming) {
                    Text(
                        text = "▌",
                        color = if (message.isUser) Color.White else Primary,
                        modifier = Modifier.animateContentSize()
                    )
                }
            }
        }
    }

    // AI Actions
    if (!message.isUser && !message.isStreaming) {
        Row(
            modifier = Modifier
                .padding(start = Spacing.md)
                .padding(top = Spacing.xxs),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            IconButton(
                onClick = { onCopy(message.content) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_copy),
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = { onShare(message.content) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_share),
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onRenew,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_rotate),
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Surface,
        shadowElevation = Elevation.lg
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(thickness = 1.dp, color = BorderLight)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = InputShape,
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = BorderStrong
                    )
                ) {
                    TextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                stringResource(R.string.chat_hint),
                                color = TextTertiary
                            )
                        },
                        enabled = enabled,
                        maxLines = 4,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }

                Card(
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .size(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Primary)
                ) {
                    IconButton(
                        onClick = onSend,
                        enabled = enabled && value.isNotBlank(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_send),
                            contentDescription = stringResource(R.string.chat_hint),
                            tint = Color.White,
                            modifier = Modifier.size(IconSize.md)
                        )
                    }
                }
            }
        }
    }
}

private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return formatter.format(Date(timestamp1)) == formatter.format(Date(timestamp2))
}