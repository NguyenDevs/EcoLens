package com.nguyendevs.ecolens.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.ui.components.EcoLensCard
import com.nguyendevs.ecolens.ui.components.EcoLensDivider
import com.nguyendevs.ecolens.ui.theme.Dimens
import com.nguyendevs.ecolens.ui.theme.EcoLensCorners
import com.nguyendevs.ecolens.ui.theme.EcoLensTextStyles
import com.nguyendevs.ecolens.ui.theme.Primary
import com.nguyendevs.ecolens.ui.theme.TextSecondary

/** Data class representing a chat history item. */
data class ChatHistoryItemData(
        val id: String,
        val title: String,
        val lastMessage: String,
        val timestamp: String
)

/** Chat history screen composable. Replaces screen_chat_history.xml */
@Composable
fun ChatHistoryScreen(
        items: List<ChatHistoryItemData>,
        onItemClick: (ChatHistoryItemData) -> Unit,
        onDeleteClick: (ChatHistoryItemData) -> Unit,
        onNewChatClick: () -> Unit,
        modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize().padding(top = 10.dp)) {
            // Header
            Text(
                    text = stringResource(R.string.chat_title),
                    style = EcoLensTextStyles.Display,
                    color = Primary,
                    modifier =
                            Modifier.padding(
                                    start = Dimens.PaddingScreenHorizontal,
                                    end = Dimens.PaddingScreenHorizontal,
                                    bottom = 15.dp
                            )
            )

            EcoLensDivider()

            if (items.isEmpty()) {
                EmptyChatState(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                        modifier =
                                Modifier.fillMaxSize()
                                        .padding(
                                                horizontal = Dimens.PaddingScreenHorizontal,
                                                vertical = Dimens.SpacingMd
                                        ),
                        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
                ) {
                    items(items = items, key = { it.id }) { item ->
                        ChatHistoryItem(
                                item = item,
                                onClick = { onItemClick(item) },
                                onDeleteClick = { onDeleteClick(item) }
                        )
                    }
                }
            }
        }

        // New Chat FAB
        FloatingActionButton(
                onClick = onNewChatClick,
                modifier = Modifier.align(Alignment.BottomEnd).padding(Dimens.SpacingLg),
                shape = CircleShape,
                containerColor = Primary,
                contentColor = Color.White
        ) {
            Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.new_chat),
                    modifier = Modifier.size(Dimens.IconMd)
            )
        }
    }
}

/** Single chat history item composable. */
@Composable
fun ChatHistoryItem(
        item: ChatHistoryItemData,
        onClick: () -> Unit,
        onDeleteClick: () -> Unit,
        modifier: Modifier = Modifier
) {
    EcoLensCard(modifier = modifier, onClick = onClick) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(Dimens.SpacingMd),
                verticalAlignment = Alignment.CenterVertically
        ) {
            // Chat icon
            Box(
                    modifier =
                            Modifier.size(50.dp)
                                    .clip(EcoLensCorners.Card)
                                    .background(Primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
            ) {
                Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(Dimens.SpacingMd))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                            text = item.title,
                            style = EcoLensTextStyles.Headline3,
                            color = Primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                    )

                    Text(
                            text = item.timestamp,
                            style = EcoLensTextStyles.Caption,
                            color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(Dimens.SpacingXs))

                Text(
                        text = item.lastMessage,
                        style = EcoLensTextStyles.Body2,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                )
            }

            // Delete button
            IconButton(onClick = onDeleteClick, modifier = Modifier.size(Dimens.TouchTargetMin)) {
                Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(Dimens.IconMd)
                )
            }
        }
    }
}

@Composable
private fun EmptyChatState(modifier: Modifier = Modifier) {
    Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
    ) {
        Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = null,
                tint = TextSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(Dimens.SpacingLg))

        Text(
                text = stringResource(R.string.empty_chat_title),
                style = EcoLensTextStyles.Headline2,
                color = TextSecondary
        )

        Spacer(modifier = Modifier.height(Dimens.SpacingSm))

        Text(
                text = stringResource(R.string.empty_chat_message),
                style = EcoLensTextStyles.Body2,
                color = TextSecondary.copy(alpha = 0.7f)
        )
    }
}
