package com.example.mymusic

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class StoredSong(
    @PrimaryKey
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val contentUri: String
)
