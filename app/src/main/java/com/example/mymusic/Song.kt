package com.example.mymusic

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val contentUri: Uri,
    val genre: String = "Unknown Genre",
    val folder: String = "Unknown Folder"
)