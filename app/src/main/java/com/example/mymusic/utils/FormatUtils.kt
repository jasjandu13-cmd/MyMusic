package com.example.mymusic.utils

import com.example.mymusic.Song
import java.util.Locale
import kotlin.math.pow

fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}

fun formatSleepTime(timeMs: Long): String {
    val totalSeconds = (timeMs / 1000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

fun gainToLinear(gainDb: Float): Float {
    return 10.0.pow((gainDb / 20.0)).toFloat()
}

fun albumKey(song: Song): String {
    return "${song.album.trim().lowercase(Locale.getDefault())}::${song.artist.trim().lowercase(Locale.getDefault())}"
}
