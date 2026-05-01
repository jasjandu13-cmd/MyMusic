package com.example.mymusic

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        StoredSong::class,
        FavoriteSongEntity::class,
        ReplayGainEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun favoriteSongDao(): FavoriteSongDao
    abstract fun replayGainDao(): ReplayGainDao
    abstract fun songDao(): SongDao
}