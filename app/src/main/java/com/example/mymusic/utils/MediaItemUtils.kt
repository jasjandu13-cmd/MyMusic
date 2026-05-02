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
    val artworkBytes = ArtworkRepository.loadEmbeddedArtworkBytes(context, uri)

    val metadataBuilder = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(album)

    artworkBytes?.let {
        metadataBuilder.setArtworkData(it, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
    }

    return MediaItem.Builder()
        .setUri(uri)
        .setMediaMetadata(metadataBuilder.build())
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
