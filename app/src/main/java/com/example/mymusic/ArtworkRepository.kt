package com.example.mymusic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri

object ArtworkRepository {

    fun loadEmbeddedArtwork(context: Context, uri: Uri): Bitmap? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val artBytes = retriever.embeddedPicture
            val bitmap = if (artBytes != null) {
                BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
            } else {
                null
            }
            retriever.release()
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    fun loadEmbeddedArtworkBytes(context: Context, uri: Uri): ByteArray? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val artBytes = retriever.embeddedPicture
            retriever.release()
            artBytes
        } catch (_: Exception) {
            null
        }
    }
}
