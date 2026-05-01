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

@Composable
fun SleepTimerDialog(
    sleepTimerActive: Boolean,
    onSetMinutes: (Int) -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        title = { Text("Sleep Timer") },
        text = {
            Column {
                Button(onClick = { onSetMinutes(10) }) { Text("10 minutes") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { onSetMinutes(20) }) { Text("20 minutes") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { onSetMinutes(30) }) { Text("30 minutes") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { onSetMinutes(45) }) { Text("45 minutes") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { onSetMinutes(60) }) { Text("60 minutes") }

                if (sleepTimerActive) {
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = onCancelTimer) {
                        Text("Cancel Current Timer")
                    }
                }
            }
        }
    )
}