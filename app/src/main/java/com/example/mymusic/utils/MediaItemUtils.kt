package com.example.mymusic.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.example.mymusic.ArtworkRepository
import com.example.mymusic.Song
import com.example.mymusic.StoredSong

private fun buildSongMediaItem(
    context: Context,
    uri: Uri,
    title: String,
    artist: String,
    album: String
): MediaItem {
    // Keep metadata lightweight – no embedded artwork here
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(album)
        .build()

    return MediaItem.Builder()
        .setUri(uri)
        .setMediaMetadata(metadata)
        .build()
}

fun Song.toMediaItem(context: Context): MediaItem {
    return buildSongMediaItem(
        context = context,
        uri = contentUri,
        title = title,
        artist = artist,
        album = album
    )
}

@SuppressLint("UseKtx")
fun StoredSong.toMediaItem(context: Context): MediaItem {
    return buildSongMediaItem(
        context = context,
        uri = contentUri.toUri(),
        title = title,
        artist = artist,
        album = album
    )
}
