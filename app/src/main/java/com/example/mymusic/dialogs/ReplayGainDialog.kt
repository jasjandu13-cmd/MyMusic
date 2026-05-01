package com.example.mymusic.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mymusic.ReplayGainMode

@Composable
fun ReplayGainDialog(
    replayGainMode: ReplayGainMode,
    replayGainScanning: Boolean,
    onModeSelected: (ReplayGainMode) -> Unit,
    onScanLibrary: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        title = { Text("ReplayGain") },
        text = {
            Column {
                Button(
                    onClick = { onModeSelected(ReplayGainMode.OFF) }
                ) {
                    Text(if (replayGainMode == ReplayGainMode.OFF) "Mode: Off ✓" else "Mode: Off")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { onModeSelected(ReplayGainMode.TRACK) }
                ) {
                    Text(if (replayGainMode == ReplayGainMode.TRACK) "Mode: Track ✓" else "Mode: Track")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { onModeSelected(ReplayGainMode.ALBUM) }
                ) {
                    Text(if (replayGainMode == ReplayGainMode.ALBUM) "Mode: Album ✓" else "Mode: Album")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onScanLibrary,
                    enabled = !replayGainScanning
                ) {
                    Text(if (replayGainScanning) "Scanning..." else "Scan Library")
                }
            }
        }
    )
}