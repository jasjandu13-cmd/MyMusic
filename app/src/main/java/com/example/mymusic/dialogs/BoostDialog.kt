package com.example.mymusic.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun BoostDialog(
    preampLevel: Float,
    loudnessBoostMb: Float,
    onPreampChange: (Float) -> Unit,
    onBoostChange: (Float) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(onClick = onReset) {
                Text("Reset")
            }
        },
        title = { Text("Sound Boost") },
        text = {
            Column {
                Text(
                    text = "Preamp",
                    style = MaterialTheme.typography.titleSmall
                )
                Slider(
                    value = preampLevel,
                    onValueChange = onPreampChange,
                    valueRange = 0.5f..1.5f
                )
                Text(
                    text = String.format(
                        Locale.getDefault(),
                        "%.2fx",
                        preampLevel
                    ),
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Loudness Boost",
                    style = MaterialTheme.typography.titleSmall
                )
                Slider(
                    value = loudnessBoostMb,
                    onValueChange = onBoostChange,
                    valueRange = 0f..1200f
                )
                Text(
                    text = "${loudnessBoostMb.toInt()} mB",
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Use small boosts first. Higher boost can still distort on some songs or devices.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    )
}
