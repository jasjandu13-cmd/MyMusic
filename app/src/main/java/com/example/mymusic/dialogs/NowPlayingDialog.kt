package com.example.mymusic.dialogs

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.mymusic.Song
import com.example.mymusic.utils.formatSleepTime
import com.example.mymusic.utils.formatTime

@Composable
fun NowPlayingDialog(
    song: Song,
    artwork: Bitmap?,
    isFavorite: Boolean,
    shuffleEnabled: Boolean,
    repeatLabel: String,
    currentPosition: Long,
    duration: Long,
    isSeeking: Boolean,
    seekPosition: Float,
    sleepTimerActive: Boolean,
    sleepTimerRemainingMs: Long,
    onClose: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCancelSleepTimer: () -> Unit,
    onSeekChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenSleep: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenBoost: () -> Unit,
    onOpenReplayGain: () -> Unit
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
                // Top bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onClose) {
                        Text("Close")
                    }

                    Text(
                        text = "Now Playing",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.size(64.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Artwork
                if (artwork != null) {
                    Image(
                        bitmap = artwork.asImageBitmap(),
                        contentDescription = "Album art",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.42f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "♪",
                            style = MaterialTheme.typography.displayLarge
                        )
                    }
                }

                // Sleep timer info
                if (sleepTimerActive) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sleep in ${formatSleepTime(sleepTimerRemainingMs)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = onCancelSleepTimer) {
                        Text("Cancel Sleep Timer")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Title + artist
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(onClick = onToggleFavorite) {
                    Text(
                        if (isFavorite) "Remove from Favorites" else "Add to Favorites"
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Seek bar
                Slider(
                    value = if (duration > 0) {
                        if (isSeeking) seekPosition else currentPosition.toFloat()
                    } else {
                        0f
                    },
                    onValueChange = onSeekChange,
                    onValueChangeFinished = onSeekFinished,
                    valueRange = 0f..(if (duration > 0) duration.toFloat() else 1f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(currentPosition))
                    Text(formatTime(duration))
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Shuffle + repeat
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = onToggleShuffle) {
                        Text(if (shuffleEnabled) "Shuffle On" else "Shuffle Off")
                    }

                    Button(onClick = onCycleRepeat) {
                        Text(repeatLabel)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Prev / Play / Next
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = onPrev) {
                        Text("Prev")
                    }

                    Button(onClick = onPlayPause) {
                        Text("Play/Pause")
                    }

                    Button(onClick = onNext) {
                        Text("Next")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Queue / Sleep / EQ / Boost / RG
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onOpenQueue,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Queue")
                    }

                    Button(
                        onClick = onOpenSleep,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Sleep")
                    }

                    Button(
                        onClick = onOpenEqualizer,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("EQ")
                    }

                    Button(
                        onClick = onOpenBoost,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Boost")
                    }

                    Button(
                        onClick = onOpenReplayGain,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("RG")
                    }
                }
            }
        }
    }
}
