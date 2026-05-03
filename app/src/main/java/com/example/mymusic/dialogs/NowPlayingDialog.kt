package com.example.mymusic.dialogs

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
            color = Color(0xFF121212)  // dark background like Mi Music
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {

                // Top bar: close + player style / EQ / more
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onClose) {
                        Text("Close", color = Color.White)
                    }

                    // Right-side icon row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Player style icon (T‑shirt in Mi Music – placeholder for now)
                        IconButton(onClick = { /* TODO: open player styles screen */ }) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Circle,
                                contentDescription = "Player style",
                                tint = Color.White
                            )
                        }

                        // Equalizer icon
                        IconButton(onClick = onOpenEqualizer) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Tune,
                                contentDescription = "Equalizer",
                                tint = Color.White
                            )
                        }

                        // More menu → we’ll open the bottom “actions” row we already have
                        IconButton(onClick = onOpenSleep) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Vinyl artwork section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    VinylArtwork(artwork)
                }

                // Sleep timer info
                if (sleepTimerActive) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sleep in ${formatSleepTime(sleepTimerRemainingMs)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFBB86FC)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    TextButton(onClick = onCancelSleepTimer) {
                        Text("Cancel Sleep Timer", color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title + artist row with favorite on the right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color(0xFFB0B0B0)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = onToggleFavorite,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = if (isFavorite) "★" else "☆",
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

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
                    Text(formatTime(currentPosition), color = Color(0xFFB0B0B0))
                    Text(formatTime(duration), color = Color(0xFFB0B0B0))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Shuffle + repeat row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onToggleShuffle) {
                        Text(
                            text = if (shuffleEnabled) "Shuffle On" else "Shuffle Off",
                            color = Color.White
                        )
                    }

                    TextButton(onClick = onCycleRepeat) {
                        Text(
                            text = repeatLabel,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Prev / Play / Next
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onPrev,
                        shape = CircleShape
                    ) {
                        Text("⏮", color = Color.White)
                    }

                    Button(
                        onClick = onPlayPause,
                        shape = CircleShape,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Text("⏯", color = Color.White)
                    }

                    Button(
                        onClick = onNext,
                        shape = CircleShape
                    ) {
                        Text("⏭", color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom row: Queue / Sleep / EQ / Boost / RG (kept as text buttons for now)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onOpenQueue,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("Queue")
                    }

                    Button(
                        onClick = onOpenSleep,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("Sleep")
                    }

                    Button(
                        onClick = onOpenEqualizer,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("EQ")
                    }

                    Button(
                        onClick = onOpenBoost,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("Boost")
                    }

                    Button(
                        onClick = onOpenReplayGain,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("RG")
                    }
                }
            }
        }
    }
}

@Composable
private fun VinylArtwork(artwork: Bitmap?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),  // square
        contentAlignment = Alignment.Center
    ) {
        // Draw the vinyl disk using Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize(0.9f)
        ) {
            val radius = size.minDimension / 2f

            // Outer dark ring
            drawCircle(
                color = Color(0xFF1E1E1E),
                radius = radius,
                center = Offset(size.width / 2f, size.height / 2f)
            )

            // Mid ring
            drawCircle(
                color = Color(0xFF2C2C2C),
                radius = radius * 0.75f,
                center = Offset(size.width / 2f, size.height / 2f)
            )

            // Tiny center label / hole
            drawCircle(
                color = Color(0xFF111111),
                radius = radius * 0.08f,
                center = Offset(size.width / 2f, size.height / 2f)
            )
        }

        // Album art in the middle
        if (artwork != null) {
            Image(
                bitmap = artwork.asImageBitmap(),
                contentDescription = "Album art",
                modifier = Modifier
                    .fillMaxSize(0.55f)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.55f)
                    .clip(CircleShape)
                    .background(Color(0xFF333333)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "♪",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White
                )
            }
        }
    }
}
