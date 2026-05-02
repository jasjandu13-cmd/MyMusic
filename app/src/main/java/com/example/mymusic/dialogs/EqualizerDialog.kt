package com.example.mymusic.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    Dialog(
        onDismissRequest = onDismiss,
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
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {

                // Top bar: back + title + close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Back")
                    }

                    Text(
                        text = "Equaliser",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )

                    // Reset button on the right
                    TextButton(onClick = onReset) {
                        Text("Reset")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (eqBandCount == 0) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Equaliser not available on this device.")
                    }
                    return@Surface
                }

                // Preset grid
                val presets = listOf(
                    "Custom",
                    "Regular",
                    "Classical",
                    "Dance",
                    "Flat",
                    "Folk",
                    "Heavy metal",
                    "Hip-hop",
                    "Jazz",
                    "Pop",
                    "Rock"
                )

                val selectedPreset = remember { mutableStateOf("Custom") }
                val accent = Color(0xFF7C4DFF)

                Text(
                    text = "Recommended presets",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                // We lay presets out as rows of 3 "chips"
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.chunked(3).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { name ->
                                val isSelected = selectedPreset.value == name
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (isSelected)
                                                accent
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable {
                                            selectedPreset.value = name
                                            // NOTE: at this stage we only change UI;
                                            // later we can map each preset to real EQ settings.
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = name,
                                        color = if (isSelected) Color.White else Color.Black,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }

                            // If last row has < 3 items, fill the space
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Manual adjustment",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Sliders list
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (index in 0 until eqBandCount) {
                        val band = index.toShort()
                        val freqHz = getCenterFreq(band) / 1000
                        val level = eqLevels[index] ?: 0f

                        Column {
                            Text(
                                text = "${freqHz} Hz",
                                style = MaterialTheme.typography.bodyMedium
                            )
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
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom bar with Close button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismiss) {
                        Text("Done")
                    }
                }
            }
        }
    }
}
