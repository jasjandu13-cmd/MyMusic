package com.example.mymusic

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SongDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<StoredSong>)

    @Query("SELECT * FROM songs ORDER BY title ASC")
    suspend fun getAllSongs(): List<StoredSong>
}
