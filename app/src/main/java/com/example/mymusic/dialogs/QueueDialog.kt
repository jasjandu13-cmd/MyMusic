package com.example.mymusic.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.Player

@Composable
fun QueueDialog(
    player: Player,
    queueVersion: Int,
    onClose: () -> Unit,
    onPlayAt: (Int) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onRemoveAt: (Int) -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onClose) {
                        Text("Close")
                    }

                    Text(
                        text = "Up Next",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.size(64.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Current queue: ${player.mediaItemCount} tracks",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (player.mediaItemCount == 0) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Queue is empty.")
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn {
                        // use queueVersion so recomposition happens when it changes
                        items(player.mediaItemCount) { index ->
                            queueVersion

                            val item = player.getMediaItemAt(index)
                            val title = item.mediaMetadata.title?.toString() ?: "Unknown title"
                            val artist = item.mediaMetadata.artist?.toString() ?: "Unknown artist"
                            val isCurrent = index == player.currentMediaItemIndex

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Text(
                                        text = if (isCurrent) "Now Playing" else "Up Next",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isCurrent) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { onPlayAt(index) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Play")
                                        }

                                        Button(
                                            onClick = { onMoveUp(index) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Up")
                                        }

                                        Button(
                                            onClick = { onMoveDown(index) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Down")
                                        }

                                        Button(
                                            onClick = { onRemoveAt(index) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Remove")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
