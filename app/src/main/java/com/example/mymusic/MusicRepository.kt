package com.example.mymusic

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

object MusicRepository {

    fun getSongs(context: Context): List<Song> {
        val songs = mutableListOf<Song>()

        val collection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.TITLE)
            add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.ALBUM)
            add(MediaStore.Audio.Media.DURATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Audio.Media.RELATIVE_PATH)
            }
        }.toTypedArray()

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val relativePathColumn =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                } else {
                    -1
                }

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown Title"
                val artist = cursor.getString(artistColumn)?.takeIf { it.isNotBlank() } ?: "Unknown Artist"
                val album = cursor.getString(albumColumn)?.takeIf { it.isNotBlank() } ?: "Unknown Album"
                val duration = cursor.getLong(durationColumn)
                val contentUri: Uri = ContentUris.withAppendedId(collection, id)

                val relativePath =
                    if (relativePathColumn >= 0 && !cursor.isNull(relativePathColumn)) {
                        cursor.getString(relativePathColumn)
                    } else {
                        null
                    }

                val genre = getGenreForAudio(context, id)
                val folder = normalizeFolderLabel(relativePath)

                songs.add(
                    Song(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        duration = duration,
                        contentUri = contentUri,
                        genre = genre,
                        folder = folder
                    )
                )
            }
        }

        return songs
    }

    private fun getGenreForAudio(context: Context, audioId: Long): String {
        return try {
            val genreUri = MediaStore.Audio.Genres.getContentUriForAudioId("external", audioId.toInt())
            context.contentResolver.query(
                genreUri,
                arrayOf(MediaStore.Audio.Genres.NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)?.takeIf { it.isNotBlank() } ?: "Unknown Genre"
                } else {
                    "Unknown Genre"
                }
            } ?: "Unknown Genre"
        } catch (_: Exception) {
            "Unknown Genre"
        }
    }

    private fun normalizeFolderLabel(relativePath: String?): String {
        if (relativePath.isNullOrBlank()) return "Unknown Folder"
        return relativePath
            .trim()
            .trimEnd('/')
            .substringAfterLast('/')
            .ifBlank { "Unknown Folder" }
    }

    fun cacheSongsInDatabase() {
        // keep your existing implementation here
    }
}