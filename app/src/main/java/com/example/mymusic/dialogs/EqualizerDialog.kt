package com.example.mymusic.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun EqualizerDialog(
    eqBandCount: Int,
    eqMinLevel: Int,
    eqMaxLevel: Int,
    eqLevels: SnapshotStateMap<Int, Float>,
    getCenterFreq: (Short) -> Int,
    setBandLevel: (Short, Short) -> Unit,
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
        title = { Text("Equalizer") },
        text = {
            if (eqBandCount == 0) {
                Text("Equalizer not available on this device.")
            } else {
                LazyColumn {
                    items(eqBandCount) { index ->
                        val band = index.toShort()
                        val freqHz = getCenterFreq(band) / 1000
                        val level = eqLevels[index] ?: 0f

                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text("$freqHz Hz")
                            Slider(
                                value = level,
                                onValueChange = { newValue ->
                                    eqLevels[index] = newValue
                                    setBandLevel(band, newValue.toInt().toShort())
                                },
                                valueRange = eqMinLevel.toFloat()..eqMaxLevel.toFloat()
                            )
                            Text(
                                text = String.format(
                                    Locale.getDefault(),
                                    "%.2f dB",
                                    level / 100f
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    )
}